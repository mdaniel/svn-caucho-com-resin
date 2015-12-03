/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.Env;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * php://memory
 */
public class ByteArrayBinaryStream extends AbstractBinaryInputOutput
{
  private byte[] _buffer;
  private int _pos;
  private int _length;

  public ByteArrayBinaryStream(Env env)
  {
    super(env);

    _buffer = new byte[4096];

    InputStream is = new ByteArrayBinaryInputStream();
    OutputStream os = new ByteArrayBinaryOutputStream();

    init(is, os);
  }

  @Override
  public long getPosition()
  {
    return _pos;
  }

  @Override
  public boolean setPosition(long pos)
  {
    if (_length < pos) {
      return false;
    }

    _pos = (int) pos;

    return true;
  }

  @Override
  public void unread()
    throws IOException
  {
    if (_pos == 0) {
      throw new IOException("no more bytes to unread");
    }

    _pos--;
  }

  @Override
  public boolean isEOF()
  {
    // php/167i

    return _length <= _pos;
  }

  class ByteArrayBinaryInputStream extends InputStream
  {
    public ByteArrayBinaryInputStream()
    {
    }

    @Override
    public int read(byte[] buffer, int offset, int length)
    {
      int len = Math.min(_length - _pos, length);

      System.arraycopy(_buffer, _pos, buffer, offset, len);

      _pos += len;

      return len;
    }

    @Override
    public int read()
    {
      if (_pos < _length) {
        return _buffer[_pos++] & 0xff;
      }
      else {
        return -1;
      }
    }
  }

  class ByteArrayBinaryOutputStream extends OutputStream
  {
    public ByteArrayBinaryOutputStream()
    {
    }

    @Override
    public void write(byte[] buffer, int offset, int length)
    {
      ensureCapacity(_pos + length);

      System.arraycopy(buffer, offset, _buffer, _pos, length);

      _pos += length;

      if (_length < _pos) {
        _length = _pos;
      }
    }

    @Override
    public void write(int ch)
    {
      ensureCapacity(_pos + 1);

      _buffer[_pos++] = (byte) ch;

      if (_length < _pos) {
        _length = _pos;
      }
    }
  }

  private void ensureCapacity(int size)
  {
    if (size <= _buffer.length) {
      return;
    }

    int newSize = Math.max(_buffer.length * 2, size);

    byte []buffer = new byte[newSize];

    System.arraycopy(_buffer, 0, buffer, 0, _length);
    _buffer = buffer;
  }
}
