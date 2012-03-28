/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP static field reference.
 */
public class ClassFieldVarExprPro extends ClassFieldVarExpr
  implements ExprPro
{
  public ClassFieldVarExprPro(String className, Expr varName)
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
      // php/33ls
      return true;
    }

    /**
     * Analyze the statement
     */
    public ExprType analyze(AnalyzeInfo info)
    {
      getName().analyze(info);

      return ExprType.VALUE;
    }

    public ExprGenerator getName()
    {
      return ((ExprPro) _varName).getGenerator();
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
      out.print(_className);
      out.print("\").getStaticFieldValue(env, ");

      getName().generateStringValue(out);

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
      out.print("env.getClass(\"");
      out.print(_className);
      out.print("\").getStaticFieldVar(env, ");

      getName().generateStringValue(out);

      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
    {
      generateRef(out);

      out.print(".set(");

      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      valueGen.generateCopy(out);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
    {
      out.print("env.getClass(\"");
      out.print(_className);
      out.print("\").setStaticFieldRef(env, ");

      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      valueGen.generateRef(out);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateUnset(PhpWriter out)
    throws IOException
    {
      out.print("env.error(\"" + _className + "::$" + _varName + ": ");
      out.print("Cannot unset static variables.");
      out.print("\")");
    }
  };
}

