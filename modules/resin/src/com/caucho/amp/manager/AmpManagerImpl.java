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

package com.caucho.amp.manager;

import java.util.concurrent.atomic.AtomicLong;

import com.caucho.amp.AmpManager;
import com.caucho.amp.actor.ActorContextImpl;
import com.caucho.amp.actor.ActorRefImpl;
import com.caucho.amp.actor.AmpActor;
import com.caucho.amp.actor.AmpActorContext;
import com.caucho.amp.actor.AmpActorRef;
import com.caucho.amp.actor.AmpProxyActor;
import com.caucho.amp.mailbox.AmpMailbox;
import com.caucho.amp.mailbox.AmpMailboxFactory;
import com.caucho.amp.mailbox.SimpleAmpMailbox;
import com.caucho.amp.mailbox.SimpleMailboxFactory;
import com.caucho.amp.router.AmpBroker;
import com.caucho.amp.router.HashMapAmpBroker;
import com.caucho.amp.skeleton.AmpReflectionSkeletonFactory;
import com.caucho.amp.spi.AmpSpi;

/**
 * Creates MPC skeletons and stubs.
 */
public class AmpManagerImpl implements AmpManager
{
  private final AtomicLong _clientId = new AtomicLong();
  
  private HashMapAmpBroker _broker = new HashMapAmpBroker();
  private AmpMailboxFactory _mailboxFactory = new SimpleMailboxFactory();
  
  public AmpManagerImpl()
  {
  }
  
  @Override
  public AmpBroker getBroker()
  {
    return _broker;
  }
  

  @Override
  public <T> T createActorProxy(String to, Class<T> api)
  {
    String from = "urn:amp:client:/" + api.getSimpleName() + "/" + _clientId.incrementAndGet();
    
    return createClient(api, to, from);
  }
  
  public <T> T createClient(Class<T> api, String to, String from)
  {
    AmpReflectionSkeletonFactory factory = new AmpReflectionSkeletonFactory();
    
    AmpProxyActor proxyActor = new AmpProxyActor(from, _mailboxFactory);
    
    AmpMailbox mailbox = proxyActor.getActorContext().getMailbox();
    
    AmpActorRef toRef = getBroker().getActorRef(to);
    AmpActorRef fromRef = proxyActor.getActorContext().getActorRef();
    
    getBroker().addMailbox(from, mailbox);

    return factory.createStub(api, 
                              getBroker(), 
                              proxyActor.getActorContext(),
                              toRef, fromRef);
  }

  @Override
  public void addActor(String address, AmpActor actor)
  {
    getBroker().addMailbox(address, createMailbox(address, actor));
  }
  
  @Override
  public void addActor(String address, Object bean)
  {
    AmpReflectionSkeletonFactory factory = new AmpReflectionSkeletonFactory();
    
    AmpActor actor = factory.createSkeleton(bean, 
                                            address,
                                            getBroker());
    
    addActor(address, actor);
  }
  
  protected AmpMailbox createMailbox(String address, AmpActor actor)
  {
    return new SimpleAmpMailbox(createActorContext(address, actor));
  }
  
  protected AmpActorContext createActorContext(String address, AmpActor actor)
  {
    return new ActorContextImpl(address, actor, getMailboxFactory());
  }
  
  protected AmpMailboxFactory getMailboxFactory()
  {
    return new SimpleMailboxFactory();
  }
}
