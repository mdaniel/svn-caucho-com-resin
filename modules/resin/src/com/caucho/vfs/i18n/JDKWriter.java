/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.vfs.i18n;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.nio.charset.Charset;

import com.caucho.vfs.OutputStreamWithBuffer;

/**
 * Factory for JDK-based encoding writers.
 */
public class JDKWriter extends EncodingWriter {
  private String _javaEncoding;
  
  /**
   * Null-arg constructor for instantiation by com.caucho.vfs.Encoding only.
   */
  public JDKWriter()
  {
  }
  /**
   * Returns the Java encoding for the writer.
   */
  public String getJavaEncoding()
  {
    return _javaEncoding;
  }
  
  /**
   * Sets the Java encoding for the writer.
   */
  public void setJavaEncoding(String encoding)
  {
    _javaEncoding = encoding;
  }
  

  /**
   * Create a JDK-based reader.
   *
   * @param javaEncoding the JDK name for the encoding.
   *
   * @return an EncodingWriter
   */
  public EncodingWriter create(String javaEncoding)
  {
    try {
      return new OutputStreamEncodingWriter(javaEncoding);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    }
  }

  /**
   * JDKWriter is only a factory.
   */
  public void write(OutputStreamWithBuffer os, char ch)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  static class OutputStreamEncodingWriter extends EncodingWriter {
    private Charset _charset;
    private OutputStreamWriter _writer;
    private OutputStreamWithBuffer _os;

    OutputStreamEncodingWriter(String javaEncoding)
      throws UnsupportedEncodingException
    {
      try {
	_charset = Charset.forName(javaEncoding);
      } catch (java.nio.charset.UnsupportedCharsetException e) {
	throw new UnsupportedEncodingException(e.getMessage());
      }
    }

    /**
     * Writes a char.
     */
    public void write(OutputStreamWithBuffer os, char ch)
      throws IOException
    {
      if (_os != os) {
	_writer = new OutputStreamWriter(os, _charset);
	_os = os;
      }
      
      _writer.write(ch);
      _writer.flush();
    }

    /**
     * Writes a char buffer.
     */
    public void write(OutputStreamWithBuffer os,
		      char []buf, int offset, int length)
      throws IOException
    {
      if (_os != os) {
	_writer = new OutputStreamWriter(os, _charset);
	_os = os;
      }
      
      _writer.write(buf, offset, length);
      _writer.flush();
    }

    /**
     * Creates the child.
     */
    public EncodingWriter create(String encoding)
    {
      throw new UnsupportedOperationException();
    }
  }
}
