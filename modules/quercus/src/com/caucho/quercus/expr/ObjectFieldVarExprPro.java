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
 * Represents a PHP field reference.
 */
public class ObjectFieldVarExprPro extends ObjectFieldVarExpr
  implements ExprPro
{
  public ObjectFieldVarExprPro(Expr objExpr, Expr nameExpr)
  {
    super(objExpr, nameExpr);
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
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      objGen.analyze(info);
      nameGen.analyze(info);

      return ExprType.VALUE;
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generate(PhpWriter out)
    throws IOException
    {
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      objGen.generate(out);
      out.print(".getField(env, ");
      nameGen.generateStringValue(out);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateArg(PhpWriter out, boolean isTop)
    throws IOException
    {
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      objGen.generateArg(out, false);
      out.print(".getFieldArg(env, ");
      nameGen.generateStringValue(out);
      out.print(", " + isTop + ")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateRef(PhpWriter out)
    throws IOException
    {
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      objGen.generateObject(out);
      out.print(".getFieldVar(env, ");
      nameGen.generateStringValue(out);
      out.print(")");
    }

    /**
     * Generates code to assign the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateAssign(PhpWriter out, Expr value, boolean isTop)
    throws IOException
    {
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();
      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      objGen.generateObject(out);
      out.print(".putField(env, ");
      nameGen.generateStringValue(out);
      out.print(", ");
      valueGen.generateCopy(out); // php/3a85
      out.print(")");
    }

    /**
     * Generates code to assign the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
    throws IOException
    {
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();
      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      objGen.generateObject(out);
      out.print(".putField(env, ");
      nameGen.generateStringValue(out);
      out.print(", ");
      valueGen.generateRef(out);
      out.print(")");
    }

    @Override
    public void generateAssignOpen(PhpWriter out)
    throws IOException
    {
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      objGen.generateObject(out);
      out.print(".putField(env, ");
      nameGen.generateStringValue(out);
      out.print(", ");
    }

    @Override
    public void generateAssignClose(PhpWriter out)
    throws IOException
    {
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateObject(PhpWriter out)
    throws IOException
    {
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      objGen.generateObject(out);
      out.print(".getFieldObject(env, ");
      nameGen.generateStringValue(out);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateArray(PhpWriter out)
    throws IOException
    {
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      objGen.generateObject(out);
      out.print(".getFieldArray(env, ");
      nameGen.generateStringValue(out);
      out.print(")");
    }

    /**
     * Generates code to assign the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateUnset(PhpWriter out)
    throws IOException
    {
      ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      objGen.generate(out);
      out.print(".unsetField(");
      nameGen.generateStringValue(out);
      out.print(")");
    }
  };
}

