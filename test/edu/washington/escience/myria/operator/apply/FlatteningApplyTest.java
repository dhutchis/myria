package edu.washington.escience.myria.operator.apply;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import edu.washington.escience.myria.DbException;
import edu.washington.escience.myria.Schema;
import edu.washington.escience.myria.Type;
import edu.washington.escience.myria.expression.ConstantExpression;
import edu.washington.escience.myria.expression.CounterExpression;
import edu.washington.escience.myria.expression.Expression;
import edu.washington.escience.myria.expression.ExpressionOperator;
import edu.washington.escience.myria.expression.SplitExpression;
import edu.washington.escience.myria.expression.VariableExpression;
import edu.washington.escience.myria.operator.FlatteningApply;
import edu.washington.escience.myria.operator.TupleSource;
import edu.washington.escience.myria.storage.TupleBatch;
import edu.washington.escience.myria.storage.TupleBatchBuffer;
import edu.washington.escience.myria.util.TestEnvVars;

public class FlatteningApplyTest {

  private final String SEPARATOR = ",";
  private final int SPLIT_MAX = 10;
  private final int COUNTER_MAX = 2 * TupleBatch.BATCH_SIZE + 1;
  private final int EXPECTED_RESULTS = SPLIT_MAX * COUNTER_MAX;

  @Test
  public void testApply() throws DbException {
    final Schema schema =
        Schema.ofFields("int_count", Type.INT_TYPE, "ignore_1", Type.FLOAT_TYPE, "joined_ints", Type.STRING_TYPE,
            "ignore_2", Type.BOOLEAN_TYPE);
    final Schema expectedResultSchema =
        Schema.ofFields("int_count", Type.INT_TYPE, "joined_ints", Type.STRING_TYPE, "int_values", Type.INT_TYPE,
            "joined_ints_splits", Type.STRING_TYPE);
    final TupleBatchBuffer input = new TupleBatchBuffer(schema);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < SPLIT_MAX; ++i) {
      sb.append(i);
      if (i < SPLIT_MAX - 1) {
        sb.append(SEPARATOR);
      }
    }
    final String joinedInts = sb.toString();

    input.putInt(0, COUNTER_MAX);
    input.putFloat(1, 1.0f);
    input.putString(2, joinedInts);
    input.putBoolean(3, true);
    ImmutableList.Builder<Expression> Expressions = ImmutableList.builder();

    ExpressionOperator countColIdx = new VariableExpression(0);
    ExpressionOperator counter = new CounterExpression(countColIdx);
    Expressions.add(new Expression("int_values", counter));

    ExpressionOperator splitColIdx = new VariableExpression(2);
    ExpressionOperator regex = new ConstantExpression(SEPARATOR);
    ExpressionOperator split = new SplitExpression(splitColIdx, regex);
    Expressions.add(new Expression("joined_ints_splits", split));

    FlatteningApply apply = new FlatteningApply(new TupleSource(input), Expressions.build(), ImmutableList.of(0, 2));
    apply.open(TestEnvVars.get());
    int rowIdx = 0;
    while (!apply.eos()) {
      TupleBatch result = apply.nextReady();
      if (result != null) {
        assertEquals(expectedResultSchema, result.getSchema());

        for (int batchIdx = 0; batchIdx < result.numTuples(); ++batchIdx, ++rowIdx) {
          assertEquals(COUNTER_MAX, result.getInt(0, batchIdx));
          assertEquals(joinedInts, result.getString(1, batchIdx));
          assertEquals((rowIdx / SPLIT_MAX), result.getInt(2, batchIdx));
          assertEquals((rowIdx % SPLIT_MAX), Integer.parseInt(result.getString(3, batchIdx)));
        }
      }
    }
    assertEquals(EXPECTED_RESULTS, rowIdx);
    apply.close();
  }
}
