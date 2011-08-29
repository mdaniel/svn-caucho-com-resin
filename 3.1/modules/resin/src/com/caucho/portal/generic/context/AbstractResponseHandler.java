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


package com.caucho.portal.generic.context;

import com.caucho.portal.generic.FastPrintWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ResponseHandler that wraps another ResponseHandler.  Derived classes
 * override the print() and write() methods to intercept the output.
 *
 * flushBuffer(), reset(), and resetBuffer() DO NOT propagate to the
 * wrapped stream (they do nothing in the implementations for this class) 
 */
public class AbstractResponseHandler implements ResponseHandler
{
  protected static final Logger log = 
    Logger.getLogger(AbstractResponseHandler.class.getName());

  private PrintWriter _internalWriter;
  private OutputStream _internalOutputStream;

  private ResponseHandler _successor;

  protected PrintWriter _writer;
  protected OutputStream _outputStream;

  protected PrintWriter _writerOut;
  protected OutputStream _outputStreamOut;

  private Exception _errorCause;


  public AbstractResponseHandler()
  {
  }

  public AbstractResponseHandler(ResponseHandler successor)
  {
    open(successor);
  }

  public void open(ResponseHandler successor)
  {
    if (_successor != null)
      throw new IllegalStateException("already open");

    _successor = successor;
  }

  public void finish()
    throws IOException
  {
    Exception errorCause = _errorCause;

    _writerOut = null;
    _outputStreamOut = null;
    _successor = null;
    _errorCause = null;

    if (errorCause != null) {
      if (errorCause instanceof IOException)
        throw (IOException) errorCause;
      else {
        IOException ex = new IOException();
        ex.initCause(errorCause);
        throw ex;
      }
    }

  }

  public ResponseHandler getSuccessor()
  {
    return _successor;
  }

  public void setProperty(String name, String value)
  {
    _successor.setProperty(name, value);
  }

  public void addProperty(String name, String value)
  {
    _successor.addProperty(name, value);
  }

  public void setCharacterEncoding(String enc)
    throws UnsupportedEncodingException
  {
    if (_writer == null && _outputStream == null)
      _successor.setCharacterEncoding(enc);
  }

  public String getCharacterEncoding()
  {
    return _successor.getCharacterEncoding();
  }

  public void setContentType(String contentType)
  {
    if (_writer == null && _outputStream == null)
      _successor.setContentType(contentType);
  }

  public String getContentType()
  {
    return _successor.getContentType();
  }

  public void setLocale(Locale locale)
  {
    if (_writer == null && _outputStream == null)
      _successor.setLocale(locale);
  }

  public Locale getLocale()
  {
    return _successor.getLocale();
  }

  public boolean isCommitted()
  {
    return _successor.isCommitted();
  }

  public PrintWriter getWriter()
    throws IOException
  {
    checkErrorOrFail();

    if (_writer != null)
      return _writer;

    if (_outputStream != null)
      throw new IllegalStateException("getOutputStream() already called");

    if (getContentType() == null)
      throw new IllegalStateException(
          "response.setContentType() must be called before getWriter()");

    try {
      PrintWriter writerOut = _successor.getWriter();

      if (_internalWriter == null)
        _internalWriter = new GenericPrintWriter(this);

      _writerOut = writerOut;
      _writer = _internalWriter;

    }
    catch (Exception ex) {
      setError(ex);
    }

    checkErrorOrFail();


    return _writer;
  }

  protected PrintWriter getUnderlyingWriter()
  {
    return _writerOut;
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    checkErrorOrFail();

    if (_outputStream != null)
      return _outputStream;

    if (_writer != null)
      throw new IllegalStateException("getWriter() already called");

    if (getContentType() == null)
      throw new IllegalStateException(
          "response.setContentType() must be called before getOutputStream()");

    boolean fail = true;

    try {
      OutputStream outputStreamOut = _successor.getOutputStream();

      if (_internalOutputStream == null)
        _internalOutputStream = new GenericOutputStream(this);

      _outputStreamOut = outputStreamOut;
      _outputStream = _internalOutputStream;

      fail = false;
    }
    catch (Exception ex) {
      setError(ex);
    }

    checkErrorOrFail();

    return _outputStream;
  }

  protected OutputStream getUnderlyingOutputStream()
  {
    return _outputStreamOut;
  }

