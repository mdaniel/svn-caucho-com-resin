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

package com.caucho.quercus.env;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import java.util.IdentityHashMap;

import com.caucho.vfs.WriteStream;

/**
 * Represents a 8-bit binary string value.
 */
abstract public class BinaryValue extends StringValue {
  /**
   * Convert to a binary value.
   */
  @Override
  public BinaryValue toBinaryValue(Env env)
  {
    return this;
  }

  /**
   * Converts to a long.
   */
  public static long toLong(byte []buffer, int offset, int len)
  {
    if (len == 0)
      return 0;

    long value = 0;
    long sign = 1;

    int i = 0;
    int end = offset + len;

    if (buffer[offset] == '-') {
      sign = -1;
      offset++;
    }

    while (offset < end) {
      int ch = buffer[offset++];

      if ('0' <= ch && ch <= '9')
        value = 10 * value + ch - '0';
      else
        return sign * value;
    }

    return value;
  }

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
	out.print(charAt(i));
      else if (ch == '\r' || ch == '\n' || ch == '\t')
	out.print(charAt(i));
      else
	out.print("\\x" + Integer.toHexString(ch >> 4) + Integer.toHexString(ch % 16));
    }

    out.print("\"");
  }

  /**
   * Returns a byte stream.
   * @param charset ignored since BinaryValue has no set encoding
   */
  public InputStream toInputStream(String charset)
    throws UnsupportedEncodingException
  {
    return toInputStream();
  }

  /**
   * Returns a Unicode char stream.
   * @param charset encoding of the StringValue 
   */
  public Reader toReader(String charset)
    throws UnsupportedEncodingException
  {
    return new InputStreamReader(toInputStream(), charset);
  }

  abstract public byte[] toBytes();
}

