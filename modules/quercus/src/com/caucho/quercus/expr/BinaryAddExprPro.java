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

import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.gen.AnalyzeInfo;

import java.io.IOException;

/**
 * Represents a PHP add expression.
 */
public class BinaryAddExprPro extends BinaryAddExpr implements ExprPro {
  protected BinaryAddExprPro(Expr left, Expr right)
  {
    super(left, right);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractBinaryGenerateExpr(getLocation())
    {
      public ExprGenerator getLeft()
      {
	return ((ExprPro) _left).getGenerator();
      }
    
      public ExprGenerator getRight()
      {
	return ((ExprPro) _right).getGenerator();
      }
    
      //
      // Java code generation
      //

      /**
       * Return true for a double value
       */
      public boolean isDouble()
      {
        return getLeft().isDouble() || getRight().isDouble();
      }

      /**
       * Return true for a long value
       */
      @Override
      public boolean isLong()
      {
        return (getLeft().isLong() && getRight().isLong()
		|| getLeft().isBoolean() && getRight().isBoolean());
      }
      
      /*
       * Returns true for a boolean.
       */

      /**
       * Return true for a number
       */
      public boolean isNumber()
      {
	return true;
      }

      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	ExprType type = ExprType.LONG;

	type = type.withType(getLeft().analyze(info));
	type = type.withType(getRight().analyze(info));

	return type;
      }

      /**
       * Generates code to evaluate the expression as a long.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        if (isLong()) {
          if (getRight().isConstant() 
              && LongValue.ONE.equals(getRight().getConstant())) {
            getLeft().generate(out);
            out.print(".addOne()");
          }
          else {
            out.print("LongValue.create(");
            getLeft().generateLong(out);
            out.print(" + ");
            getRight().generateLong(out);
            out.print(")");
          }
        }
        else if (getLeft().isDouble() && getRight().isDouble()) {
          out.print("new DoubleValue(");
          getLeft().generateDouble(out);
          out.print(" + ");
          getRight().generateDouble(out);
          out.print(")");
        }
        else if (getLeft().isLong()) {
          getRight().generate(out);
          out.print(".add(");
          getLeft().generateLong(out);
          out.print(")");
        }
        else if (getRight().isLong()) {
          getLeft().generate(out);
          out.print(".add(");
          getRight().generateLong(out);
          out.print(")");
        }
        else {
          getLeft().generate(out);
          out.print(".add(");
          getRight().generate(out);
          out.print(")");
        }
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateLong(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getLeft().generateLong(out);
	out.print(" + ");
	getRight().generateLong(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateDouble(PhpWriter out)
	throws IOException
      {
	out.print("(");
	getLeft().generateDouble(out);
	out.print(" + ");
	getRight().generateDouble(out);
	out.print(")");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new com.caucho.quercus.expr.AddExpr(");
	getLeft().generateExpr(out);
	out.print(", ");
	getRight().generateExpr(out);
	out.print(")");
      }
    };
}

