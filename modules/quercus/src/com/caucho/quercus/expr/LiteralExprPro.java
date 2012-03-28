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

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents a literal expression.
 */
public class LiteralExprPro extends LiteralExpr
  implements ExprPro
{
  protected LiteralExprPro(Value value)
  {
    super(value);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      /**
       * Returns true for a literal expression.
       */
      @Override
      public boolean isLiteral()
      {
        return true;
      }

      /**
       * Returns the literal value
       */
      @Override
      public Object getLiteral()
      {
        return getValue();
      }

      /**
       * Returns true if a static true value.
       */
      public boolean isTrue()
      {
        if (getValue() == BooleanValue.TRUE)
          return true;
        else if (getValue() instanceof LongValue)
          return getValue().toLong() != 0;
        else
          return false;
      }

      /**
       * Returns true if a static true value.
       */
      public boolean isFalse()
      {
        if (getValue() == BooleanValue.FALSE)
          return true;
        else if (getValue() instanceof LongValue)
          return getValue().toLong() == 0;
        else
          return false;
      }

      /**
       * Returns true for a long value.
       */
      @Override
      public boolean isLong()
      {
        return getType().isLong();
      }

      /**
       * Returns true for a double value.
       */
      @Override
      public boolean isDouble()
      {
        return getType().isDouble();
      }
      
      /*
       * Returns true for a boolean value.
       */
      @Override
      public boolean isBoolean()
      {
        return getType().isBoolean();
      }

      /**
       * Returns the static, analyzed type
       */
      public ExprType getType()
      {
        if (getValue() instanceof LongValue)
          return ExprType.LONG;
        else if (getValue() instanceof DoubleValue)
          return ExprType.DOUBLE;
        else if (getValue() instanceof BooleanValue)
          return ExprType.BOOLEAN;
        else
          return ExprType.VALUE;
      }

      /**
       * Analyze the statement
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        return getType();
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generate(PhpWriter out)
        throws IOException
      {
        out.print(out.addValue(getValue()));
      }
      
      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateRef(PhpWriter out)
        throws IOException
      {
        out.print("new Var(");
        generate(out);
        out.print(")");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateTop(PhpWriter out)
        throws IOException
      {
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateStatement(PhpWriter out)
        throws IOException
      {
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateString(PhpWriter out)
        throws IOException
      {
        try {
          out.print("\"");
          out.printJavaString(getValue().toString());
          out.print("\"");
        } catch (IOException e) {
          throw e;
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateDouble(PhpWriter out)
        throws IOException
      {
        out.print(getValue().toDouble());
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateLong(PhpWriter out)
        throws IOException
      {
        out.print(getValue().toLong() + "L");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateBoolean(PhpWriter out)
        throws IOException
      {
        // php/3627
        if (getValue().toBoolean()) {
          out.print("BooleanValue.TRUE.toBoolean()");
        }
        else {
          out.print("BooleanValue.FALSE.toBoolean()");
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
        out.print("new com.caucho.quercus.expr.LiteralExpr(");

        PrintWriter writer = new PrintWriter(out);
        getValue().generate(writer);
        writer.close();

        out.print(")");
      }
    };
}
