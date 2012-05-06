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

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP variable expression.
 */
public class VarVarExprPro extends VarVarExpr
  implements ExprPro
{
  public VarVarExprPro(Expr var)
  {
    super(var);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      private ExprGenerator getVar()
      {
        return ((ExprPro) _var).getGenerator();
      }

      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        /*
        info.getFunction().setVariableVar(true);

        getVar().analyze(info);
        */

        // php/3d71
        analyzeAssign(info, getVar());

        return ExprType.VALUE;
      }
      /**
       * Analyze the expression
       */
      public ExprType analyzeAssign(AnalyzeInfo info, ExprGenerator expr)
      {
        info.getFunction().setVariableVar(true);

        getVar().analyzeAssign(info, expr);

        // php/3253
        info.clear();

        return ExprType.VALUE;
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        out.print("env.getValue(");
        getVar().generateStringValue(out);
        out.print(")");
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateRef(PhpWriter out)
        throws IOException
      {
        out.print("env.getVar(");
        getVar().generateStringValue(out);
        out.print(")");
      }

      /**
       * Generates code to evaluate the expression, copying the result
       *
       * @param out the writer to the Java source code.
       */
      public void generateCopy(PhpWriter out)
        throws IOException
      {
        generate(out);
        out.print(".copy()");  // php/3a5t
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateAssign(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        ExprGenerator valueGen = ((ExprPro) value).getGenerator();

        out.print("env.getVar(");
        getVar().generateString(out);
        out.print(").set(");
        valueGen.generateCopy(out); // php/3a84
        out.print(")");
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        ExprGenerator valueGen = ((ExprPro) value).getGenerator();

        // getVar().generateAssignRef(out, value, isTop);

        out.print("env.setRef(");
        getVar().generateStringValue(out);
        out.print(", ");
        valueGen.generateRef(out);
        out.print(")");
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateUnset(PhpWriter out)
        throws IOException
      {
        out.print("env.unsetVar(");
        getVar().generateStringValue(out);
        out.print(")");
      }
  };
}

