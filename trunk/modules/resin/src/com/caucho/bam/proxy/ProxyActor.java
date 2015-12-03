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
 * @author Emil Ong
 */

package com.caucho.bam.proxy;

import java.io.Serializable;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.actor.Actor;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.SimpleActorSender;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.stream.MessageStream;
import com.caucho.bam.stream.NullMessageStream;

/**
 * The Skeleton introspects and dispatches messages for a
 * {@link com.caucho.bam.actor.SimpleActor}
 * or {@link com.caucho.bam.actor.SkeletonActorFilter}.
 */
public class ProxyActor<T> implements Actor
{
  private static final Logger log
    = Logger.getLogger(ProxyActor.class.getName());
  
  private String _address;
  private T _bean;
  private Broker _broker;
  private ProxySkeleton<T> _skeleton;
  private SimpleActorSender _sender;
  private MessageStream _fallback;
  
  public ProxyActor(T bean,
                    String address,
                    Broker broker)
  {
    _address = address;
    _bean = bean;
    _broker = broker;
    _skeleton = ProxySkeleton.getSkeleton((Class<T>) bean.getClass());
    _fallback = new NullMessageStream(address, broker);
    
    _sender = new SimpleActorSender(address, this, _broker);
  }

  @Override
  public String getAddress()
  {
    return _address;
  }
  
  public ActorSender getSender()
  {
    return _sender;
  }

  @Override
  public Broker getBroker()
  {
    return _broker;
  }
  
  public Actor getActor()
  {
    return _sender.getActor();
  }

  @Override
  public boolean isClosed()
  {
    return false;
  }

  @Override
  public void message(String to, String from, Serializable payload)
  {
    _skeleton.message(_bean, _fallback, to, from, payload);
  }

  @Override
  public void messageError(String to, String from, Serializable payload,
                           BamError error)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void query(long id, String to, String from, Serializable payload)
  {
    _skeleton.query(_bean, _fallback, getBroker(), id, to, from, payload);
  }

  @Override
  public void queryResult(long id, String to, String from, Serializable payload)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void queryError(long id, String to, String from, Serializable payload,
                         BamError error)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "," + _bean + "]";
  }
}
