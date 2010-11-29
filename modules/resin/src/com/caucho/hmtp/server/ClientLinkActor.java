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

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import com.caucho.bam.Actor;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.bam.BamSkeleton;
import com.caucho.hmtp.HmtpWebSocketListener;
import com.caucho.hmtp.HmtpWebSocketWriter;
import com.caucho.websocket.WebSocketListener;
import com.caucho.websocket.WebSocketServletRequest;

/**
 * HmtpWriteStream writes HMTP packets to an OutputStream.
 */
public class ClientLinkActor implements Actor, ActorStream
{
  private static final Logger log
    = Logger.getLogger(ClientLinkActor.class.getName());

  private String _jid;
  private HmtpWebSocketWriter _out;

  public ClientLinkActor(String jid, HmtpWebSocketWriter out)
  {
    _jid = getClass().getSimpleName() + "@localhost";
    _out = out;
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

  /* (non-Javadoc)
   * @see com.caucho.bam.Actor#getActorStream()
   */
  @Override
  public ActorStream getActorStream()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.Actor#getLinkStream()
   */
  @Override
  public ActorStream getLinkStream()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.Actor#setActorStream(com.caucho.bam.ActorStream)
   */
  @Override
  public void setActorStream(ActorStream actorStream)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.Actor#setLinkStream(com.caucho.bam.ActorStream)
   */
  @Override
  public void setLinkStream(ActorStream linkStream)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#close()
   */
  @Override
  public void close()
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#isClosed()
   */
  @Override
  public boolean isClosed()
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#message(java.lang.String, java.lang.String, java.io.Serializable)
   */
  @Override
  public void message(String to, String from, Serializable payload)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#messageError(java.lang.String, java.lang.String, java.io.Serializable, com.caucho.bam.ActorError)
   */
  @Override
  public void messageError(String to, String from, Serializable value,
                           ActorError error)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#queryError(long, java.lang.String, java.lang.String, java.io.Serializable, com.caucho.bam.ActorError)
   */
  @Override
  public void queryError(long id, String to, String from, Serializable payload,
                         ActorError error)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#queryGet(long, java.lang.String, java.lang.String, java.io.Serializable)
   */
  @Override
  public void queryGet(long id, String to, String from, Serializable payload)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#queryResult(long, java.lang.String, java.lang.String, java.io.Serializable)
   */
  @Override
  public void queryResult(long id, String to, String from, Serializable payload)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#querySet(long, java.lang.String, java.lang.String, java.io.Serializable)
   */
  @Override
  public void querySet(long id, String to, String from, Serializable payload)
  {
    // TODO Auto-generated method stub
    
  }
}
