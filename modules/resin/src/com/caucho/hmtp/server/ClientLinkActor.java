/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.hmtp.server;

import java.io.Serializable;

import com.caucho.bam.Actor;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.hmtp.HmtpWebSocketWriter;

/**
 * HmtpWriteStream writes HMTP packets to an OutputStream.
 */
class ClientLinkActor implements Actor, ActorStream
{
  private String _jid;
  
  private ActorStream _linkStream;
  private ActorStream _actorStream;
  private ActorStream _out;

  public ClientLinkActor(String jid, ActorStream out)
  {
    _jid = jid;
    _out = out;
    
    _actorStream = _out;
  }

  @Override
  public String getJid()
  {
    return _jid;
  }

  @Override
  public void setJid(String jid)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public ActorStream getActorStream()
  {
    return _actorStream;
  }

  @Override
  public void setActorStream(ActorStream actorStream)
  {
    _actorStream = actorStream;
  }
  
  @Override
  public ActorStream getLinkStream()
  {
    return _linkStream;
  }

  @Override
  public void setLinkStream(ActorStream linkStream)
  {
    _linkStream = linkStream;
  }

  @Override
  public void close()
  {
  }

  @Override
  public boolean isClosed()
  {
    return false;
  }

  @Override
  public void message(String to, String from, Serializable payload)
  {
    getLinkStream().message(to, getJid(), payload);
  }

  @Override
  public void messageError(String to, String from, Serializable payload,
                           ActorError error)
  {
    getLinkStream().messageError(to, getJid(), payload, error);
  }

  @Override
  public void queryGet(long id, String to, String from, Serializable payload)
  {
    getLinkStream().queryGet(id, to, getJid(), payload);
  }

  @Override
  public void querySet(long id,
                       String to,
                       String from,
                       Serializable payload)
  {
    getLinkStream().querySet(id, to, getJid(), payload);
  }

  @Override
  public void queryResult(long id, 
                          String to, 
                          String from, 
                          Serializable payload)
  {
    getLinkStream().queryResult(id, to, getJid(), payload);
  }

  @Override
  public void queryError(long id, 
                         String to,
                         String from, 
                         Serializable payload,
                         ActorError error)
  {
    getLinkStream().queryError(id, to, getJid(), payload, error);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
