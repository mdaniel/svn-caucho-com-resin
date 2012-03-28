/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.ConstStringValue;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a list assignment expression.
 */
public class ListHeadExprPro extends ListHeadExpr
  implements ExprPro
{
  private String _varName;
  
  public ListHeadExprPro(ArrayList<Expr> varList)
  {
    super(varList);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  public ExprGenerator getListEachGenerator()
  {
    return LIST_EACH_GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {

    /**
     * Analyze the expression
     */
    public ExprType analyze(AnalyzeInfo info)
    {
      _varName
        = "q_list_" + info.getFunction().getTempIndex();

      info.getFunction().addTempVar(_varName);

      for (int i = 0; i < _varList.length; i++) {
        ExprPro var = (ExprPro) _varList[i];

        if (var != null)
          var.getGenerator().analyzeAssign(info, new DummyGenerator());
      }

      return ExprType.VALUE;
    }

    /**
     * Generates code to evaluate the expression
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateAssign(PhpWriter out, Expr value, boolean isTop)
      throws IOException
    {
      out.print("env.first(" + _varName + " = ");

      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      valueGen.generate(out);

      InfoVarPro varInfo = new InfoVarTempPro(new ConstStringValue(_varName));

      VarExprPro varExpr = new VarTempExprPro(varInfo);

      varExpr.setVarState(VarState.VALID);

      int count = 1;

      for (int i = 0; i < _varList.length; i++) {
        ExprPro var = (ExprPro) _varList[i];

        if (var == null)
          continue;

        out.print(", ");

        if (i > 0 && i % 4 == 0) {
          out.print("env.first(");
          count++;
        }

        AbstractVarExpr refExpr
          = new ArrayGetExprPro(getLocation(), varExpr,
                                new LiteralLongExprPro(i));

        if (_varList[i] instanceof AbstractVarExpr)
          var.getGenerator().generateAssign(out, refExpr, false);
        else
          var.getGenerator().generateAssign(out, refExpr, false);
      }

      for (; count > 0; count--)
        out.print(")");
    }

    /**
     * Generates code to evaluate the expression
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generate(PhpWriter out)
      throws IOException
    {
      throw new UnsupportedOperationException();
    }
  };

  private ExprGenerator LIST_EACH_GENERATOR = new ExprGenerator(getLocation()) {

    /**
     * Analyze the expression
     */
    public ExprType analyze(AnalyzeInfo info)
    {
      _varName
        = "q_list_" + info.getFunction().getTempIndex();

      info.getFunction().addTempVar(_varName);

      for (int i = 0; i < _varList.length; i++) {
        ExprPro var = (ExprPro) _varList[i];

        if (var != null)
          var.getGenerator().analyzeAssign(info, new DummyGenerator());
      }

      return ExprType.VALUE;
    }

    /**
     * Generates code to evaluate the expression
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateAssign(PhpWriter out, Expr value, boolean isTop)
      throws IOException
    {      
      ExprPro valuePro = (ExprPro) value;
      
      if (! valuePro.getGenerator().isVar()) {
        out.print("env.error(\"list() expression for a loop must be a variable at '");
        out.printJavaString(valuePro.toString());
        out.print("\")");
        return;
      }

      out.print("((" + _varName + " = ");
      
      valuePro.getGenerator().generateRef(out);
      out.print(").hasCurrent() ? ");

      out.print("env.first(BooleanValue.TRUE");

      if (_varList.length > 0 && _varList[0] != null) {
        ExprPro keyVar = (ExprPro) _varList[0];
        out.print(", ");
        keyVar.getGenerator().generateAssign(out, 
                                             new ProEachKeyExpr(_varName), 
                                             false);
      }

      if (_varList.length > 1 && _varList[1] != null) {
        ExprPro valueVar = (ExprPro) _varList[1];
        out.print(", ");
        valueVar.getGenerator().generateAssign(out,
                                               new ProEachValueExpr(_varName),
                                               false);
      }

      out.print(", " + _varName + ".next()");

      out.print(") : BooleanValue.FALSE)");
    }

    /**
     * Generates code to evaluate the expression
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generate(PhpWriter out)
      throws IOException
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Generates code to evaluate the expression
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateListEachStatement(PhpWriter out, Expr value)
      throws IOException
    {
      ExprPro valuePro = (ExprPro) value;

      out.print(_varName + " = ");
      valuePro.getGenerator().generateRef(out);
      out.println(";");

      if (_varList.length > 0 && _varList[0] != null) {
        ExprPro keyVar = (ExprPro) _varList[0];
        keyVar.getGenerator().generateAssign(out, 
                                             new ProEachKeyExpr(_varName), 
                                             false);
        out.println(";");
      }

      if (_varList.length > 1 && _varList[1] != null) {
        ExprPro valueVar = (ExprPro) _varList[1];
        valueVar.getGenerator().generateAssign(out,
                                               new ProEachValueExpr(_varName),
                                               false);
        out.println(";");
      }

      out.println(_varName + ".next();");
    }
  };

  static class ProEachKeyExpr extends Expr implements ExprPro {
    private String _var;

    ProEachKeyExpr(String var)
    {
      _var = var;
    }

    public Value eval(Env env)
    {
      throw new UnsupportedOperationException();
    }

    public ExprGenerator getGenerator()
    {
      return GENERATOR;
    }

    private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
        return ExprType.VALUE;
      }

      @Override
      public void generate(PhpWriter out)
      throws IOException
      {
        out.print(_var + ".key()");
      }

      public void generateExpr(PhpWriter out)
      throws IOException
      {
        out.print("null");
      }
    };
  }

  static class ProEachValueExpr extends Expr implements ExprPro {
    private String _var;

    ProEachValueExpr(String var)
    {
      _var = var;
    }

    public Value eval(Env env)
    {
      throw new UnsupportedOperationException();
    }

    public ExprGenerator getGenerator()
    {
      return GENERATOR;
    }

    private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      public ExprType analyze(AnalyzeInfo info)
      {
        return ExprType.VALUE;
      }

      @Override
      public void generate(PhpWriter out)
      throws IOException
      {
        out.print(_var + ".current()");
      }

      @Override
      public void generateCopy(PhpWriter out)
      throws IOException
      {
        out.print(_var + ".current().copy()");
      }

      public void generateExpr(PhpWriter out)
      throws IOException
      {
        out.print("null");
      }
    };
  }
}

