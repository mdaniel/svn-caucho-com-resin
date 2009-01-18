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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.bam.hmtp;

import com.caucho.bam.BamStream;
import com.caucho.bam.BamError;
import com.caucho.hessian.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * ClientToServerLink stream handles client packets received from the server.
 */
class FromServerLinkStream extends FromLinkStream implements Runnable
{
  private static final Logger log
    = Logger.getLogger(FromServerLinkStream.class.getName());

  private static long _gId;
  
  private HmtpClient _client;
  private ClassLoader _loader;

  private BamStream _toLinkStream;
  private BamStream _toClientStream;

  FromServerLinkStream(HmtpClient client,
		       InputStream is)
  {
    super(is);

    _toLinkStream = client.getBrokerStream();
    _toClientStream = client.getAgentStream();

    _client = client;
    _loader = Thread.currentThread().getContextClassLoader();
  }

  public String getJid()
  {
    return _client.getJid();
  }

  protected BamStream getStream(String to)
  {
    return _toClientStream;
  }

  protected BamStream getLinkStream()
  {
    return _toLinkStream;
  }

  protected void close()
  {
    super.close();
    
    _client.close();
  }

  public void run()
  {
    Thread thread = Thread.currentThread();
    String oldName = thread.getName();
      
    try {
      thread.setName("hmpp-client-" + _gId++);
      thread.setContextClassLoader(_loader);
      
      while (! _client.isClosed()) {
	readPacket();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      close();

      thread.setName(oldName);
    }
  }
}
