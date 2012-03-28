/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import java.io.IOException;

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a PHP static field reference.
 */
public class ClassVirtualFieldExprPro
  extends ClassVirtualFieldExpr
  implements ExprPro
{
  public ClassVirtualFieldExprPro(String varName)
  {
    super(varName);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
    @Override
    public boolean isVar()
    {
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
      out.print("qClass.getStaticFieldValue(env, ");
      out.printString(_varName);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateRef(PhpWriter out)
    throws IOException
    {
      out.print("qClass.getStaticFieldVar(env, ");
      out.printString(_varName);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateVar(PhpWriter out)
    throws IOException
    {
      out.print("qClass.getStaticFieldVar(env, ");
      out.printString(_varName);
      out.print(")");
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

      generateRef(out);
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

      generateRef(out);
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
      out.print("env.error(env.getCallingClassName() + \"::$" + _varName + ": ");
      out.print("Cannot unset static variables.");
      out.print("\")");
    }
  };
}

