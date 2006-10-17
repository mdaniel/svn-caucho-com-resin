/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import java.io.IOException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.BinaryValue;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

/**
 * Represents a PHP string literal expression.
 */
public class BinaryLiteralExpr extends StringLiteralExpr
{
  public BinaryLiteralExpr(Location location, byte[] bytes)
  {
    super(location, new BinaryBuilderValue(bytes));
  }

  public BinaryLiteralExpr(Location location, BinaryValue value)
  {
    super(location, value);
  }

  public BinaryLiteralExpr(byte[] bytes)
  {
    this(Location.UNKNOWN, bytes);
  }

  public BinaryLiteralExpr(BinaryValue value)
  {
    this(Location.UNKNOWN, value);
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
      out.print("StringValue.EMPTY");
    else {
      String var = out.addValue(_value);

      out.print(var);
    }
  }

  /**
   * Generates code to append to a string builder.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAppend(PhpWriter out)
    throws IOException
  {
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
    out.print("new BinaryLiteralExpr(\"");
    out.printJavaString(_value.toString());
    out.print("\")");
  }
}

