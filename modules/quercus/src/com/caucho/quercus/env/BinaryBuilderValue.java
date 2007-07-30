/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.quercus.env;

import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.IdentityHashMap;

/**
 * Represents a 8-bit PHP 6 style binary builder
 */
public class BinaryBuilderValue
  extends BytesBuilderValue
{
  public BinaryBuilderValue()
  {
  }

  public BinaryBuilderValue(byte[] buffer)
  {
    super(buffer);
  }

  public BinaryBuilderValue(Byte[] buffer)
  {
    super(buffer);
  }

  public BinaryBuilderValue(byte[] buffer, int offset, int length)
  {
    super(buffer, offset, length);
  }

  public BinaryBuilderValue(int capacity)
  {
    super(capacity);
  }

  /**
   * @param string the value as a Java string
   * @param encoding the original encoding of the binary data
   */
  public BinaryBuilderValue(String string, String encoding)
  {
    super(string, encoding);
  }

  @Override
  protected BytesValue copy(byte[] buffer, int offset, int length)
  {
    return new BinaryBuilderValue(buffer, offset, length);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  @Override
  public void generate(PrintWriter out)
    throws IOException
  {
    out.print("new BinaryBuilderValue(\"");
    printJavaString(out, this);
    out.print("\", \"UTF-8\")");
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    int length = length();

    out.print("binary(" + length() + ") \"");

    for (int i = 0; i < length; i++) {
      char ch = charAt(i);

      if (0x20 <= ch && ch < 0x7f)
        out.print(ch);
      else if (ch == '\r' || ch == '\n' || ch == '\t')
        out.print(ch);
      else
        out.print("\\x" + Integer.toHexString(ch >> 4) + Integer.toHexString(ch % 16));
    }

    out.print("\"");
  }

}
