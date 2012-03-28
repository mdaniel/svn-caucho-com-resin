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

import java.io.IOException;

/**
 * Represents a PHP string literal expression.
 */
public class LiteralUnicodeExprPro extends LiteralUnicodeExpr
  implements ExprPro
{
  protected LiteralUnicodeExprPro(String value)
  {
    super(Location.UNKNOWN, value);
  }

  /**
   * Returns the generator.
   */
  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      //
      // Java code generation
      //

      /**
       * Analyze the statement
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	return ExprType.VALUE;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	if (_value.toString().equals(""))
	  out.print("UnicodeBuilderValue.EMPTY");
	else {
	  String var = out.addValue(_value);

	  out.print(var);
	}
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateTop(PhpWriter out)
	throws IOException
      {
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateStatement(PhpWriter out)
	throws IOException
      {
      }

      /**
       * Generates code to evaluate the expression directly.
       *
       * @param out the writer to the Java source code.
       */
      public void generatePrint(PhpWriter out)
	throws IOException
      {
	if (_value.toString().equals(""))
	  return;

	generateGetOut(out);

	out.print(".print(\"");
	out.printJavaString(_value.toString());
	out.print("\")");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateString(PhpWriter out)
	throws IOException
      {
	out.print("\"");
	out.printJavaString(_value.toString());
	out.print("\"");
      }

      /**
       * Generates code to append to a string builder.
       *
       * @param out the writer to the Java source code.
       */
      public void generateAppend(PhpWriter out)
	    throws IOException
      {
        if (_value.length() == 1) {
          char ch = _value.charAt(0);

          if (ch ==  '\'')
            out.print("'\\''");
          else {
            out.print('\'');
            out.printJavaString(_value.toString());
            out.print('\'');
          }
        }
        else
          generate(out);
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
        throws IOException
      {
        out.print("new LiteralUnicodeExpr(\"");
        out.printJavaString(_value.toString());
        out.print("\")");
      }
    };
}

