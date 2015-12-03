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

import java.util.logging.*;
import java.io.*;

/**
 * convenience methods for io
 */
public class IoUtil {
  private static final Logger log
    = Logger.getLogger(IoUtil.class.getName());
  
  public static int readInt(InputStream is)
    throws IOException
  {
    return ((is.read() << 24)
        + (is.read() << 16)
        + (is.read() << 8)
        + is.read());
  }
  
  public static void writeInt(OutputStream os, int v)
    throws IOException
  {
    os.write(v >> 24);
    os.write(v >> 16);
    os.write(v >> 8);
    os.write(v);
  }
  
  public static long readLong(InputStream is)
    throws IOException
  {
    return (((long) is.read() << 56)
        + ((long) is.read() << 48)
        + ((long) is.read() << 40)
        + ((long) is.read() << 32)
        + ((long) is.read() << 24)
        + ((long) is.read() << 16)
        + ((long) is.read() << 8)
        + ((long) is.read()));
  }
  
  public static void writeLong(OutputStream os, long v)
    throws IOException
  {
    os.write((int) (v >> 56));
    os.write((int) (v >> 48));
    os.write((int) (v >> 40));
    os.write((int) (v >> 32));
    os.write((int) (v >> 24));
    os.write((int) (v >> 16));
    os.write((int) (v >> 8));
    os.write((int) v);
  }

  public static void close(InputStream is)
  {
    try {
      if (is != null)
        is.close();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  public static void close(OutputStream os)
  {
    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  public static void close(Writer os)
  {
    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
}
