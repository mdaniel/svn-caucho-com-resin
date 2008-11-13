/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.http;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImpl;

import java.io.IOException;

/**
 * StreamImpl so servlets can read POST data as a normal stream.
 */
class ChunkedInputStream extends StreamImpl  {
  private ReadStream _next;
  private int _available;

  void init(ReadStream next)
  {
    _next = next;
    _available = 0;
  }

  public boolean canRead()
  {
    return true;
  }

  public int getAvailable()
  {
    return _available;
  }

  /**
   * Reads more data from the input stream.
   */
  public int read(byte []buf, int offset, int len) throws IOException
  {
    // The chunk still has more data left
    if (_available > 0) {
      if (_available < len)
	len = _available;

      len = _next.read(buf, offset, len);

      if (len > 0)
        _available -= len;
    }
    // The chunk is done, so read the next chunk
    else if (_available == 0) {
      _available = readChunkLength();

      // the new chunk has data
      if (_available > 0) {
	if (_available < len)
	  len = _available;

	len = _next.read(buf, offset, len);

	if (len > 0)
	  _available -= len;
      }
      // the new chunk is the last
      else {
	_available = -1;
	len = -1;
      }
    }
    else
      len = -1;

    return len;
  }

  /**
   * Reads the next chunk length from the input stream.
   */
  private int readChunkLength()
    throws IOException
  {
    int length = 0;
    int ch;

    // skip whitespace
    for (ch = _next.read();
	 ch == '\r' || ch == ' ' || ch == '\n';
	 ch = _next.read()) {
    }

    // XXX: This doesn't properly handle the case when when the browser
    // sends headers at the end of the data.  See the HTTP/1.1 spec.
    for (; ch > 0 && ch != '\r' && ch != '\n'; ch = _next.read()) {
      if (ch >= '0' && ch <= '9')
	length = 16 * length + ch - '0';
      else if (ch >= 'a' && ch <= 'f')
	length = 16 * length + ch - 'a' + 10;
      else if (ch >= 'A' && ch <= 'F')
	length = 16 * length + ch - 'A' + 10;
      else if (ch == ' ' || ch == '\t') {
	//if (dbg.canWrite())
	//  dbg.println("unexpected chunk whitespace.");
      }
      else {
	throw new IOException("HTTP/1.1 protocol error: bad chunk at "
			      + "'" + (char) ch
			      + "' 0x" + Integer.toHexString(ch)
			      + " length=" + length);
      }
    }

    if (ch == '\r')
      ch = _next.read();

    return length;
  }
}
