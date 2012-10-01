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
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.google.appengine.api.files.FileReadChannel;

/**
 * Reading from a google stream.
 */
class GoogleReadStream extends StreamImpl {
  private static final Logger log
    = Logger.getLogger(GoogleReadStream.class.getName());

  private static final L10N L = new L10N(GoogleReadStream.class);

  private final GooglePath _path;
  private FileReadChannel _is;
  private final ByteBuffer _buf = ByteBuffer.allocate(1024 * 8);

  GoogleReadStream(GooglePath path, FileReadChannel is)
    throws IOException
  {
    _path = path;
    _is = is;

    _buf.clear();

    int sublen = _is.read(_buf);

    _buf.flip();
  }

  @Override
  public boolean canRead()
  {
    return true;
  }

  /**
   * Returns the number of bytes available without blocking.  Depending on
   * the stream, this may return less than the actual bytes, but will always
   * return a number > 0 if there is any data available.
   */
  @Override
  public int getAvailable() throws IOException
  {
    return 1;
  }

  @Override
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    if (! _buf.hasRemaining()) {
      _buf.clear();

      int result = _is.read(_buf);

      _buf.flip();

      if (result <= 0) {
        return result;
      }
    }

    int sublen = Math.min(length, _buf.remaining());

    _buf.get(buffer, offset, sublen);

    return sublen;
  }

  /**
   * Seeks based on the start.
   */
  @Override
  public void seekStart(long offset)
    throws IOException
  {
    _is.position(offset);

    _buf.clear();
    _buf.flip();
  }

  @Override
  public long getReadPosition()
  {
    try {
      return _is.position();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns true if the stream implements skip.
   */
  @Override
  public boolean hasSkip()
  {
    return true;
  }

  /**
   * Skips a number of bytes, returning the bytes skipped.
   *
   * @param n the number of types to skip.
   *
   * @return the actual bytes skipped.
   */
  @Override
  public long skip(long n)
    throws IOException
  {
    long remaining = _buf.remaining();

    if (n <= remaining) {
      _buf.position(_buf.position() + (int) n);

      return n;
    }

    _buf.clear();
    _buf.flip();

    long toSkip = n - remaining;

    long pos = _is.position();
    _is.position(pos + toSkip);

    return _is.position() - pos + remaining;
  }
  
  @Override
  public void close()
    throws IOException
  {
    FileReadChannel is = _is;
    _is = null;

    if (is != null) {
      is.close();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path.getNativePath() + "," + _is + "]";
  }
}
