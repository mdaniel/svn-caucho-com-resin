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

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Writer;
import java.io.OutputStream;
import java.io.IOException;

public class BufferFactoryImpl implements BufferFactory
{
  static protected final Logger log = 
    Logger.getLogger(BufferFactoryImpl.class.getName());

  private static int _bufferCount = 10;

  private int _bufferSize = 8192;
  private int _poolSize = 32;

  FreeList<CharBufferImpl> _charBufferFreeList
    = new FreeList<CharBufferImpl>(_poolSize);

  FreeList<ByteBufferImpl> _byteBufferFreeList
    = new FreeList<ByteBufferImpl>(_poolSize);


  public void setBufferSize(int bufferSize)
  {
    synchronized (_byteBufferFreeList) {
      synchronized (_charBufferFreeList) {
        _byteBufferFreeList.clear();
        _charBufferFreeList.clear();
        _bufferSize = bufferSize;
      }
    }
  }

  public void setPoolSize(int poolSize)
  {
    synchronized (_byteBufferFreeList) {
      synchronized (_charBufferFreeList) {
        _poolSize = poolSize;
        _byteBufferFreeList.ensureCapacity(poolSize);
        _charBufferFreeList.ensureCapacity(poolSize);
      }
    }
  }

  public int getDefaultBufferSize()
  {
    return _bufferSize;
  }

  public PortletCharBuffer allocateCharBuffer(int capacity)
  {
    CharBufferImpl b = null;

    if (capacity == Integer.MAX_VALUE)
      capacity = _bufferSize;
    else if (capacity < _bufferSize)
      capacity = _bufferSize;

    if (capacity == _bufferSize)
      b = _charBufferFreeList.allocate();

    if (b == null) {
      b = new CharBufferImpl(capacity);

      if (log.isLoggable(Level.FINEST))
        b.log("allocated new with capacity " + capacity);
    }
    else {
      if (log.isLoggable(Level.FINEST))
        b.log("allocated reused with capacity " + b.getCapacity());
    }

    return b;
  }

  public PortletByteBuffer allocateByteBuffer(int capacity)
  {
    ByteBufferImpl b = null;

    if (capacity == Integer.MAX_VALUE)
      capacity = _bufferSize;
    else if (capacity <= _bufferSize)
      capacity = _bufferSize;

    b = _byteBufferFreeList.allocate();

    if (b == null) {
      b = new ByteBufferImpl(capacity);

      if (log.isLoggable(Level.FINEST))
        b.log("allocated new with capacity " + capacity);
    }
    else {
      if (log.isLoggable(Level.FINEST))
        b.log("allocated reused with capacity " + b.getCapacity());
    }

    return b;
  }
  
  private class CharBufferImpl implements PortletCharBuffer
  {
    private String _bufferId;

    private int _bufferSize;
    private char[] _buf;
    private int _bufferPos = 0;

    public CharBufferImpl(int size)
    {
      int id = _bufferCount++;
      _bufferId = Integer.toString(id, Character.MAX_RADIX);

      _bufferSize = size;
      _buf = new char[size];
    }

    void log(String message)
    {
      if (log.isLoggable(Level.FINEST)) {
        message = new StringBuffer(256).append("char buffer ")
                                       .append(_bufferId)
                                       .append(' ')
                                       .append(message)
                                       .toString();

        log.log(Level.FINEST, message);
      }
    }

    public boolean print(char buf[], int off, int len)
    {
      if (len > _bufferSize - _bufferPos)
        return overrun();
      else {
        System.arraycopy(buf, off, _buf, _bufferPos, len);
        _bufferPos += len;

        return true;
      }
    }

    public boolean print(String str, int off, int len)
    {
      if (len > _bufferSize - _bufferPos)
        return overrun();
      else {
        str.getChars(off, off + len,  _buf, _bufferPos);
        _bufferPos += len;

        return true;
      }
    }

