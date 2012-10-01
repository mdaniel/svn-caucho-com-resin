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

package com.caucho.hmtp.server;

import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.actor.ActorHolder;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.stream.MessageStream;

/**
 * HmtpWriteStream writes HMTP packets to an OutputStream.
 */
class ClientLinkActor implements MessageStream
{
  private String _address;
  
  private Broker _broker;
  private MessageStream _actorStream;
  private MessageStream _out;

  public ClientLinkActor(String address, Broker broker, MessageStream out)
  {
    if (address == null)
      throw new IllegalArgumentException();
    
    if (out == null)
      throw new IllegalArgumentException();
    
    _address = address;
    _broker = broker;
    _out = out;
    
    _actorStream = _out;
  }

  @Override
  public String getAddress()
  {
    return _address;
  }
  
  //@Override
  public MessageStream getActorStream()
  {
    return _actorStream;
  }

  public void setActorStream(MessageStream actorStream)
  {
    _actorStream = actorStream;
  }
  
  @Override
  public Broker getBroker()
  {
    return _broker;
  }

  //@Override
  public void setBroker(Broker broker)
  {
    _broker = broker;
  }
  
  @Override
  public boolean isClosed()
  {
    return false;
  }

  @Override
  public void message(String to, String from, Serializable payload)
  {
    getBroker().message(to, getAddress(), payload);
  }

  @Override
  public void messageError(String to, String from, Serializable payload,
                           BamError error)
  {
    getBroker().messageError(to, getAddress(), payload, error);
  }

  @Override
  public void query(long id, 
                    String to, 
                    String from, 
                    Serializable payload)
  {
    getBroker().query(id, to, getAddress(), payload);
  }

  @Override
  public void queryResult(long id, 
                          String to, 
                          String from, 
                          Serializable payload)
  {
    getBroker().queryResult(id, to, getAddress(), payload);
  }

  @Override
  public void queryError(long id, 
                         String to,
                         String from, 
                         Serializable payload,
                         BamError error)
  {
    getBroker().queryError(id, to, getAddress(), payload, error);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
}
