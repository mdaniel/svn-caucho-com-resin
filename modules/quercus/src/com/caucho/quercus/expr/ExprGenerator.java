/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents compilable PHP expression.
 */
abstract public class ExprGenerator {
  private static final L10N L = new L10N(ExprGenerator.class);
  private static final Logger log
    = Logger.getLogger(ExprGenerator.class.getName());

  private Location _location = Location.UNKNOWN;

  public static final int COMPILE_ARG_MAX = 5;

  public ExprGenerator()
  {
  }

  public ExprGenerator(Location location)
  {
    _location = location;
  }

  //
  // Java code generation
  //

  /**
   * Properties.
   */

  public Location getLocation()
  {
    return _location;
  }

  /**
   * Returns true for a long-valued expression.
   */
  public boolean isLong()
  {
    return getType().isLong();
  }

  /**
   * Returns true for a double-valued expression.
   */
  public boolean isDouble()
  {
    return getType().isDouble();
  }

  /**
   * Returns true for a number.
   */
  public boolean isNumber()
  {
    return isLong() || isDouble();
  }

  /*
   * Returns true for a boolean.
   */
  public boolean isBoolean()
  {
    return getType().isBoolean();
  }

  /**
   * Returns true for a String-valued expression.
   */
  public boolean isString()
  {
    return getType().isString();
  }

  /**
   * Returns true for an assignment expression.
   */
  public boolean isAssignment()
  {
    return false;
  }

  /**
   * Returns true for a literal
   */
  public boolean isLiteral()
  {
    return false;
  }

  /**
   * Returns true for a constant.
   */
  public boolean isConstant()
  {
    return isLiteral();
  }
  
  /**
   * Returns true for a Var, i.e. an Expr which will return a Var for
   * generateRef
   */
  public boolean isVar()
  {
    return false;
  }

  public boolean isDefault()
  {
    return false;
  }

  /**
   * Returns the literal value
   */
  public Object getLiteral()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the constant value
   */
  public Object getConstant()
  {
    return getLiteral();
  }

  /**
   * Returns the static, analyzed type
   */
  public ExprType getType()
  {
    return ExprType.VALUE;
  }

  /**
   * Analyze the expression
   */
  abstract public ExprType analyze(AnalyzeInfo info);

  /**
   * Analyze the expression as a statement
   */
  public void analyzeTop(AnalyzeInfo info)
  {
    analyze(info);
  }

  /**
   * Analyze the expression
   */
  public ExprType analyzeAssign(AnalyzeInfo info, ExprGenerator value)
  {
    analyze(info);

    value.analyze(info);

    return ExprType.VALUE;
  }

  /**
   * Analyze as modified.
   */
  public void analyzeSetModified(AnalyzeInfo info)
  {
    analyze(info);
  }

  /**
   * Analyze as reference
   */
  public void analyzeSetReference(AnalyzeInfo info)
  {
    analyze(info);
  }

  /**
   * Set a post increment
   */
  public void analyzeSetPostIncrement()
  {
  }

  /**
   * Analyze as unset
   */
  public void analyzeUnset(AnalyzeInfo info)
  {
    analyze(info);
  }

  /**
   * Returns the variables state.
   *
   * @param leftState the variables to test
   * @param rightState the owning expression
   */
  protected VarState combineBinaryVarState(VarState leftState,
                                           VarState rightState)
  {
    if (leftState == VarState.UNSET || leftState == VarState.UNDEFINED)
      return leftState;

    if (rightState == VarState.UNSET || rightState == VarState.UNDEFINED)
      return rightState;

    if (leftState == VarState.VALID || rightState == VarState.VALID)
      return VarState.VALID;
    else
      return VarState.UNKNOWN;
  }

  /**
   * Returns true if the variable is ever assigned.
   *
   * @param var the variable to test
   */
  public boolean isVarAssigned(VarExpr var)
  {
    return false;
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates an expression for isset().
   */
  public void generateIsset(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".isset()");
  }

