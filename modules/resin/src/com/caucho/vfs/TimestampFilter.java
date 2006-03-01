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

/**
 * Automatically-rotating streams.  Normally, clients will call
 * getStream instead of using the StreamImpl interface.
 */
public class TimestampFilter extends StreamImpl {
  private WriteStream _stream;
  
  private String _timestamp;

  private QDate _calendar = new QDate(true);

  private boolean _isLineBegin = true;

  /**
   * Create listener.
   *
   * @param path underlying log path
   */
  public TimestampFilter()
  {
  }

  /**
   * Create listener.
   *
   * @param path underlying log path
   */
  public TimestampFilter(WriteStream out, String timestamp)
  {
    _stream = out;
    _timestamp = timestamp;
  }

  public void setTimestamp(String timestamp)
  {
    _timestamp = timestamp;
  }

  public void setStream(WriteStream stream)
  {
    _stream = stream;
  }

  public Path getPath()
  {
    if (_stream != null)
      return _stream.getPath();
    else
      return super.getPath();
  }

  /**
   * Returns true if the stream can write.
   */
  public boolean canWrite()
  {
    return _stream != null && _stream.canWrite();
  }

  /**
   * Write data to the stream.
   */
  public void write(byte []buffer, int offset, int length, boolean isEnd)
    throws IOException
  {
    if (_stream == null)
      return;

    if (_timestamp == null) {
      _stream.write(buffer, offset, length);
      return;
    }
    
    long now;

    if (CauchoSystem.isTesting())
      now = Alarm.getCurrentTime();
    else
      now = System.currentTimeMillis();
    
    for (int i = 0; i < length; i++) {
      if (_isLineBegin) {
        _stream.print(_calendar.formatLocal(now, _timestamp));
        _isLineBegin = false;
      }

      int ch = buffer[offset + i];
      _stream.write(ch);
      
      if (ch == '\n' ||
          ch == '\r' && i + 1 < length && buffer[offset + i + 1] != '\n')
        _isLineBegin = true;
    }
  }

  /**
   * Flushes the data.
   */
  public void flush()
    throws IOException
  {
    if (_stream != null)
      _stream.flush();
  }

  /**
   * Flushes the data.
   */
  public void close()
    throws IOException
  {
    if (_stream != null)
      _stream.close();
  }
}
