package edu.washington.escience.myria.parallel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

import com.google.common.collect.Sets;

import edu.washington.escience.myria.DbException;
import edu.washington.escience.myria.MyriaConstants;
import edu.washington.escience.myria.MyriaConstants.FTMODE;
import edu.washington.escience.myria.MyriaSystemConfigKeys;
import edu.washington.escience.myria.accessmethod.ConnectionInfo;
import edu.washington.escience.myria.coordinator.catalog.CatalogException;
import edu.washington.escience.myria.coordinator.catalog.WorkerCatalog;
import edu.washington.escience.myria.parallel.ipc.FlowControlBagInputBuffer;
import edu.washington.escience.myria.parallel.ipc.IPCConnectionPool;
import edu.washington.escience.myria.parallel.ipc.InJVMLoopbackChannelSink;
import edu.washington.escience.myria.proto.ControlProto.ControlMessage;
import edu.washington.escience.myria.proto.TransportProto.TransportMessage;
import edu.washington.escience.myria.util.IPCUtils;
import edu.washington.escience.myria.util.JVMUtils;
import edu.washington.escience.myria.util.concurrent.RenamingThreadFactory;
import edu.washington.escience.myria.util.concurrent.ThreadAffinityFixedRoundRobinExecutionPool;
import edu.washington.escience.myria.util.concurrent.TimerTaskThreadFactory;

/**
 * Workers do the real query execution. A query received by the server will be pre-processed and then dispatched to the
 * workers.
 * 
 * To execute a query on a worker, 4 steps are proceeded:
 * 
 * 1) A worker receive an Operator instance as its execution plan. The worker then stores the plan and does some
 * pre-processing, e.g. initializes the data structures which are needed during the execution of the plan.
 * 
 * 2) Each worker sends back to the server a message (it's id) to notify the server that the query plan has been
 * successfully received. And then each worker waits for the server to send the "start" message.
 * 
 * 3) Each worker executes its query plan after "start" is received.
 * 
 * 4) After the query plan finishes, each worker removes the query plan and related data structures, and then waits for
 * next query plan
 * 
 */
public final class Worker {

