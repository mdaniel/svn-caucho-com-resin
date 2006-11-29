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

import com.caucho.portal.generic.BufferFactory;
import com.caucho.portal.generic.PortletByteBuffer;
import com.caucho.portal.generic.PortletCharBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class BufferedResponseHandler extends AbstractResponseHandler
{
  protected static final Logger log = 
    Logger.getLogger(BufferedResponseHandler.class.getName());

  private static final int NOT_ALLOCATED = Integer.MIN_VALUE;

  private BufferFactory _bufferFactory;

  // size of buffer to allocate when Writer or OutputStream is obtained
  private int _initialBufferSize = -1;


  private LinkedHashMap<String, Object> _propertiesMap;

  private int _bufferSize =  NOT_ALLOCATED; // size allocated
  private PortletCharBuffer _charBuffer;
  private PortletByteBuffer _byteBuffer;
  private PrintWriter _writer;
  private OutputStream _outputStream;
  private boolean _isCommitted;

  public BufferedResponseHandler( ResponseHandler responseHandler, 
                                  BufferFactory bufferFactory,
                                  int initialBufferSize)
  {
    open(responseHandler, bufferFactory, initialBufferSize );
  }

  public void open( ResponseHandler responseHandler, 
                    BufferFactory bufferFactory,
                    int initialBufferSize )
  {
    super.open(responseHandler);
    _bufferFactory = bufferFactory;
    _initialBufferSize = initialBufferSize;
  }

  public void finish()
    throws IOException
  {
    if (!isError())
      flushBuffer();

    freeBuffers(true);

    if (_propertiesMap != null)
      _propertiesMap.clear();

    _isCommitted = false;
    _writer = null;
    _outputStream = null;
    _bufferSize = NOT_ALLOCATED;
    _initialBufferSize = -1;
    _bufferFactory = null;

    super.finish();
  }

  public void setBufferSize(int bufferSize)
  {
    if (_bufferSize != NOT_ALLOCATED) {
      if (bufferSize > _bufferSize) {
        if (!_isCommitted) {
          freeBuffers(true);
          _initialBufferSize = bufferSize;
        }
        else {
          if (_bufferSize == 0 && _initialBufferSize != 0)
            throw new IllegalStateException(
                "buffer already committed to `" + _bufferSize + "'");
          else
            throw new IllegalStateException("buffer already committed");
        }
      }
    }
    else
      _initialBufferSize = bufferSize;
  }

  public int getBufferSize()
  {
    if (_bufferSize != NOT_ALLOCATED)
      return _bufferSize;
    else if (_initialBufferSize != -1)
      return _initialBufferSize;
    else
      return _bufferFactory.getDefaultBufferSize();
  }

  public void setProperty(String name, String value)
  {
    if (_isCommitted)
      throw new IllegalStateException("response committed");

    if (_propertiesMap == null)
      _propertiesMap = new LinkedHashMap<String, Object>();

    _propertiesMap.put(name, value);
  }

  public void addProperty(String name, String value)
  {
    if (_isCommitted)
      throw new IllegalStateException("response committed");

    Object existingValue = null;

    if (_propertiesMap == null)
      _propertiesMap = new LinkedHashMap<String, Object>();
    else
      existingValue = _propertiesMap.get(name);

    if (existingValue == null) {
      _propertiesMap.put(name, value);
    }
    else if (existingValue instanceof ArrayList) {
      ((ArrayList<String>) existingValue).add(value);
    }
    else {
      ArrayList<String> valueList = new ArrayList<String>();
      valueList.add((String) existingValue);
      valueList.add(value);
    }
  }

  public boolean isCommitted()
  {
    return _isCommitted;
  }

  private void allocateCharBufferIfNeeded()
  {
    if (_byteBuffer != null)
      throw new IllegalStateException(
          "cannot allocate char buffer, byte buffer already allocated");

    if (_bufferSize == NOT_ALLOCATED) {
      if (_initialBufferSize != 0) {
        _charBuffer = _bufferFactory.allocateCharBuffer(_initialBufferSize);
        _bufferSize = _charBuffer.getCapacity();
      }
      else
        _bufferSize = 0;
    }
  }

  private void allocateByteBufferIfNeeded()
  {
    if (_charBuffer != null)
      throw new IllegalStateException(
          "cannot allocate byte buffer, char buffer already allocated");

    if (_bufferSize == NOT_ALLOCATED) {
      if (_initialBufferSize != 0) {
        _byteBuffer = _bufferFactory.allocateByteBuffer(_initialBufferSize);
        _bufferSize = _byteBuffer.getCapacity();
      }
      else
        _bufferSize = 0;
    }
  }

  public PrintWriter getWriter()
    throws IOException
  {
    checkErrorOrFail();

    if (_writer != null)
      return _writer;

    return _writer = super.getWriter();
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    checkErrorOrFail();

    if (_outputStream != null)
      return _outputStream;

    return _outputStream = super.getOutputStream();
  }

  public void reset()
  {
    resetBuffer();

    if (_propertiesMap != null)
      _propertiesMap.clear();
  }

  public void resetBuffer()
  {
    if (_isCommitted) 
      throw new IllegalStateException("response is already committed");

    if (_byteBuffer != null)
      _byteBuffer.reset();

    if (_charBuffer != null)
      _charBuffer.reset();

    freeBuffers(true);
  }

  /**
   * @param isAllocateAgain allow them to be allocated again on next write
   */
  private void freeBuffers(boolean isAllocateAgain)
  {
    PortletCharBuffer charBuffer = _charBuffer;
    PortletByteBuffer byteBuffer = _byteBuffer;

    _charBuffer = null;
    _byteBuffer = null;

    if (charBuffer != null) {
      if (charBuffer.size() != 0)
        throw new IllegalStateException("still content in buffer");

      charBuffer.finish();
    }

    if (byteBuffer != null) {
      if (byteBuffer.size() != 0)
        throw new IllegalStateException("still content in buffer");
      
      byteBuffer.finish();
    }

    if (isAllocateAgain)
      _bufferSize = NOT_ALLOCATED;
  }

  public void flushBuffer()
    throws IOException
  {
    checkErrorOrFail();

    try {
      flushProperties();
      flushBufferOnly();
    }
    catch (Exception ex) {
      setError(ex);
    }
  }

  private void flushProperties()
  {
    if (_propertiesMap != null && _propertiesMap.size() > 0) {

      _isCommitted = true;

      Iterator<Map.Entry<String, Object>> iter 
        = _propertiesMap.entrySet().iterator();

      do {
        Map.Entry<String, Object> entry = iter.next();
        String name = entry.getKey();
        Object value = entry.getValue();

        if (value instanceof ArrayList) {
          ArrayList<String> valueList = (ArrayList<String>) value;

          for (int i = 0; i < valueList.size(); i++) {
            super.addProperty(name, valueList.get(i));
          }
        }
        else {
          super.setProperty(name, (String) value);
        }

        iter.remove();
      } while (iter.hasNext());
    }
  }

  private void flushBufferOnly()
    throws IOException
  {
    if (_charBuffer != null && _charBuffer.size() > 0) {
      _isCommitted = true;
      _charBuffer.flush(super.getUnderlyingWriter());
    }

    if (_byteBuffer != null && _byteBuffer.size() > 0) {
      _isCommitted = true;
      _byteBuffer.flush(super.getUnderlyingOutputStream());
    }

    // after they are flushed once, don't need them anymore

    freeBuffers(false);
  }

  protected void print(char buf[], int off, int len)
    throws IOException
  {
    if (len == 0)
      return;

    allocateCharBufferIfNeeded();

    checkErrorOrFail();

    if (_charBuffer == null) {
      _isCommitted = true;
      super.print(buf, off, len);
    } 
    else {
      if (!_charBuffer.print(buf, off, len))  {
        flushBuffer();
        _isCommitted = true;
        super.print(buf, off, len);
      }
    }
  }

  protected void print(String str, int off, int len)
    throws IOException
  {
    if (len == 0)
      return;

    allocateCharBufferIfNeeded();

    checkErrorOrFail();

    if (_charBuffer == null) {
      _isCommitted = true;
      super.print(str, off, len);
    }
    else {
      if (!_charBuffer.print(str, off, len))  {
        flushBuffer();
        _isCommitted = true;
        super.print(str, off, len);
      }
    }
  }

  protected void print(char c)
    throws IOException
  {
    allocateCharBufferIfNeeded();

    checkErrorOrFail();

    if (_charBuffer == null) {
      _isCommitted = true;
      super.print(c);
    }
    else {
      if (!_charBuffer.print(c))  {
        flushBuffer();
        _isCommitted = true;
        super.print(c);
      }
    }
  }

  protected void write(byte[] buf, int off, int len) 
    throws IOException
  {
    if (len == 0)
      return;

    allocateByteBufferIfNeeded();

    checkErrorOrFail();

    if (_byteBuffer == null) {
      _isCommitted = true;
      super.write(buf, off, len);
    }
    else {
      if (!_byteBuffer.write(buf, off, len))  {
        flushBuffer();
        _isCommitted = true;
        super.write(buf, off, len);
      }
    }
  }

  protected void write(byte b) 
    throws IOException
  {
    checkErrorOrFail();

    allocateByteBufferIfNeeded();

    if (_byteBuffer == null) {
      _isCommitted = true;
      super.write(b);
    }
    else {
      if (!_byteBuffer.write(b))  {
        flushBuffer();
        _isCommitted = true;
        super.write(b);
      }
    }
  }
}
