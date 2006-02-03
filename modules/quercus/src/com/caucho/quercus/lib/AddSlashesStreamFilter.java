/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */


package com.caucho.quercus.lib;

import com.caucho.vfs.StreamFilter;

import java.io.IOException;

/**
 * Add slashes on read and on write.
 */
public class AddSlashesStreamFilter
  extends StreamFilter
{
  private static byte[] ZERO_BYTES = new byte[] { '\\', '0' };
  private static byte[] SINGLEQUOTE_BYTES = new byte[] { '\\', '\'' };
  private static byte[] DOUBLEQUOTE_BYTES = new byte[] { '\\', '"' };
  private static byte[] BACKSLASH_BYTES = new byte[] { '\\', '\\' };

  private int nextReadByte = -1;


  public void write(byte []buffer, int offset, int length, boolean atEnd)
    throws IOException
  {
    final int end = offset + length;
    final int last = end - 1;

    for (int i = offset; i < end; i++) {

      byte b = buffer[i];

      switch (b) {
        case 0x0:
          super.write(ZERO_BYTES, 0, ZERO_BYTES.length, atEnd && i == last);
          break;
        case '\'':
          super.write(SINGLEQUOTE_BYTES, 0, SINGLEQUOTE_BYTES.length, atEnd && i == last);
          break;
        case '\"':
          super.write(DOUBLEQUOTE_BYTES, 0, DOUBLEQUOTE_BYTES.length, atEnd && i == last);
          break;
        case '\\':
          super.write(BACKSLASH_BYTES, 0, BACKSLASH_BYTES.length, atEnd && i == last);
          break;
        default:
          super.write(buffer, i, 1, atEnd && i == last);
          break;
      }
    }
  }

  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    byte[] buf = new byte[1];

    if (length <= 0)
      return 0;

    final int end = offset + length;

    int i;

    for (i = offset; i < end; i++) {
      if (nextReadByte  > -1) {
        buffer[i] = (byte) nextReadByte;
        nextReadByte = -1;
        continue;
      }

      int read = super.read(buf, 0, 1);

      if (read < 1)
        break;

      byte b = buf[0];

      switch (b) {
        case 0x0:
          buffer[i] = '\\';
          nextReadByte = '0';
          break;
        case '\'':
        case '\"':
        case '\\':
          buffer[i] = '\\';
          nextReadByte = b;
          break;
        default:
          buffer[i] = b;
          break;
      }
    }

    return i - offset;
  }
}
