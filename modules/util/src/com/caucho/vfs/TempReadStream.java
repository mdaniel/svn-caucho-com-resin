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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.IOException;

public class TempReadStream extends StreamImpl {
  private TempBuffer _cursor;

  private int _offset;
  private boolean _freeWhenDone = true;

  public TempReadStream(TempBuffer cursor)
  {
    init(cursor);
  }

  public TempReadStream()
  {
  }

  public void init(TempBuffer cursor)
  {
    _cursor = cursor;
    _offset = 0;
    _freeWhenDone = true;
  }

  public void setFreeWhenDone(boolean free)
  {
    _freeWhenDone = free;
  }

  public boolean canRead() { return true; }

  // XXX: any way to make this automatically free?
  public int read(byte []buf, int offset, int length) throws IOException
  {
    if (_cursor == null)
      return -1;

    if (_cursor._length - _offset < length)
      length = _cursor._length - _offset;

    System.arraycopy(_cursor._buf, _offset, buf, offset, length);

    if (_cursor._length <= _offset + length) {
      TempBuffer next = _cursor._next;
      if (_freeWhenDone)
        TempBuffer.free(_cursor);
      _cursor = next;
      _offset = 0;
    }
    else
      _offset += length;

    return length > 0 ? length : -1;
  }

  public int getAvailable() throws IOException
  {
    if (_cursor != null)
      return _cursor._length - _offset;
    else
      return 0;
  }

  public void close()
    throws IOException
  {
    if (_freeWhenDone && _cursor != null)
      TempBuffer.freeAll(_cursor);
    
    _cursor = null;
  }

  public String toString()
  {
    return "TempReadStream[]";
  }
}
