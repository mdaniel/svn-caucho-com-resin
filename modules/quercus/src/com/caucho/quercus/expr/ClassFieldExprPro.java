/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP static field reference.
 */
public class ClassFieldExprPro extends ClassFieldExpr
  implements ExprPro
{
  public ClassFieldExprPro(String className,
                               String varName)
  {
    super(className, varName);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
    @Override
    public boolean isVar()
    {
      // php/33lr
      return true;
    }

    /**
     * Analyze the statement
     */
    public ExprType analyze(AnalyzeInfo info)
    {
      return ExprType.VALUE;
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generate(PhpWriter out)
    throws IOException
    {
      out.print("env.getClass(\"");
      out.printJavaString(_className);
      out.print("\").getStaticFieldValue(env, ");
      out.printString(_varName);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    private void generateFieldVar(PhpWriter out)
    throws IOException
    {
      out.print("env.getClass(\"");
      out.printJavaString(_className);
      out.print("\").getStaticFieldVar(env, ");
      out.printString(_varName);
      out.print(")");
    }

    /**
     * Generates code to recreate the expression, creating an array
     * for an unset value.
     *
     * @param out the writer to the Java source code.
     */
    public void generateArray(PhpWriter out)
    throws IOException
    {
      generateFieldVar(out);
      out.print(".getArray()");
    }

    /**
     * Generates code to recreate the expression, creating an array
     * for an unset value.
     *
     * @param out the writer to the Java source code.
     */
    public void generateObject(PhpWriter out)
    throws IOException
    {
      generateFieldVar(out);
      out.print(".getObject(env)");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateRef(PhpWriter out)
    throws IOException
    {
      generateFieldVar(out);
    }

    /**
     * Generates code to recreate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateCopy(PhpWriter out)
    throws IOException
    {
      generateFieldVar(out);
      out.print(".copy()");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
    {
      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      generateFieldVar(out);
      out.print(".set(");

      valueGen.generateCopy(out);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
    {
      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      generateFieldVar(out);
      out.print(".set(");

      valueGen.generateRef(out);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateUnset(PhpWriter out)
    throws IOException
    {
      out.print("env.error(\"" + _className + "::$" + _varName + ": ");
      out.print("Cannot unset static variables.");
      out.print("\")");
    }
  };
}