    public boolean print(int c)
    {
      if (_bufferPos == _bufferSize)
        return overrun();
      else {
        _buf[_bufferPos++] = (char) c;

        return true;
      }
    }

    private boolean overrun()
    {
      log("overrun");
      return false;
    }

    public void flush(Writer out)
      throws IOException
    {
      if (_bufferPos != 0)  {
        log("flush");

        out.write(_buf, 0, _bufferPos);
        _bufferPos = 0;
      }
    }

    public int size()
    {
      return _bufferPos;
    }

    public int getCapacity()
    {
      return _bufferSize;
    }

    public void reset()
    {
      log("reset");

      _bufferPos = 0;
    }

    public void finish()
    {
      _bufferPos = 0;

      log("finish");

      if (_bufferSize <= BufferFactoryImpl.this._bufferSize) {
        if (!_charBufferFreeList.free(this))
          _buf = null;
      }
      else
        _buf = null;
    }
  }

  private class ByteBufferImpl implements PortletByteBuffer
  {
    private String _bufferId;

    private int _bufferSize;
    private byte[] _buf;
    private int _bufferPos = 0;

    public ByteBufferImpl(int size)
    {
      int id = _bufferCount++;
      _bufferId = Integer.toString(id, Character.MAX_RADIX);

      _bufferSize = size;
      _buf = new byte[size];
    }

    void log(String message)
    {
      if (log.isLoggable(Level.FINEST)) {
        message = new StringBuffer(256).append("byte buffer ")
                                       .append(_bufferId)
                                       .append(' ')
                                       .append(message)
                                       .toString();

        log.log(Level.FINEST, message);
      }
    }

    public boolean write(byte[] buf, int off, int len) 
    {
      if (len > _bufferSize - _bufferPos)
        return overrun();
      else {
        System.arraycopy(buf, off, _buf, _bufferPos, len);
        _bufferPos += len;

        return true;
      }
    }

    /**
     * @return false if the buffer is full and the byte could not be written
     */
    public boolean write(int b) 
    {
      if (_bufferPos == _bufferSize)
        return overrun();
      else {
        _buf[_bufferPos++] = (byte) b;

        return true;
      }
    }

    public int size()
    {
      return _bufferPos;
    }

    public int getCapacity()
    {
      return _bufferSize;
    }

    private boolean overrun()
    {
      log("overrun");
      return false;
    }

    public void flush(OutputStream out)
      throws IOException
    {
      if (_bufferPos != 0)  {
        log("flush");
        out.write(_buf, 0, _bufferPos);
        _bufferPos = 0;
      }
    }

    public void reset()
    {
      log("reset");
      _bufferPos = 0;
    }

    public void finish()
    {
      _bufferPos = 0;
      log("finish");

      if (_bufferSize <= BufferFactoryImpl.this._bufferSize) {
        if (!_byteBufferFreeList.free(this))
          _buf = null;
      }
      else
        _buf = null;
    }
  }

  private class FreeList<E>
  {
    private int _freeListSize;
    private ArrayList<E> _freeList;

    private long _lastAllocateFail;

    public FreeList(int initialCapacity)
    {
      _freeListSize = initialCapacity;
      _freeList = new ArrayList<E>(_freeListSize);
    }

    public E allocate()
    {
      synchronized (_freeList) {
        int size = _freeList.size();

        if (size > 0)
          return _freeList.remove(--size);
      }


      return null;
    }

    public boolean free(E obj)
    {
      synchronized (_freeList) {
        int size = _freeList.size();

        if (size < _freeListSize) {
          _freeList.add(obj);
          return true;
        }
        else {
          if (log.isLoggable(Level.CONFIG))
            log.config("buffer pool overrun, consider increasing buffer-factory pool-size");
          return false;
        }
      }
    }

    public void clear()
    {
      synchronized (_freeList) {
        _freeList.clear();
      }
    }

    public void ensureCapacity(int capacity)
    {
      synchronized (_freeList) {
        _freeList.ensureCapacity(capacity);
      }
    }
  }
}
