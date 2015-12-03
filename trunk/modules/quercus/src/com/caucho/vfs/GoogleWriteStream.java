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

import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.vfs.GoogleInode.FileType;
import com.google.appengine.api.files.FileWriteChannel;

/**
 * Writing to a google stream.
 */
class GoogleWriteStream extends StreamImpl {
  private static final L10N L = new L10N(GoogleWriteStream.class);

  private final GooglePath _path;
  private FileWriteChannel _os;
  private final ByteBuffer _buf = ByteBuffer.allocate(1024 * 8);

  // private final ByteArrayOutputStream _bout = new ByteArrayOutputStream();

  private GoogleInode _inode;

  private long _length;

  GoogleWriteStream(GooglePath path, FileWriteChannel os, GoogleInode inode)
  {
    _path = path;
    _os = os;
    _inode = inode;
  }

  @Override
  public boolean canWrite()
  {
    return true;
  }

  @Override
  public void write(byte []buffer, int offset, int length, boolean isEnd)
    throws IOException
  {
    // String s = new String(buffer, offset, length);

    /*
    System.out.println("XX-WRITE:[[" + s + "]]");
    if (s.indexOf("images/multi-wide.gif") >= 0)
      Thread.dumpStack();
      */
    while (length > 0) {
      int sublen = Math.min(_buf.capacity(), length);

      _buf.clear();

      _buf.put(buffer, offset, sublen);
      _buf.flip();

      sublen = _os.write(_buf);

      if (sublen <= 0)
        throw new IOException(L.l("{0}: Unable to write", this));

      length -= sublen;
      offset += sublen;

      _length += sublen;
    }
  }

  public void close()
    throws IOException
  {
    FileWriteChannel os = _os;
    _os = null;

    if (os != null) {
      os.closeFinally();

      GoogleInode inode = _inode;

      if (inode == null || ! inode.isDirectory()) {
        long time = CurrentTime.getCurrentTime();

        if (inode != null) {
          inode.setLastModified(time);
        }

        inode = new GoogleInode(_path.getTail(), FileType.FILE, _length, time);

        _inode = inode;
      }

      _path.writeInode(inode);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path.getNativePath() + "," + _os + "]";
  }
}
