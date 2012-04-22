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

import com.caucho.util.L10N;
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileWriteChannel;


/**
 * Writing to a google stream.
 */
class GoogleStoreReadStream extends StreamImpl {
  private static final L10N L = new L10N(GoogleStoreReadStream.class);
  
  private final GoogleStorePath _path;
  private FileReadChannel _is;
  private final ByteBuffer _buf = ByteBuffer.allocate(1024);
  
  GoogleStoreReadStream(GoogleStorePath path, FileReadChannel is)
  {
    _path = path;
    _is = is;
  }
  
  @Override
  public boolean canRead()
  {
    return true;
  }
  
  @Override
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    _buf.clear();
    
    int sublen = Math.min(_buf.capacity(), length);
    
    sublen = _is.read(_buf);
    
    if (sublen <= 0)
      return sublen;
    
    _buf.flip();
    
    _buf.get(buffer, offset, sublen);
    
    return sublen;
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
