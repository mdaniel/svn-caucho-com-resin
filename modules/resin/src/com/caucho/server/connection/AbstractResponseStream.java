/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.connection;

import com.caucho.util.L10N;
import com.caucho.vfs.OutputStreamWithBuffer;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * API for handling the PrintWriter/ServletOutputStream
 */
public abstract class AbstractResponseStream extends OutputStreamWithBuffer {
  private static final Logger log
    = Logger.getLogger(AbstractResponseStream.class.getName());
  private static final L10N L = new L10N(AbstractResponseStream.class);

  /**
   * Starts the response stream.
   */
  public void start()
  {
  }

  /**
   * Returns true for a Caucho response stream.
   */
  abstract public boolean isCauchoResponseStream();

  /**
   * Returns true if data has been sent
   */
  public boolean isFlushed()
  {
    return false;
  }
  
  /**
   * Sets the encoding.
   */
  public void setEncoding(String encoding)
    throws UnsupportedEncodingException
  {
  }

  /**
   * Set true for output stream only request.
   */
  public void setOutputStreamOnly(boolean isOutputStreamOnly)
  {
  }

  /**
   * Sets the locale.
   */
  public void setLocale(Locale locale)
    throws UnsupportedEncodingException
  {
  }

  /**
   * Sets the buffer size.
   */
  abstract public void setBufferSize(int size);

  /**
   * Gets the buffer size.
   */
  abstract public int getBufferSize();

  /**
   * Sets the auto-flush
   */
  public void setAutoFlush(boolean isAutoFlush)
  {
  }

  /**
   * Return the auto-flush.
   */
  public boolean isAutoFlush()
  {
    return true;
  }

  /**
   * Returns the remaining buffer entries.
   */
  abstract public int getRemaining();

  /**
   * Returns the char buffer.
   */
  abstract public char []getCharBuffer()
    throws IOException;

  /**
   * Returns the char buffer offset.
   */
  abstract public int getCharOffset()
    throws IOException;

  /**
   * Sets the char buffer offset.
   */
  abstract public void setCharOffset(int offset)
    throws IOException;

  /**
   * Returns the next char buffer.
   */
  abstract public char []nextCharBuffer(int offset)
    throws IOException;

  /**
   * Returns true if the response is committed.
   */
  public boolean isCommitted()
  {
    return false;
  }

  public boolean hasData()
  {
    return false;
  }

  /**
   * Set true for HEAD requests.
   */
  public void setHead()
  {
  }

  /**
   * Set true for HEAD requests.
   */
  public boolean isHead()
  {
    return false;
  }

  /**
   * Sets a byte cache stream.
   */
  public void setByteCacheStream(OutputStream cacheStream)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets a char cache stream.
   */
  public void setCharCacheStream(Writer cacheStream)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the written content length
   */
  public int getContentLength()
  {
    return 0;
  }
  
  /**
   * Writes a byte to the output.
   */
  abstract public void write(int v)
    throws IOException;

  /**
   * Writes a byte array to the output.
   */
  abstract public void write(byte []buffer, int offset, int length)
    throws IOException;

  /**
   * Writes a character to the output.
   */
  abstract public void print(int ch)
    throws IOException;

  /**
   * Writes a char array to the output.
   */
  abstract public void print(char []buffer, int offset, int length)
    throws IOException;

  /**
   * Clears the output buffer, including headers if possible.
   */
  public void clear()
    throws IOException
  {
    clearBuffer();
  }

  /**
   * Clears the output buffer.
   */
  abstract public void clearBuffer();

  /**
   * Flushes the output buffer.
   */
  abstract public void flushBuffer()
    throws IOException;

  /**
   * Flushes the output.
   */
  public void flushByte()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Flushes the output.
   */
  public void flushChar()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Sends a file.
   *
   * @param path the path to the file
   * @param length the length of the file (-1 if unknown)
   */
  public void sendFile(Path path, long length)
    throws IOException
  {
    path.writeToStream(this);
  }

  /**
   * Flushes the output.
   */
  public void flush()
    throws IOException
  {
    flushByte();
  }

  /**
   * Finishes the response stream
   */
  public void finish()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Closes the response stream
   */
  public void close()
    throws IOException
  {
    finish();
  }

  /**
   * Clears the close
   */
  public void clearClosed()
    throws IOException
  {
  }

  protected void killCaching()
  {
  }
}
