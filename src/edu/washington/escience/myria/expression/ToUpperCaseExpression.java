package edu.washington.escience.myria.expression;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import edu.washington.escience.myria.Schema;
import edu.washington.escience.myria.Type;

/**
 * Return the upper case representation of the operand.
 */
public class ToUpperCaseExpression extends UnaryExpression {
  /***/
  private static final long serialVersionUID = 1L;

  /**
   * This is not really unused, it's used automagically by Jackson deserialization.
   */
  @SuppressWarnings("unused")
  private ToUpperCaseExpression() {
    super();
  }

  /**
   * Change all characters in a string to upper case.
   * 
   * @param operand the operand.
   */
  public ToUpperCaseExpression(final ExpressionOperator operand) {
    super(operand);
  }

  @Override
  public Type getOutputType(final Schema schema) {
    Type operandType = getOperand().getOutputType(schema);
    ImmutableList<Type> validTypes = ImmutableList.of(Type.STRING_TYPE);
    int operandIdx = validTypes.indexOf(operandType);
    Preconditions.checkArgument(operandIdx != -1, "%s cannot handle operand [%s] of Type %s", getClass()
        .getSimpleName(), getOperand(), operandType);
    return Type.STRING_TYPE;
  }

  @Override
  public String getJavaString(final Schema schema) {
    return getDotFunctionCallUnaryString(".toUpperCase()", schema);
  }
}