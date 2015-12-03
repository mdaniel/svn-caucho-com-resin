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

package com.caucho.hmtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.actor.ActorHolder;
import com.caucho.bam.stream.MessageStream;

/**
 * HMTP client protocol
 */
public class HmtpLinkWorker implements Runnable {
  protected InputStream _is;
  protected OutputStream _os;

  private String _address;

  private MessageStream _actorStream;
  
  private HmtpWebSocketWriter _toLinkStream;
  private HmtpWebSocketReader _in;

  public HmtpLinkWorker(ActorHolder actor, InputStream is, OutputStream os)
    throws IOException
  {
    _actorStream = actor.getActor();
    
    _is = is;
    _os = os;

    _toLinkStream = new HmtpWebSocketWriter(_os);
    _in = new HmtpWebSocketReader(_is);

    /*
    if (actor.getAddress() == null)
      actor.setAddress(actor.getClass().getSimpleName() + "@link");
      */
    
    actor.setBroker(_toLinkStream);
  }

  public String getAddress()
  {
    return _address;
  }

  public void setAddress(String address)
  {
    _address = address;
  }

  public MessageStream getLinkStream()
  {
    return _toLinkStream;
  }
  
  /**
   * Returns the current stream to the actor, throwing an exception if
   * it's unavailable
   */
  public MessageStream getActorStream()
  {
    MessageStream stream = _actorStream;

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
  @Override
  public void run()
  {
    try {
      while (! isClosed() && _in.readPacket(_actorStream)) {
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
