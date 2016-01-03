/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.protocol;

import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.OutputStreamWithBuffer;
import com.caucho.v5.vfs.PathImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * API for handling the PrintWriter/ServletOutputStream
 */
public class OutResponseStub extends OutResponseBase {
  private final byte []_byteBuffer = new byte[16];
  private final char []_charBuffer = new char[16];
  
  /**
   * Returns true for a Caucho response stream.
   */
  @Override
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  /**
   * Sets the buffer size.
   */
  @Override
  public void setBufferCapacity(int size)
  {
  }

  /**
   * Gets the buffer size.
   */
  @Override
  public int getBufferCapacity()
  {
    return 0;
  }

  /**
   * Returns the remaining buffer entries.
   */
  @Override
  public int getRemaining()
  {
    return 0;
  }
  /**
   * Returns the stream's buffer.
   */
  public byte []getBuffer()
    throws IOException
  {
    return _byteBuffer;
  }
  
  /**
   * Returns the stream's buffer offset.
   */
  public int getBufferOffset()
    throws IOException
  {
    return 0;
  }
  
  /**
   * Sets the stream's buffer length.
   */
  public void setBufferOffset(int offset)
    throws IOException
  {
  }
  
  /**
   * Returns the next buffer.
   *
   * @param length the length of the completed buffer
   *
   * @return the next buffer
   */
  public byte []nextBuffer(int offset)
    throws IOException
  {
    return _byteBuffer;
  }

  /**
   * Returns the char buffer.
   */
  @Override
  public char []getCharBuffer()
    throws IOException
  {
    return _charBuffer;
  }

  /**
   * Returns the char buffer offset.
   */
  @Override
  public int getCharOffset()
    throws IOException
  {
    return 0;
  }

  /**
   * Sets the char buffer offset.
   */
  @Override
  public void setCharOffset(int offset)
    throws IOException
  {
  }

  /**
   * Returns the next char buffer.
   */
  @Override
  public char []nextCharBuffer(int offset)
    throws IOException
  {
    return _charBuffer;
  }
  
  /**
   * Writes a byte to the output.
   */
  @Override
  public void write(int v)
    throws IOException
  {
  }

  /**
   * Writes a byte array to the output.
   */
  @Override
  public void write(byte []buffer, int offset, int length)
  {
  }

  /**
   * Writes a character to the output.
   */
  @Override
  public void print(int ch)
    throws IOException
  {
  }

  /**
   * Writes a char array to the output.
   */
  @Override
  public void print(char []buffer, int offset, int length)
    throws IOException
  {
  }

  /**
   * Writes a char array to the output.
   */
  @Override
  public void print(String v)
    throws IOException
  {
  }

  /**
   * Clears the output buffer.
   */
  @Override
  public void clearBuffer()
  {
  }

  /**
   * Flushes the output buffer.
   */
  @Override
  public void flushBuffer()
    throws IOException
  {
  }
}
