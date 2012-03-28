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
public class ClassVirtualFieldVarExprPro
  extends ClassVirtualFieldVarExpr
  implements ExprPro
{
  public ClassVirtualFieldVarExprPro(Expr varName)
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
      // php/33lw
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
    public void generate(PhpWriter out)
    throws IOException
    {
      out.print("env.getStaticValue(");
      out.print("env.createStringBuilder()");
      out.print(".append(qClass.getName()).append(\"::\").append(");
      getName().generateAppend(out);
      out.print("))");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateRef(PhpWriter out)
    throws IOException
    {
      out.print("env.getStaticVar(");
      out.print("env.createStringBuilder()");
      out.print(".append(qClass.getName()).append(\"::\").append(");
      getName().generateAppend(out);
      out.print("))");
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
      out.print("env.error(env.getCallingClass(q_this).getName() + \"::$" + _varName + ": ");
      out.print("Cannot unset static variables.");
      out.print("\")");
    }
  };
}

