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

package com.caucho.hmtp;

import com.caucho.bam.QueryCallback;
import com.caucho.bam.ActorStream;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorClient;
import com.caucho.bam.SimpleActorClient;
import com.caucho.bam.SimpleActorStream;
import com.caucho.bam.ActorException;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.hessian.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.security.PublicKey;
import javax.servlet.http.HttpServletResponse;

/**
 * HMTP client protocol
 */
public class HmtpLink implements Runnable {
  private static final Logger log
    = Logger.getLogger(HmtpLink.class.getName());

  protected InputStream _is;
  protected OutputStream _os;

  private String _jid;

  private SimpleActorStream _actorStream;
  
  private HmtpWriter _toLinkStream;
  private HmtpReader _in;

  public HmtpLink(SimpleActorStream actorStream, InputStream is, OutputStream os)
  {
    _actorStream = actorStream;
    
    _is = is;
    _os = os;

    _toLinkStream = new HmtpWriter(_os);
    _in = new HmtpReader(_is);

    if (actorStream.getJid() == null)
      actorStream.setJid(actorStream.getClass().getSimpleName() + "@link");
    
    actorStream.setLinkStream(_toLinkStream);
  }

  public String getJid()
  {
    return _jid;
  }

  public void setJid(String jid)
  {
    _jid = jid;
  }
		       
  public ActorStream getLinkStream()
  {
    return _toLinkStream;
  }
  
  /**
   * Returns the current stream to the broker, throwing an exception if
   * it's unavailable
   */
  public ActorStream getActorStream()
  {
    ActorStream stream = _actorStream;

    if (stream != null)
      return stream;
    else
      throw new RemoteConnectionFailedException("connection has been closed");
  }

  public boolean isClosed()
  {
    return _actorStream == null;
  }

  /**
   * Receive messages from the client
   */
  public void run()
  {
    try {
      while (! isClosed()) {
	_in.readPacket(_actorStream);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
 
  public void close()
  {
    _actorStream = null;
  }
}
