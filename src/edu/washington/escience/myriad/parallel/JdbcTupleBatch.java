package edu.washington.escience.myriad.parallel;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import edu.washington.escience.myriad.Predicate;
import edu.washington.escience.myriad.Schema;
import edu.washington.escience.myriad.TupleBatch;
import edu.washington.escience.myriad.TupleBatchBuffer;
import edu.washington.escience.myriad.Type;
import edu.washington.escience.myriad.accessmethod.JdbcAccessMethod;
import edu.washington.escience.myriad.column.Column;
import edu.washington.escience.myriad.table._TupleBatch;

// Not yet @ThreadSafe
public class JdbcTupleBatch implements _TupleBatch {

  private static final long serialVersionUID = 1L;

  public static final int BATCH_SIZE = 100;

  private final Schema inputSchema;
  private final String connectString;
  private int numInputTuples;
  private final String driverClass;
  private final String tableName;
  private final String username;
  private final String password;

  public JdbcTupleBatch(final Schema inputSchema, final String tableName, final String connectionString,
      final String driverClass, final String username, final String password) {
    this.inputSchema = Objects.requireNonNull(inputSchema);
    connectString = connectionString;
    this.driverClass = driverClass;
    this.tableName = tableName;
    this.username = username;
    this.password = password;
  }

  @Override
  public synchronized _TupleBatch append(final _TupleBatch another) {
    final Iterator<Schema.TDItem> it = inputSchema.iterator();

    final String[] fieldNames = new String[inputSchema.numFields()];
    final String[] placeHolders = new String[inputSchema.numFields()];
    int i = 0;
    while (it.hasNext()) {
      final Schema.TDItem item = it.next();
      placeHolders[i] = "?";
      fieldNames[i++] = item.getName();
    }

    JdbcAccessMethod.tupleBatchInsert(driverClass, connectString, "insert into " + tableName + " ( "
        + StringUtils.join(fieldNames, ',') + " ) values ( " + StringUtils.join(placeHolders, ',') + " )",
        new TupleBatch(another.outputSchema(), another.outputRawData(), another.numOutputTuples()), username, password);
    return this;
  }

  @Override
  public synchronized _TupleBatch distinct() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public synchronized _TupleBatch except(final _TupleBatch another) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public synchronized JdbcTupleBatch filter(final int fieldIdx, final Predicate.Op op, final Object operand) {
    return this;
  }

  @Override
  public synchronized boolean getBoolean(final int column, final int row) {
    // return ((BooleanColumn) inputColumns.get(column)).getBoolean(row);
    return false;
  }

  @Override
  public synchronized double getDouble(final int column, final int row) {
    // return ((DoubleColumn) inputColumns.get(column)).getDouble(row);
    return 0d;
  }

  @Override
  public synchronized float getFloat(final int column, final int row) {
    // return ((FloatColumn) inputColumns.get(column)).getFloat(row);
    return 0f;
  }

  @Override
  public synchronized int getInt(final int column, final int row) {
    // return ((IntColumn) inputColumns.get(column)).getInt(row);
    return 0;
  }

  @Override
  public synchronized long getLong(final int column, final int row) {
    // return ((IntColumn) inputColumns.get(column)).getInt(row);
    return 0;
  }

  @Override
  public synchronized String getString(final int column, final int row) {
    // return ((StringColumn) inputColumns.get(column)).getString(row);
    return null;
  }

  @Override
  public synchronized _TupleBatch groupby() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int hashCode(final int rowIndx) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode(final int rowIndx, final int[] colIndx) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Schema inputSchema() {
    return inputSchema;
  }

  @Override
  public synchronized _TupleBatch intersect(final _TupleBatch another) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public synchronized _TupleBatch join(final _TupleBatch other, final Predicate p, final _TupleBatch output) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public synchronized int numInputTuples() {
    return numInputTuples;
  }

  @Override
  public synchronized int numOutputTuples() {
    return numInputTuples;
  }

  @Override
  public synchronized _TupleBatch orderby() {
    // TODO Auto-generated method stub
    return null;
  }

  protected synchronized int[] outputColumnIndices() {
    final int numInputColumns = inputSchema.numFields();
    final int[] validC = new int[numInputColumns];
    int j = 0;
    for (int i = 0; i < numInputColumns; i++) {
      // operate on index i here
      validC[j++] = i;
    }
    return validC;
  }

  @Override
  public List<Column> outputRawData() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public synchronized Schema outputSchema() {

    final int[] columnIndices = outputColumnIndices();
    final String[] columnNames = new String[columnIndices.length];
    final Type[] columnTypes = new Type[columnIndices.length];
    int j = 0;
    for (final int columnIndx : columnIndices) {
      columnNames[j] = inputSchema.getFieldName(columnIndx);
      columnTypes[j] = inputSchema.getFieldType(columnIndx);
      j++;
    }

    return new Schema(columnTypes, columnNames);
  }

  public synchronized JdbcTupleBatch[] partition(final PartitionFunction<?, ?> p) {
    return null;
  }

  @Override
  public TupleBatchBuffer[] partition(final PartitionFunction<?, ?> p, final TupleBatchBuffer[] buffers) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public synchronized JdbcTupleBatch project(final int[] remainingColumns) {
    return this;
  }

  @Override
  public synchronized _TupleBatch purgeFilters() {
    return this;
  }

  @Override
  public synchronized _TupleBatch purgeProjects() {
    return this;
  }

  @Override
  public _TupleBatch remove(final int innerIdx) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized _TupleBatch renameColumn(final int inputColumnIdx, final String newName) {
    return this;
  }

  @Override
  public synchronized _TupleBatch union(final _TupleBatch another) {
    // TODO Auto-generated method stub
    return null;
  }

}
