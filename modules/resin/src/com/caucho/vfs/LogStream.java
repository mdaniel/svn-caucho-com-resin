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

package com.caucho.vfs;

import java.io.*;
import java.net.*;
import java.util.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * The primary debugging stream in Resin.
 */
public class LogStream extends StreamImpl implements ChangeListener {
  private static int changeCount;
  
  private String logId;
  private byte []idBuf;
  private RotateStream dest;
  private String timestampFormat;
  private QDate calendar;
  private boolean doLogId;

  LogStream(String logId)
  {
    if (logId.charAt(logId.length() - 1) != '/')
      logId = logId + '/';

    this.logId = logId;
    this.idBuf = (logId + ": ") .getBytes();

    calendar = new QDate();

    changeCount = Registry.getChangeCount();

    init();

    Registry.addListener(this);
  }

  public void setDoLogId(boolean logId)
  {
    this.doLogId = logId;
  }

  /**
   * When the resin.conf changes, update the logging.
   */
  public void handleChange(Object object)
  {
    dest = null;
    
    init();
  }

  private void init()
  {
    RegistryNode root = Registry.lookup("/resin");
    if (root == null)
      root = Registry.lookup("/caucho.com");
    if (root == null) {
      return;
    }
    
    String bestId = "/";
    RegistryNode bestNode = null;

    Iterator iter = root.select("log");
    while (iter.hasNext()) {
      RegistryNode node = (RegistryNode) iter.next();

      String id = node.getValue();
      if (id == null)
	id = "/";
      else if (id.length() == 0 || id.charAt(id.length() - 1) != '/')
	id = id + '/';

      if (logId.startsWith(id) && id.startsWith(bestId)) {
	bestId = id;
	bestNode = node;
      }
    }

    if (bestNode == null)
      return;

    String bestHref = bestNode.getPath("href", null);
    if (bestHref == null)
      return;

    if (bestNode.lookup("timestamp") != null)
      timestampFormat = bestNode.getString("timestamp",
					   "[%Y/%m/%d %H:%M:%S]");

    Path path = Vfs.lookupNative(bestHref);

    try {
      path.getParent().mkdirs();
    } catch (IOException e) {
    }

    dest = RotateStream.create(path);

    try {
      int maxRollover = bestNode.getInt("rollover-count", -1);
      if (maxRollover > 0)
        dest.setMaxRolloverCount(maxRollover);
      
      int size = bestNode.getInt("rollover-size", -1);
      if (size > 0)
        dest.setRolloverSize(size);
      
      long period = bestNode.getPeriod("rollover-period", -1);
      if (period > 0)
        dest.setRolloverPeriod(period);
    } catch (RegistryException e) {
      e.printStackTrace();
    }
  }

  /**
   * Create a new stream based on the logId.  The LogStream will only
   * write to something if it's configured in the resin.conf.
   *
   * @param logId the name of the debug stream to open
   */

  public static WriteStream open(String logId)
  {
    LogStream log = new LogStream(logId);
    WriteStream stream = new WriteStream(log);

    stream.setFlushOnNewline(true);

    return stream;
  }
  
  /**
   * Create a new stream based on the logId.  The LogStream will only
   * write to something if it's configured in the resin.conf.
   *
   * @param logId the name of the debug stream to open
   */
  public static WriteStream openQuiet(String logId)
  {
    LogStream log = new LogStream(logId);
    log.setDoLogId(false);
    WriteStream stream = new WriteStream(log);

    stream.setFlushOnNewline(true);

    return stream;
  }

  public final boolean canWrite()
  {
    return dest != null;
  }

  /**
   * Writes the buffered chunk to the underlying stream.
   *
   * @param buf byte buffer to write
   * @param offset offset into the buffer to start writing
   * @param length length of the buffer to write
   * @param isEnd true when the stream is closing.
   */
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    RotateStream dest = this.dest;
    if (dest == null)
      return;

    WriteStream os = dest.getStream();
    if (os == null)
      return;
    
    synchronized (os) {
      if (timestampFormat != null) {
        calendar.setGMTTime(System.currentTimeMillis());
        String date = calendar.format(timestampFormat);
        byte []bytes = date.getBytes();
        os.write(bytes, 0, bytes.length);
        os.print(' ');
      }
      if (doLogId)
        os.write(idBuf, 0, idBuf.length);
      os.write(buf, offset, length);
      os.flush();
    }
  }

  public void flush()
  {
  }

  public void close()
  {
  }

  public void destroy()
  {
    Registry.removeListener(this);
  }
}