  /**
   * Set an error with a cause.
   */
  protected void setError(Exception cause)
  {
    if (cause == null)
      throw new NullPointerException();

    if (_errorCause != null) {
      _errorCause = cause;
      log.log(Level.FINEST, _errorCause.toString(), cause);
    }
  }

  /**
   * Return an exception if this ResponseHandler has failed.
   */ 
  public Exception getErrorCause()
  {
    return _errorCause;
  }

  public boolean isError()
  {
    return _errorCause != null;
  }

  protected void checkErrorOrFail()
    throws IOException
  {
    if (_errorCause != null) {
      if (_errorCause instanceof IOException)
        throw (IOException) _errorCause;
      else {
        IOException ex = new IOException();
        ex.initCause(_errorCause);
        throw ex;
      }
    }
  }

  public void setBufferSize(int bufferSize)
  {
    _successor.setBufferSize(bufferSize);
  }

  public int getBufferSize()
  {
    return _successor.getBufferSize();
  }

  /** 
   * flushBuffer(), reset(), and resetBuffer() DO NOT propagate to the
   * wrapped stream (they do nothing in the implementations for this class) 
   */ 
  public void reset()
  {
  }

  /** 
   * flushBuffer(), reset(), and resetBuffer() DO NOT propagate to the
   * wrapped stream (they do nothing in the implementations for this class) 
   */ 
  public void resetBuffer()
  {
  }

  /** 
   * flushBuffer(), reset(), and resetBuffer() DO NOT propagate to the
   * wrapped stream (they do nothing in the implementations for this class) 
   */ 
  public void flushBuffer()
    throws IOException
  {
    checkErrorOrFail();
  }

  /**
   * Write chars out to the underlying Writer
   */
  protected void print(char buf[], int off, int len)
    throws IOException
  {
    if (len == 0)
      return;

    checkErrorOrFail();

    _writerOut.write(buf, off, len);
  }

  /**
   * Write chars out to the underlying Writer
   */
  protected void print(String str, int off, int len)
    throws IOException
  {
    if (len == 0)
      return;

    checkErrorOrFail();

    _writerOut.write(str, off, len);
  }

  /**
   * Write a char out to the underlying Writer
   */
  protected void print(char c)
    throws IOException
  {
    checkErrorOrFail();

    _writerOut.write((int)c);
  }

  /**
   * Write bytes out to the underlying OutputStream
   */
  protected void write(byte[] buf, int off, int len) 
    throws IOException
  {
    checkErrorOrFail();

    _outputStreamOut.write(buf, off, len);
  }

  /**
   * Write a byte out to the underlying OutputStream
   */
  protected void write(byte b) 
    throws IOException
  {
    checkErrorOrFail();

    _outputStreamOut.write((int)b);
  }

  private static class GenericPrintWriter
      extends FastPrintWriter
  {
    private AbstractResponseHandler _successor;

    public GenericPrintWriter(AbstractResponseHandler successor)
    {
      _successor = successor;
    }

    protected void setError()
    {
      _successor.setError(new IOException());
    }

    protected void setError(Exception errorCause)
    {
      _successor.setError(errorCause);
    }

    public boolean checkError()
    {
      return _successor.getErrorCause() != null;
    }

    public Exception getErrorCause()
    {
      return _successor.getErrorCause();
    }


    public void writeOut(char buf[], int off, int len)
      throws IOException
    {
      _successor.print(buf, off, len);
    }

    public void writeOut(String str, int off, int len)
      throws IOException
    {
      _successor.print(str, off, len);
    }

    public void writeOut(char c)
      throws IOException
    {
      _successor.print((char)c);
    }

    public void close()
    {
    }
  }

  static private class GenericOutputStream extends OutputStream
  {
    private AbstractResponseHandler _successor;

    public GenericOutputStream(AbstractResponseHandler successor)
    {
      _successor = successor;
    }

    public void flush()
      throws IOException
    {
      _successor.flushBuffer();
    }

    public void write(byte[] buf)
      throws IOException
    {
      _successor.write(buf, 0, buf.length);
    }

    public void write(byte[] buf, int off, int len) 
      throws IOException
    {
      _successor.write(buf, off, len);
    }

    public void write(int b) 
      throws IOException
    {
      _successor.write((byte)b);
    }

    public void close()
    {
    }
  }
}
