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

package com.caucho.util;

import java.util.*;
import java.io.*;

/**
 * Base64 decoding.
 */
public class Base64 {
  static int decode[];

  static {
    decode = new int[256];
    for (int i = 'A'; i <= 'Z'; i++)
      decode[i] = i - 'A';
    for (int i = 'a'; i <= 'z'; i++)
      decode[i] = i - 'a' + 26;
    for (int i = '0'; i <= '9'; i++)
      decode[i] = i - '0' + 52;
    decode['+'] = 62;
    decode['/'] = 63;
  }

  public static void encode(CharBuffer cb, long data)
  {
    cb.append(Base64.encode(data >> 60));
    cb.append(Base64.encode(data >> 54));
    cb.append(Base64.encode(data >> 48));
    cb.append(Base64.encode(data >> 42));
    cb.append(Base64.encode(data >> 36));
    cb.append(Base64.encode(data >> 30));
    cb.append(Base64.encode(data >> 24));
    cb.append(Base64.encode(data >> 18));
    cb.append(Base64.encode(data >> 12));
    cb.append(Base64.encode(data >> 6));
    cb.append(Base64.encode(data));
  }

  public static void encode24(CharBuffer cb, int data)
  {
    cb.append(Base64.encode(data >> 18));
    cb.append(Base64.encode(data >> 12));
    cb.append(Base64.encode(data >> 6));
    cb.append(Base64.encode(data));
  }

  public static void encode(CharBuffer cb, byte []buffer,
                            int offset, int length)
  {
    while (length >= 3) {
      int data = (buffer[offset] & 0xff) << 16;
      data += (buffer[offset + 1] & 0xff) << 8;
      data += (buffer[offset + 2] & 0xff);
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append(Base64.encode(data));

      offset += 3;
      length -= 3;
    }

    if (length == 2) {
      int b1 = buffer[offset] & 0xff;
      int b2 = buffer[offset + 1] & 0xff;
      
      int data = (b1 << 16) + (b2 << 8);
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append('=');
    }
    else if (length == 1) {
      int data = (buffer[offset] & 0xff) << 16;
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append('=');
      cb.append('=');
    }
  }

  public static void oldEncode(CharBuffer cb, byte []buffer,
			       int offset, int length)
  {
    while (length >= 3) {
      int data = (buffer[offset] & 0xff) << 16;
      data += (buffer[offset + 1] & 0xff) << 8;
      data += (buffer[offset + 2] & 0xff);
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append(Base64.encode(data));

      offset += 3;
      length -= 3;
    }

    if (length == 2) {
      int b1 = buffer[offset] & 0xff;
      int b2 = buffer[offset + 1] & 0xff;
      
      int data = (b1 << 8) + (b2);
      
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append(Base64.encode(data));
      cb.append('=');
    }
    else if (length == 1) {
      int data = (buffer[offset] & 0xff);
      
      cb.append(Base64.encode(data >> 6));
      cb.append(Base64.encode(data));
      cb.append('=');
      cb.append('=');
    }
  }

  public static char encode(long d)
  {
    d &= 0x3f;
    if (d < 26)
      return (char) (d + 'A');
    else if (d < 52)
      return (char) (d + 'a' - 26);
    else if (d < 62)
      return (char) (d + '0' - 52);
    else if (d == 62)
      return '+';
    else
      return '/';
  }

  public static int decode(int d)
  {
    return decode[d];
  }

  public static String encode(String value)
  {
    CharBuffer cb = new CharBuffer();

    int i = 0;
    for (i = 0; i + 2 < value.length(); i += 3) {
      long chunk = (int) value.charAt(i);
      chunk = (chunk << 8) + (int) value.charAt(i + 1);
      chunk = (chunk << 8) + (int) value.charAt(i + 2);
        
      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append(encode(chunk >> 6));
      cb.append(encode(chunk));
    }
    
    if (i + 1 < value.length()) {
      long chunk = (int) value.charAt(i);
      chunk = (chunk << 8) + (int) value.charAt(i + 1);
      chunk <<= 8;

      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append(encode(chunk >> 6));
      cb.append('=');
    }
    else if (i < value.length()) {
      long chunk = (int) value.charAt(i);
      chunk <<= 16;

      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append('=');
      cb.append('=');
    }

    return cb.toString();
  }

  public static String encodeFromByteArray(byte[] value)
  {
    CharBuffer cb = new CharBuffer();

    int i = 0;
    for (i = 0; i + 2 < value.length; i += 3) {
      long chunk = (value[i] & 0xff);
      chunk = (chunk << 8) + (value[i+1] & 0xff);
      chunk = (chunk << 8) + (value[i+2] & 0xff);
        
      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append(encode(chunk >> 6));
      cb.append(encode(chunk));
    }
    
    if (i + 1 < value.length) {
      long chunk = (value[i] & 0xff);
      chunk = (chunk << 8) + (value[i+1] & 0xff);
      chunk <<= 8;

      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append(encode(chunk >> 6));
      cb.append('=');
    }
    else if (i < value.length) {
      long chunk = (value[i] & 0xff);
      chunk <<= 16;

      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append('=');
      cb.append('=');
    }

    return cb.toString();
  }

  public static String decode(String value)
  {
    CharBuffer cb = new CharBuffer();

    int length = value.length();
    for (int i = 0; i + 3 < length; i += 4) {
      int ch0 = value.charAt(i + 0) & 0xff;

      // skip whitespace
      if (ch0 == ' ' || ch0 == '\n' || ch0 == '\r') {
        i -= 3;
        continue;
      }
      
      int ch1 = value.charAt(i + 1) & 0xff;
      int ch2 = value.charAt(i + 2) & 0xff;
      int ch3 = value.charAt(i + 3) & 0xff;

      int chunk = ((decode[ch0] << 18) +
		   (decode[ch1] << 12) +
		   (decode[ch2] << 6) +
		   (decode[ch3]));

      cb.append((char) ((chunk >> 16) & 0xff));

      if (ch2 != '=')
	cb.append((char) ((chunk >> 8) & 0xff));
      if (ch3 != '=')
	cb.append((char) ((chunk & 0xff)));
    }

    return cb.toString();
  }

  public static byte[] decodeToByteArray(String value)
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    int length = value.length();
    for (int i = 0; i + 3 < length; i += 4) {
      int ch0 = value.charAt(i + 0) & 0xff;
      
      // skip whitespace
      if (ch0 == ' ' || ch0 == '\n' || ch0 == '\r') {
	i -= 3;
	continue;
      }
      
      int ch1 = value.charAt(i + 1) & 0xff;
      int ch2 = value.charAt(i + 2) & 0xff;
      int ch3 = value.charAt(i + 3) & 0xff;
      
      int chunk = ((decode[ch0] << 18) +
		   (decode[ch1] << 12) +
		   (decode[ch2] << 6) +
		   (decode[ch3]));
      
      baos.write((byte) ((chunk >> 16) & 0xff));
      
      if (ch2 != '=')
	baos.write((byte) ((chunk >> 8) & 0xff));
      if (ch3 != '=')
	baos.write((byte) ((chunk & 0xff)));
    }
    try {
      baos.flush();
      baos.close();
    } catch (IOException ioe) {
      throw new RuntimeException("this should not be possible");
    }
    return baos.toByteArray();
  }
}
