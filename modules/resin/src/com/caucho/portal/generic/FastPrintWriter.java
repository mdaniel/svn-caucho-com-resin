/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic;

import java.io.*;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An unsynchronized PrintWriter.  Derived classes override three writeOut()
 * methods to intercept the content being written.
 */
public class FastPrintWriter
  extends PrintWriter
{
  static final public Logger log = 
    Logger.getLogger(FastPrintWriter.class.getName());

  final static Writer _dummy = new PipedWriter();
  private final static char []_newline = "\n".toCharArray();

  protected Writer _out;
  private boolean _error;
  private Exception _errorCause;

  public FastPrintWriter()
  {
    super(_dummy);
  }

  public FastPrintWriter(Writer out)
    throws IOException
  {
    super(_dummy);
    open(out);
  }


  public void open(Writer out)
    throws IOException
  {
    if (_out != null)
      throw new IOException("already open");

    if (out == null)
      throw new NullPointerException();

    _out = out;

  }

  protected void setError()
  {
    if (_error == false) {
      _error = true;
      _errorCause = new IOException("stream failed (no exception)");
    }
  }

  protected void setError(Exception errorCause)
  {
    if (_error == false) {
      _error = true;
      _errorCause = errorCause;
    }
  }

  public boolean checkError()
  {
    return _error;
  }

  public Exception getErrorCause()
  {
    return _errorCause;
  }

  public void clearError()
  {
    _error = false;
    _errorCause = null;
  }

  public void flush()
  {
    if (checkError())
      return;

    try {
      _out.flush();
    }
    catch (Exception ex) {
      setError(ex);
    }
  }

  public void writeOut(char buf[], int off, int len)
    throws IOException
  {
    _out.write(buf, off, len);
  }

  public void writeOut(String str, int off, int len)
    throws IOException
  {
    _out.write(str, off, len);
  }

  public void writeOut(char c)
    throws IOException
  {
    _out.write((int) c);
  }

  public void close()
  {
    Writer out = _out;
    boolean error = _error;

    flush();

    _out = null;
    _error = false;
    _errorCause = null;

    if (!_error) {
      try {
        out.close();
      }
      catch (IOException ex) {
        setError(ex);
      }
    }
  }

  public void write(char buf[], int off, int len)
  {
    if (checkError())
      return;

    try {
      writeOut(buf, off, len);
    }
    catch (Exception ex) {
      setError(ex);
    }
  }

  public void write(String str, int off, int len)
  {
    if (checkError())
      return;

    try {
      writeOut(str, off, len);
    }
    catch (Exception ex) {
      setError(ex);
    }
  }

  public void write(int c)
  {
    if (checkError())
      return;

    try {
      writeOut((char) c);
    }
    catch (Exception ex) {
      setError(ex);
    }
  }

  public void write(char cbuf[])
  {
    write(cbuf, 0, cbuf.length);
  }

  public void write(String str)
  {
    write(str, 0, str.length());
  }

  public void print(boolean b) 
  {
    write(b ? "true" : "false");
  }

  public void print(char c) 
  {
    write(c);
  }

  public void print(int i) 
  {
    write(String.valueOf(i));
  }

  public void print(long l) 
  {
    write(String.valueOf(l));
  }

  public void print(float f) 
  {
    write(String.valueOf(f));
  }

  public void print(double d) 
  {
    write(String.valueOf(d));
  }

  public void print(char s[]) 
  {
    write(s);
  }

  public void print(String s) 
  {
    write(s == null ? "null" : s);
  }

  public void print(Object obj) 
  {
    write(String.valueOf(obj));
  }

  public void println()
  {
    write(_newline, 0, _newline.length);
  }


  public void println(boolean b) 
  {
    print(b);
    println();
  }

  public void println(char c) 
  {
    print(c);
    println();
  }

  public void println(int i) 
  {
    print(i);
    println();
  }

  public void println(long l) 
  {
    print(l);
    println();
  }

  public void println(float f) 
  {
    print(f);
    println();
  }

  public void println(double d) 
  {
    print(d);
    println();
  }

  public void println(char c[]) 
  {
    print(c);
    println();
  }

  public void println(String s) 
  {
    print(s);
    println();
  }

  public void println(Object o) 
  {
    print(o);
    println();
  }
}
