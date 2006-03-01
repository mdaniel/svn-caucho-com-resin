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

import java.io.*;
import java.net.*;
import java.util.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

public class StringStream extends StreamImpl {
  private String _string;
  private int _length;
  private int _index;

  StringStream(String string)
  {
    // this.path = new NullPath("string");
    _string = string;
    _length = string.length();
    _index = 0;
  }

  public static ReadStream open(String string)
  {
    StringStream ss = new StringStream(string);
    return new ReadStream(ss);
  }

  public Path getPath() { return new StringPath(_string); }

  public boolean canRead() { return true; }

  // XXX: encoding issues
  public int read(byte []buf, int offset, int length) throws IOException
  {
    if (_length - _index < length)
      length = _length - _index;

    for (int i = 0; i < length; i++) {
      int ch = _string.charAt(_index + i); 

      if (ch < 0x80)
	buf[offset++] = (byte) ch;
      else if (ch < 0x800) {
	buf[offset++] = (byte) (0xc0 | (ch >> 6));
	buf[offset++] = (byte) (0x80 | (ch & 0x3f));
      }
      else if (ch < 0x8000) {
	buf[offset++] = (byte) (0xe0 | (ch >> 12));
	buf[offset++] = (byte) (0x80 | ((ch >> 6) & 0x3f));
	buf[offset++] = (byte) (0x80 | ((ch >> 6) & 0x3f));
      }
    }

    _index += length;

    return length > 0 ? length : -1;
  }

  public int getAvailable() throws IOException
  {
    return _length - _index;
  }
}
