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

package com.caucho.util;

/**
 * bit/bytes utilities.
 */
public class BitsUtil {
  public static void writeLong(byte []buffer, int offset, long value)
  {
    buffer[0 + offset] = (byte) (value >> 56);
    buffer[1 + offset] = (byte) (value >> 48);
    buffer[2 + offset] = (byte) (value >> 40);
    buffer[3 + offset] = (byte) (value >> 32);
    buffer[4 + offset] = (byte) (value >> 24);
    buffer[5 + offset] = (byte) (value >> 16);
    buffer[6 + offset] = (byte) (value >> 8);
    buffer[7 + offset] = (byte) (value >> 0);
  }
  
  public static long readLong(byte []buffer, int offset)
  {
    return (((buffer[0 + offset] & 0xffL) << 56)
           + ((buffer[1 + offset] & 0xffL) << 48)
           + ((buffer[2 + offset] & 0xffL) << 40)
           + ((buffer[3 + offset] & 0xffL) << 32)
           + ((buffer[4 + offset] & 0xffL) << 24)
           + ((buffer[5 + offset] & 0xffL) << 16)
           + ((buffer[6 + offset] & 0xffL) << 8)
           + ((buffer[7 + offset] & 0xffL) << 0));
  }
}