  /** The logger for this class. */
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Worker.class.getName());

  /**
   * Control message processor.
   * */
  private final class ControlMessageProcessor implements Runnable {
    @Override
    public void run() {
      try {

        TERMINATE_MESSAGE_PROCESSING : while (true) {
          if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            break TERMINATE_MESSAGE_PROCESSING;
          }

          ControlMessage cm = null;
          try {
            while ((cm = controlMessageQueue.take()) != null) {
              int workerId = cm.getWorkerId();
              switch (cm.getType()) {
                case SHUTDOWN:
                  if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("shutdown requested");
                  }
                  toShutdown = true;
                  abruptShutdown = false;
                  break;
                case REMOVE_WORKER:
                  if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("received REMOVE_WORKER " + workerId);
                  }
                  connectionPool.removeRemote(workerId).await();
                  sendMessageToMaster(IPCUtils.removeWorkerAckTM(workerId));
                  for (Long id : activeQueries.keySet()) {
                    WorkerQueryPartition wqp = activeQueries.get(id);
                    if (wqp.getFTMode().equals(FTMODE.abandon)) {
                      wqp.getMissingWorkers().add(workerId);
                      wqp.updateProducerChannels(workerId, false);
                      wqp.triggerTasks();
                    } else if (wqp.getFTMode().equals(FTMODE.rejoin)) {
                      wqp.getMissingWorkers().add(workerId);
                    }
                  }
                  break;
                case ADD_WORKER:
                  if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("received ADD_WORKER " + workerId);
                  }
                  connectionPool.putRemote(workerId, SocketInfo.fromProtobuf(cm.getRemoteAddress()));
                  sendMessageToMaster(IPCUtils.addWorkerAckTM(workerId));
                  break;
                default:
                  if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Unexpected control message received at worker: " + cm.getType());
                  }
              }
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      } catch (Throwable ee) {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("Unknown exception caught at control message processing.", ee);
        }
      }
    }
  }

  /**
   * The non-blocking query driver. It calls root.nextReady() to start a query.
   */
  private class QueryMessageProcessor implements Runnable {

    @Override
    public final void run() {
      try {
        WorkerQueryPartition q = null;
        while (true) {
          try {
            q = queryQueue.take();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }

          if (q != null) {
            try {
              receiveQuery(q);
              sendMessageToMaster(IPCUtils.queryReadyTM(q.getQueryID()));
            } catch (DbException e) {
              if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unexpected exception at preparing query. Drop the query.", e);
              }
              q = null;
            }
          }
        }
      } catch (Throwable ee) {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("Unknown exception caught at query nonblocking driver.", ee);
        }
      }
    }

  }

  /** Send heartbeats to server periodically. */
  private class HeartbeatReporter extends TimerTask {
    @Override
    public synchronized void run() {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("sending heartbeat to server");
      }
      sendMessageToMaster(IPCUtils.CONTROL_WORKER_HEARTBEAT).awaitUninterruptibly();
    }
  }

  /**
   * Periodically detect whether the {@link Worker} should be shutdown. 1) it detects whether the server is still alive.
   * If the server got killed because of any reason, the workers will be terminated. 2) it detects whether a shutdown
   * message is received.
   * */
  private class ShutdownChecker extends TimerTask {
    @Override
    public final synchronized void run() {
      try {
        if (!connectionPool.isRemoteAlive(MyriaConstants.MASTER_ID)) {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("The Master has shutdown, I'll shutdown now.");
          }
          toShutdown = true;
          abruptShutdown = true;
        }
      } catch (Throwable e) {
        toShutdown = true;
        abruptShutdown = true;
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("Unknown error in " + ShutdownChecker.class.getSimpleName(), e);
        }
      }
      if (toShutdown) {
        try {
          shutdown();
        } catch (Throwable e) {
          try {
            if (LOGGER.isErrorEnabled()) {
              LOGGER.error("Unknown error in shutdown, halt the worker directly", e);
            }
          } finally {
            JVMUtils.shutdownVM();
          }
        }
      }
    }
  }

  /**
   * This class manages all currently running user threads. It waits all these threads to finish within some given
   * timeout. If timeout, try interrupting them. If any thread is interrupted for a given number of times, stop waiting
   * and kill it directly.
   * */
  private class ShutdownThreadCleaner extends Thread {

    /**
     * In wait state for at most 5 seconds.
     * */
    static final int WAIT_MAXIMUM_MS = 5 * 1000;
    /**
     * Interrupt an unresponding thread for at most 3 times.
     * */
    static final int MAX_INTERRUPT_TIMES = 3;

    /**
     * for setting not daemon.
     * */
    ShutdownThreadCleaner() {
      super.setDaemon(true);
    }

    /**
     * How many milliseconds a thread have been waited to get finish.
     * */
    private final HashMap<Thread, Integer> waitedForMS = new HashMap<Thread, Integer>();
    /**
     * How many times a thread has been interrupted.
     * */
    private final HashMap<Thread, Integer> interruptTimes = new HashMap<Thread, Integer>();
    /**
     * The set of threads we have been waiting for the maximum MS, and so have decided to kill them directly.
     * */
    private final Set<Thread> abandonThreads = Sets.newSetFromMap(new HashMap<Thread, Boolean>());

    /**
     * utility method, add an integer v to the value of m[t] and return the new value. null key and value are taken ca
     * of.
     * 
     * @return the new value
     * @param m a map
     * @param t a thread
     * @param v the value
     * */
    private int addToMap(final Map<Thread, Integer> m, final Thread t, final int v) {
      Integer tt = m.get(t);
      if (tt == null) {
        tt = 0;
      }
      m.put(t, tt + v);
      return tt + v;
    }

    /**
     * utility method, get the value of m[t] . null key and value are taken care of.
     * 
     * @param m a map
     * @param t a thread
     * @return the value
     * */
    private int getFromMap(final Map<Thread, Integer> m, final Thread t) {
      Integer tt = m.get(t);
      if (tt == null) {
        tt = 0;
      }
      return tt;
    }

    @Override
    public final void run() {

      while (true) {
        Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
        HashMap<Thread, Integer> nonSystemThreads = new HashMap<Thread, Integer>();
        for (final Thread t : allThreads) {
          if (t.getThreadGroup() != null && t.getThreadGroup() != mainThreadGroup
              && t.getThreadGroup() != mainThreadGroup.getParent() && t != Thread.currentThread()
              && !abandonThreads.contains(t)) {
            nonSystemThreads.put(t, 0);
          }
        }

        if (nonSystemThreads.isEmpty()) {
          if (abandonThreads.isEmpty()) {
            return;
          } else {
            JVMUtils.shutdownVM();
          }
        }

        try {
          Thread.sleep(MyriaConstants.SHORT_WAITING_INTERVAL_100_MS);
        } catch (InterruptedException e) {
          JVMUtils.shutdownVM();
        }

        for (final Thread t : nonSystemThreads.keySet()) {
          if (addToMap(waitedForMS, t, MyriaConstants.SHORT_WAITING_INTERVAL_100_MS) > WAIT_MAXIMUM_MS) {
            waitedForMS.put(t, 0);
            if (addToMap(interruptTimes, t, 1) > MAX_INTERRUPT_TIMES) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Thread {} have been interrupted for {} times. Kill it directly.", t, getFromMap(
                    interruptTimes, t) - 1);
              }
              abandonThreads.add(t);
            } else {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Waited Thread {} to finish for {} seconds. I'll try interrupting it.", t,
                    TimeUnit.MILLISECONDS.toSeconds(WAIT_MAXIMUM_MS) * getFromMap(interruptTimes, t));
              }
              t.interrupt();
            }
          }
        }
      }
    }
  }

  /**
   * usage.
   * */
  static final String USAGE = "Usage: worker [--conf <conf_dir>]";

  /**
   * {@link ExecutorService} for query executions.
   * */
  private volatile ExecutorService queryExecutor;

  /**
   * @return the query executor used in this worker.
   * */
  ExecutorService getQueryExecutor() {
    return queryExecutor;
  }

  /**
   * {@link ExecutorService} for non-query message processing.
   * */
  private volatile ExecutorService messageProcessingExecutor;

  /**
   * current active queries. queryID -> QueryPartition
   * */
  private final ConcurrentHashMap<Long, WorkerQueryPartition> activeQueries;

  /**
   * shutdown checker executor.
   * */
  private ScheduledExecutorService scheduledTaskExecutor;

  /**
   * The ID of this worker.
   */
  private final int myID;

  /**
   * connectionPool[0] is always the master.
   */
  private final IPCConnectionPool connectionPool;

  /**
   * A indicator of shutting down the worker.
   */
  private volatile boolean toShutdown = false;

  /**
   * abrupt shutdown.
   * */
  private volatile boolean abruptShutdown = false;

  /**
   * Message queue for control messages.
   * */
  private final LinkedBlockingQueue<ControlMessage> controlMessageQueue;

  /**
   * Message queue for queries.
   * */
  private final PriorityBlockingQueue<WorkerQueryPartition> queryQueue;

  /**
   * My catalog.
   * */
  private final WorkerCatalog catalog;

  /**
   * master IPC address.
   * */
  private final SocketInfo masterSocketInfo;

  /**
   * Query execution mode. May remove
   * */
  private final QueryExecutionMode queryExecutionMode;

  /**
   * {@link ExecutorService} for Netty pipelines.
   * */
  private volatile OrderedMemoryAwareThreadPoolExecutor pipelineExecutor;

  /**
   * The default input buffer capacity for each {@link Consumer} input buffer.
   * */
  private final int inputBufferCapacity;

  /**
   * the system wide default inuput buffer recover event trigger.
   * 
   * @see FlowControlBagInputBuffer#INPUT_BUFFER_RECOVER
   * */
  private final int inputBufferRecoverTrigger;

  /**
   * Current working directory. It's the logical root of the worker. All the data the worker and the operators running
   * on the worker can access should be put under this directory.
   * */
  private final String workingDirectory;

  /**
   * Execution environment variables for operators.
   * */
  private final ConcurrentHashMap<String, Object> execEnvVars;

  /**
   * The thread group of the main thread.
   * */
  private static volatile ThreadGroup mainThreadGroup;

  /**
   * @param args command line arguments
   * @return options parsed from command line.
   * */
  private static HashMap<String, Object> processArgs(final String[] args) {
    HashMap<String, Object> options = new HashMap<String, Object>();
    if (args.length > 2) {
      LOGGER.warn("Invalid number of arguments.\n" + USAGE);
      JVMUtils.shutdownVM();
    }

    String workingDirTmp = System.getProperty("user.dir");
    if (args.length >= 2) {
      if (args[0].equals("--workingDir")) {
        workingDirTmp = args[1];
      } else {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("Invalid arguments.\n" + USAGE);
        }
        JVMUtils.shutdownVM();
      }
    }
    options.put("workingDir", workingDirTmp);
    return options;
  }

  /**
   * Setup system properties.
   * 
   * @param cmdlineOptions command line options
   * */
  private static void systemSetup(final HashMap<String, Object> cmdlineOptions) {
    java.util.logging.Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.SEVERE);
    java.util.logging.Logger.getLogger("com.almworks.sqlite4java.Internal").setLevel(Level.SEVERE);

    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(final Thread t, final Throwable e) {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("Uncaught exception in thread: " + t, e);
        }
        if (e instanceof OutOfMemoryError) {
          JVMUtils.shutdownVM();
        }
      }
    });

    mainThreadGroup = Thread.currentThread().getThreadGroup();
  }

  /**
   * @param cmdlineOptions command line options
   * */
  private static void bootupWorker(final HashMap<String, Object> cmdlineOptions) {
    final String workingDir = (String) cmdlineOptions.get("workingDir");
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("workingDir: " + workingDir);
    }

    ThreadGroup workerThreadGroup = new ThreadGroup(mainThreadGroup, "MyriaWorkerThreadGroup");
    Thread myriaWorkerMain = new Thread(workerThreadGroup, "MyriaWorkerMain") {
      @Override
      public void run() {
        try {
          // Instantiate a new worker
          final Worker w = new Worker(workingDir, QueryExecutionMode.NON_BLOCKING);
          // int port = w.port;

          // Start the actual message handler by binding
          // the acceptor to a network socket
          // Now the worker can accept messages
          w.start();

          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Worker started at:" + w.catalog.getWorkers().get(w.myID));
          }
        } catch (Throwable e) {
          if (LOGGER.isErrorEnabled()) {
            LOGGER.error("Unknown error occurs at Worker. Quit directly.", e);
          }
          JVMUtils.shutdownVM();
        }
      }
    };
    myriaWorkerMain.start();
  }

  /**
   * Worker process entry point.
   * 
   * @param args command line arguments.
   * */
  public static void main(final String[] args) {
    try {
      HashMap<String, Object> cmdlineOptions = processArgs(args);
      systemSetup(cmdlineOptions);
      bootupWorker(cmdlineOptions);
    } catch (Throwable e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Unknown error occurs at Worker. Quit directly.", e);
      }
      JVMUtils.shutdownVM();
    }
  }

  /**
   * @return my control message queue.
   * */
  LinkedBlockingQueue<ControlMessage> getControlMessageQueue() {
    return controlMessageQueue;
  }

  /**
   * @return my query queue.
   * */
  PriorityBlockingQueue<WorkerQueryPartition> getQueryQueue() {
    return queryQueue;
  }

  /**
   * @return my connection pool for IPC.
   * */
  IPCConnectionPool getIPCConnectionPool() {
    return connectionPool;
  }

  /**
   * @return my pipeline executor.
   * */
  OrderedMemoryAwareThreadPoolExecutor getPipelineExecutor() {
    return pipelineExecutor;
  }

  /**
   * @return the system wide default inuput buffer capacity.
   * */
  int getInputBufferCapacity() {
    return inputBufferCapacity;
  }

  /**
   * @return the system wide default inuput buffer recover event trigger.
   * @see FlowControlBagInputBuffer#INPUT_BUFFER_RECOVER
   * */
  int getInputBufferRecoverTrigger() {
    return inputBufferRecoverTrigger;
  }

  /**
   * @return my execution environment variables for init of operators.
   * */
  ConcurrentHashMap<String, Object> getExecEnvVars() {
    return execEnvVars;
  }

  /**
   * @return the working directory of the worker.
   * */
  public String getWorkingDirectory() {
    return workingDirectory;
  }

  /**
   * @return the current active queries.
   * */
  ConcurrentHashMap<Long, WorkerQueryPartition> getActiveQueries() {
    return activeQueries;
  }

  /**
   * @return query execution mode.
   * */
  QueryExecutionMode getQueryExecutionMode() {
    return queryExecutionMode;
  }

  /**
   * @param workingDirectory my working directory.
   * @param mode my execution mode.
   * @throws CatalogException if there's any catalog operation errors.
   * @throws FileNotFoundException if catalog files are not found.
   * */
  public Worker(final String workingDirectory, final QueryExecutionMode mode) throws CatalogException,
      FileNotFoundException {
    queryExecutionMode = mode;
    catalog = WorkerCatalog.open(FilenameUtils.concat(workingDirectory, "worker.catalog"));

    this.workingDirectory = workingDirectory;
    myID = Integer.parseInt(catalog.getConfigurationValue(MyriaSystemConfigKeys.WORKER_IDENTIFIER));

    controlMessageQueue = new LinkedBlockingQueue<ControlMessage>();
    queryQueue = new PriorityBlockingQueue<WorkerQueryPartition>();

    masterSocketInfo = catalog.getMasters().get(0);

    final Map<Integer, SocketInfo> workers = catalog.getWorkers();
    final Map<Integer, SocketInfo> computingUnits = new HashMap<Integer, SocketInfo>();
    computingUnits.putAll(workers);
    computingUnits.put(MyriaConstants.MASTER_ID, masterSocketInfo);

    connectionPool =
        new IPCConnectionPool(myID, computingUnits, IPCConfigurations.createWorkerIPCServerBootstrap(this),
            IPCConfigurations.createWorkerIPCClientBootstrap(this), new TransportMessageSerializer(),
            new WorkerShortMessageProcessor(this));
    activeQueries = new ConcurrentHashMap<Long, WorkerQueryPartition>();

    inputBufferCapacity =
        Integer.valueOf(catalog.getConfigurationValue(MyriaSystemConfigKeys.OPERATOR_INPUT_BUFFER_CAPACITY));

    inputBufferRecoverTrigger =
        Integer.valueOf(catalog.getConfigurationValue(MyriaSystemConfigKeys.OPERATOR_INPUT_BUFFER_RECOVER_TRIGGER));

    execEnvVars = new ConcurrentHashMap<String, Object>();

    for (Entry<String, String> cE : catalog.getAllConfigurations().entrySet()) {
      execEnvVars.put(cE.getKey(), cE.getValue());
    }
    final String databaseSystem = catalog.getConfigurationValue(MyriaSystemConfigKeys.WORKER_STORAGE_DATABASE_SYSTEM);
    execEnvVars.put(MyriaConstants.EXEC_ENV_VAR_DATABASE_SYSTEM, databaseSystem);
    LOGGER.info("Worker: Database system " + databaseSystem);
    String jsonConnInfo = catalog.getConfigurationValue(MyriaSystemConfigKeys.WORKER_STORAGE_DATABASE_CONN_INFO);
    if (jsonConnInfo == null) {
      throw new CatalogException("Missing database connection information");
    }
    LOGGER.info("Worker: Connection info " + jsonConnInfo);
    execEnvVars.put(MyriaConstants.EXEC_ENV_VAR_DATABASE_CONN_INFO, ConnectionInfo.of(databaseSystem, jsonConnInfo));
  }

  /**
   * It does the initialization and preparation for the execution of the query.
   * 
   * @param query the received query.
   * @throws DbException if any error occurs.
   */
  public void receiveQuery(final WorkerQueryPartition query) throws DbException {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Query received " + query.getQueryID());
    }

    activeQueries.put(query.getQueryID(), query);
    query.getExecutionFuture().addListener(new QueryFutureListener() {

      @Override
      public void operationComplete(final QueryFuture future) {
        activeQueries.remove(query.getQueryID());

        if (future.isSuccess()) {

          sendMessageToMaster(IPCUtils.queryCompleteTM(query.getQueryID(), query.getExecutionStatistics()))
              .addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                  if (future.isSuccess()) {
                    if (LOGGER.isDebugEnabled()) {
                      LOGGER.debug("The query complete message is sent to the master for sure ");
                    }
                  }
                }

              });
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("My part of query " + query + " finished");
          }

        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Query failed because of exception: ", future.getCause());
          }

          TransportMessage tm = null;
          try {
            tm = IPCUtils.queryFailureTM(query.getQueryID(), future.getCause(), query.getExecutionStatistics());
          } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
              LOGGER.error("Unknown query failure TM creation error", e);
            }
            tm = IPCUtils.simpleQueryFailureTM(query.getQueryID());
          }
          sendMessageToMaster(tm).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
              if (future.isSuccess()) {
                if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("The query complete message is sent to the master for sure ");
                }
              }
            }
          });
        }
      }
    });
  }

  /**
   * @param message the message to get sent to the master
   * @return the future of this sending action.
   * */
  ChannelFuture sendMessageToMaster(final TransportMessage message) {
    return Worker.this.connectionPool.sendShortMessage(MyriaConstants.MASTER_ID, message);
  }

  /**
   * This method should be called whenever the system is going to shutdown.
   * 
   */
  void shutdown() {
    try {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Shutdown requested. Please wait when cleaning up...");
      }

      if (!connectionPool.isShutdown()) {
        if (!abruptShutdown) {
          connectionPool.shutdown();
        } else {
          connectionPool.shutdownNow();
        }
      }
      connectionPool.releaseExternalResources();

      if (pipelineExecutor != null && !pipelineExecutor.isShutdown()) {
        pipelineExecutor.shutdown();
      }

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("shutdown IPC completed");
      }
      // must use shutdownNow here because the query queue processor and the control message processor are both
      // blocking.
      // We have to interrupt them at shutdown.
      messageProcessingExecutor.shutdownNow();
      queryExecutor.shutdown();
      scheduledTaskExecutor.shutdown();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Worker #" + myID + " shutdown completed");
      }
    } finally {
      new Thread(mainThreadGroup, new ShutdownThreadCleaner(), "ShutdownThreadCleaner").start();
    }
  }

  /**
   * Start the worker service.
   * 
   * @throws Exception if any error meets.
   * */
  public void start() throws Exception {
    ExecutorService bossExecutor = Executors.newCachedThreadPool(new RenamingThreadFactory("IPC boss"));
    ExecutorService workerExecutor = Executors.newCachedThreadPool(new RenamingThreadFactory("IPC worker"));
    pipelineExecutor =
        new OrderedMemoryAwareThreadPoolExecutor(3, 0, 0, MyriaConstants.THREAD_POOL_KEEP_ALIVE_TIME_IN_MS,
            TimeUnit.MILLISECONDS, new RenamingThreadFactory("Pipeline executor"));

    ChannelFactory clientChannelFactory =
        new NioClientSocketChannelFactory(bossExecutor, workerExecutor,
            Runtime.getRuntime().availableProcessors() * 2 + 1);

    // Start server with Nb of active threads = 2*NB CPU + 1 as maximum.
    ChannelFactory serverChannelFactory =
        new NioServerSocketChannelFactory(bossExecutor, workerExecutor,
            Runtime.getRuntime().availableProcessors() * 2 + 1);

    ChannelPipelineFactory serverPipelineFactory =
        new IPCPipelineFactories.WorkerServerPipelineFactory(connectionPool, getPipelineExecutor());
    ChannelPipelineFactory clientPipelineFactory =
        new IPCPipelineFactories.WorkerClientPipelineFactory(connectionPool, getPipelineExecutor());
    ChannelPipelineFactory workerInJVMPipelineFactory =
        new IPCPipelineFactories.WorkerInJVMPipelineFactory(connectionPool);

    connectionPool.start(serverChannelFactory, serverPipelineFactory, clientChannelFactory, clientPipelineFactory,
        workerInJVMPipelineFactory, new InJVMLoopbackChannelSink());

    if (queryExecutionMode == QueryExecutionMode.NON_BLOCKING) {
      int numCPU = Runtime.getRuntime().availableProcessors();
      queryExecutor =
      // new ThreadPoolExecutor(numCPU, numCPU, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
      // new RenamingThreadFactory("Nonblocking query executor"));
          new ThreadAffinityFixedRoundRobinExecutionPool(numCPU,
              new RenamingThreadFactory("Nonblocking query executor"));
    } else {
      // blocking query execution
      queryExecutor = Executors.newCachedThreadPool(new RenamingThreadFactory("Blocking query executor"));
    }
    messageProcessingExecutor =
        Executors.newCachedThreadPool(new RenamingThreadFactory("Control/Query message processor"));
    messageProcessingExecutor.submit(new QueryMessageProcessor());
    messageProcessingExecutor.submit(new ControlMessageProcessor());
    // Periodically detect if the server (i.e., coordinator)
    // is still running. IF the server goes down, the
    // worker will stop itself
    scheduledTaskExecutor = Executors.newScheduledThreadPool(2, new TimerTaskThreadFactory("Worker global timer"));
    scheduledTaskExecutor.scheduleAtFixedRate(new ShutdownChecker(), MyriaConstants.WORKER_SHUTDOWN_CHECKER_INTERVAL,
        MyriaConstants.WORKER_SHUTDOWN_CHECKER_INTERVAL, TimeUnit.MILLISECONDS);
    scheduledTaskExecutor.scheduleAtFixedRate(new HeartbeatReporter(), 0, MyriaConstants.HEARTBEAT_INTERVAL,
        TimeUnit.MILLISECONDS);
  }

  /**
   * @param configKey config key.
   * @return a worker configuration.
   * */
  public String getConfiguration(final String configKey) {
    try {
      return catalog.getConfigurationValue(configKey);
    } catch (CatalogException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Configuration retrieval error", e);
      }
      return null;
    }
  }
}