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

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Represents a PHP array[] reference expression.
 */
public class ArrayTailExprPro extends ArrayTailExpr
  implements ExprPro
{
  private static L10N L = new L10N(ArrayTailExprPro.class);

  public ArrayTailExprPro(Location location, Expr expr)
  {
    super(location, expr);
  }
  
  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      public ExprGenerator getExpr()
      {
        return ((ExprPro) _expr).getGenerator();
      }

      /**
       * Returns true for an expression that can be read (only $a[] uses this)
       */
      public boolean canRead()
      {
        return false;
      }

      /**
       * Analyze the statement
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        getExpr().analyze(info);
        getExpr().analyzeSetReference(info);
        getExpr().analyzeSetModified(info);

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
        // php/3d6d
        out.println("env.error(\"Cannot use [] as a read-value.\")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateArg(PhpWriter out, boolean isTop)
        throws IOException
      {
        getExpr().generateRef(out);
        out.print(".putVar()");
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
        getExpr().generateRef(out);
        out.print(".putVar()");
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
        getExpr().generateVar(out);
        out.print(".putVar()");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateArray(PhpWriter out)
        throws IOException
      {
        // quercus/3d1i
        getExpr().generateRef(out);
        out.print(".putArray()");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateObject(PhpWriter out)
        throws IOException
      {
        getExpr().generateRef(out);
        out.print(".putObject(env)");
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
        
        // php/3a55
        getExpr().generateRef(out);
        out.print(".put(");
        valueGen.generateCopy(out); // php/3a80
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
	
        // php/3a56
        getExpr().generateRef(out);
        out.print(".put(");
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
        throw new UnsupportedOperationException();
      }
    };
}