  /**
   * Generates code to eval the expression when the content might
   * be modified, e.g. when evaluating $a[0], $a needs to use generateModified
   */
  public void generateModifiedRead(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to evaluate the expression, expecting a modification.
   *
   * @param out the writer to the Java source code.
   */
  public void generateDirty(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code for a function arg, where the declaration of the
   * argument is unknown.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArg(PhpWriter out, boolean isTop)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code for a call arg, where the function is known to
   * take a value, i.e. a fun($x) declaration.
   *
   * @param out the writer to the Java source code.
   */
  public void generateValueArg(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code for a function arg.
   *
   * @param out the writer to the Java source code.
   */
  public void generateValue(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code for a reference.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out)
    throws IOException
  {
    // php/3243
    // php/33lg

    /* php/3248
    out.print("env.toRefArgument(");
    generate(out);
    out.print(")");
    */

    generate(out);
  }

  /**
   * Generates code for a reference that can be used as an array.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArray(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.println(".toArray()");
  }
  
  /**
   * Generates code for an array assignment $a[$index] = $value.
   */
  public void generateArrayAssign(PhpWriter out,
                                  ExprGenerator index,
                                  ExprGenerator value,
                                  boolean isTop)
    throws IOException
  {
    generateArray(out);
    
    if (isTop) {
      out.print(".append(");
    }
    else {
      out.print(".put(");
    }
    
    index.generate(out);
    out.print(", ");
    
    value.generateCopy(out); // php/3a5k
    out.print(")");
  }

  /**
   * Generates code for a reference.
   *
   * @param out the writer to the Java source code.
   */
  public void generateVar(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.println(".toVar()");
  }

  /**
   * Generates code for a return reference.
   *
   * @param out the writer to the Java source code.
   */
  public void generateReturnRef(PhpWriter out)
    throws IOException
  {
    // php/3c3d
    generateVar(out);
  }

  /**
   * Generates code for a reference.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRefArg(PhpWriter out)
    throws IOException
  {
    // php/3243
    // php/33lg

    generateRef(out);
    out.print(".toArgRef()");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to recreate the expression, creating an array
   * for an unset value.
   *
   * @param out the writer to the Java source code.
   */
  /*
  public void generateArray(PhpWriter out)
    throws IOException
  {
    generate(out);
  }
  */

  /**
   * Generates code to recreate the expression, creating an object
   * for an unset value.
   *
   * @param out the writer to the Java source code.
   */
  public void generateObject(PhpWriter out)
    throws IOException
  {
    generateRef(out);
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssign(PhpWriter out, Expr value,
                             boolean isTop)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName() + " is not a valid left-hand side.");
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignOpen(PhpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignClose(PhpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates code to evaluate the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateAssignBoolean(PhpWriter out, Expr value, boolean isTop)
    throws IOException
  {
    generateAssign(out, value, isTop);
    out.println(".toBoolean()");
  }

  /**
   * Generates code to unset the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateUnset(PhpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates code to unset the expression
   *
   * @param out the writer to the Java source code.
   */
  public void generateUnsetArray(PhpWriter out, ExprGenerator index)
    throws IOException
  {
    generateDirty(out);
    out.print(".remove(");
    index.generate(out);
    out.print(")");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateTop(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code for a return value
   *
   * @param out the writer to the Java source code.
   */
  public void generateReturn(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateStatement(PhpWriter out)
    throws IOException
  {
    generateTop(out);
    out.println(";");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateListEachStatement(PhpWriter out, Expr value)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates code to evaluate a boolean directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toBoolean()");
  }

  /**
   * Generates code to evaluate a string directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateString(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toString()");
  }

  /**
   * Generates code to evaluate a string directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateStringValue(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toStringValue(env)");
  }

  /**
   * Generates code to append to a string builder.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAppend(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to evaluate a string directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateChar(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toChar()");
  }

  /**
   * Generates code to evaluate the expression directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateInt(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toInt()");
  }

  /**
   * Generates code to evaluate the expression directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateLong(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toLong()");
  }

  /**
   * Generates code to evaluate the expression directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateDouble(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toDouble()");
  }

  /**
   * Generates code for the native type
   *
   * @param out the writer to the Java source code.
   */
  public void generateType(PhpWriter out)
    throws IOException
  {
    switch (getType()) {
    case LONG:
      generateLong(out);
      break;
    case DOUBLE:
      generateDouble(out);
      break;
    case BOOLEAN:
      generateBoolean(out);
      break;
    case STRING:
      generateStringValue(out);
    default:
      generate(out);
      break;
    }
  }

  /**
   * Generates code to get the out.
   */
  public void generateGetOut(PhpWriter out)
    throws IOException
  {
    // quercus/1l07
    // out.print("_quercus_out");

    // out.print("env.getOut()");
    out.print("env");
  }

  /**
   * Generates code to print the expression directly
   *
   * @param out the writer to the Java source code.
   */
  public void generatePrint(PhpWriter out)
    throws IOException
  {
    if (isLong()) {
      out.print("env.print(");
      generateLong(out);
      out.print(")");
    }
    else if (isDouble()) {
      out.print("env.print(");
      generateDouble(out);
      out.print(")");
    }
    /* php/3a04
    else if (isBoolean()) {
      out.print("env.print(");
      generateBoolean(out);
      out.print(")");
    }
    */
    else if (isString()) {
      out.print("env.print(");
      generateString(out);
      out.print(")");
    }
    else {
      generate(out);
      out.print(".print(env)");
    }
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}

