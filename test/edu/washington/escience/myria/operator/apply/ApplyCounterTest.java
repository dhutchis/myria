package edu.washington.escience.myria.operator.apply;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import edu.washington.escience.myria.DbException;
import edu.washington.escience.myria.Schema;
import edu.washington.escience.myria.Type;
import edu.washington.escience.myria.expression.CounterExpression;
import edu.washington.escience.myria.expression.Expression;
import edu.washington.escience.myria.expression.ExpressionOperator;
import edu.washington.escience.myria.expression.VariableExpression;
import edu.washington.escience.myria.operator.FlatteningApply;
import edu.washington.escience.myria.operator.TupleSource;
import edu.washington.escience.myria.storage.TupleBatch;
import edu.washington.escience.myria.storage.TupleBatchBuffer;
import edu.washington.escience.myria.util.TestEnvVars;

public class ApplyCounterTest {
  private final int COUNT = 2 * TupleBatch.BATCH_SIZE + 1;

  @Test
  public void testApply() throws DbException {
    final Schema schema = Schema.ofFields("int_count", Type.INT_TYPE);
    final Schema expectedResultSchema = Schema.ofFields("int_values", Type.INT_TYPE);
    final TupleBatchBuffer input = new TupleBatchBuffer(schema);

    input.putInt(0, COUNT);

    ImmutableList.Builder<Expression> Expressions = ImmutableList.builder();
    ExpressionOperator colIdx = new VariableExpression(0);
    ExpressionOperator split = new CounterExpression(colIdx);
    Expression expr = new Expression("int_values", split);
    Expressions.add(expr);

    FlatteningApply apply = new FlatteningApply(new TupleSource(input), Expressions.build(), null);
    apply.open(TestEnvVars.get());
    int rowIdx = 0;
    while (!apply.eos()) {
      TupleBatch result = apply.nextReady();
      if (result != null) {
        assertEquals(expectedResultSchema, result.getSchema());

        for (int batchIdx = 0; batchIdx < result.numTuples(); ++batchIdx, ++rowIdx) {
          assertEquals(rowIdx, result.getInt(0, batchIdx));
        }
      }
    }
    assertEquals(COUNT, rowIdx);
    apply.close();
  }
}
