/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.io.*;
import java.net.*;
import java.util.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Stream using with JNI.
 */
public class JniStream extends StreamImpl {
  private static boolean hasInitJni;
  
  private static NullPath nullPath;

  private int fd;
  private boolean flushOnNewline;
  private boolean closeChildOnClose = true;

  /**
   * Create a new JniStream based on the java.io.* stream.
   */
  public JniStream(int fd)
  {
    if (! hasInitJni)
      initJni();
    
    init(fd);
    if (nullPath == null)
      nullPath = new NullPath("jni-stream");
    setPath(nullPath);
  }
  
  public JniStream()
  {
    if (! hasInitJni)
      initJni();
    
    if (nullPath == null)
      nullPath = new NullPath("jni-stream");
    setPath(nullPath);
  }

  public void init(int fd)
  {
    this.fd = fd;
  }

  void initJni()
  {
    hasInitJni = true;
  }

  public boolean canRead()
  {
    return true;
  }

  public int read(byte []buf, int offset, int length)
    throws IOException
  {
    if (buf == null)
      throw new NullPointerException();
    else if (offset < 0 || buf.length < offset + length)
      throw new ArrayIndexOutOfBoundsException();
    
    return readNative(fd, buf, offset, length);
  }

  native int readNative(int fd, byte []buf, int offset, int length)
    throws IOException;

  // XXX: needs update
  public int getAvailable() throws IOException
  {
    return -1;
  }

  public boolean canWrite()
  {
    return true;
  }

  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    if (buf == null)
      throw new NullPointerException();
    else if (offset < 0 || buf.length < offset + length)
      throw new ArrayIndexOutOfBoundsException();
    
    int result = writeNative(fd, buf, offset, length);

    if (result < 0)
      throw new ClientDisconnectException("connection reset by peer");
  }

  native int writeNative(int fd, byte []buf, int offset, int length)
    throws IOException;

  public void flush()
    throws IOException
  {
    int result = flushNative(fd);

    if (result < 0)
      throw new ClientDisconnectException("connection reset by peer");
  }

  native int flushNative(int fd) throws IOException;

  public void close() throws IOException
  {
  }
}
