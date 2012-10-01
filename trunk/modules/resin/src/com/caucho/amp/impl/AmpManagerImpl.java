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

package com.caucho.amp.impl;

import java.util.concurrent.atomic.AtomicLong;

import com.caucho.amp.AmpManager;
import com.caucho.amp.AmpManagerBuilder;
import com.caucho.amp.actor.AbstractAmpActor;
import com.caucho.amp.actor.ActorContextImpl;
import com.caucho.amp.actor.ActorRefImpl;
import com.caucho.amp.actor.AmpActor;
import com.caucho.amp.actor.AmpActorContext;
import com.caucho.amp.actor.AmpActorRef;
import com.caucho.amp.actor.AmpProxyActor;
import com.caucho.amp.broker.AmpBroker;
import com.caucho.amp.broker.AmpBrokerFactory;
import com.caucho.amp.mailbox.AmpMailbox;
import com.caucho.amp.mailbox.AmpMailboxBuilder;
import com.caucho.amp.mailbox.AmpMailboxBuilderFactory;
import com.caucho.amp.mailbox.AmpMailboxFactory;
import com.caucho.amp.skeleton.AmpReflectionSkeletonFactory;
import com.caucho.amp.spi.AmpSpi;

/**
 * Creates MPC skeletons and stubs.
 */
public class AmpManagerImpl implements AmpManager
{
  private final AtomicLong _clientId = new AtomicLong();
  
  private final AmpBroker _broker;
  private final AmpMailboxBuilderFactory _mailboxBuilderFactory;
  private final AmpMailboxBuilder _mailboxFactory;
  private final AmpActorContext _systemContext;
  
  public AmpManagerImpl(AmpManagerBuilder builder)
  {
    AmpBrokerFactory brokerFactory = builder.getBrokerFactory();
    
    AmpBroker broker = brokerFactory.createBroker();

    if (broker == null) {
      throw new NullPointerException();
    }

    _broker = broker;
    
    AmpMailboxBuilderFactory mailboxBuilderFactory
      = builder.getMailboxBuilderFactory();
    
    if (mailboxBuilderFactory == null) {
      throw new NullPointerException();
    }

    _mailboxBuilderFactory = mailboxBuilderFactory;
    
    _mailboxFactory = _mailboxBuilderFactory.createMailboxBuilder();
    
    AmpActor nullStream = new AbstractAmpActor();
    _systemContext = new ActorContextImpl("system", nullStream, _mailboxFactory);
  }
  
  @Override
  public AmpBroker getBroker()
  {
    return _broker;
  }
  
  @Override
  public AmpActorContext getSystemContext()
  {
    return _systemContext;
  }
  
  @Override
  public <T> T createActorProxy(String to, Class<T> api)
  {
    String from = "urn:amp:client:/" + api.getSimpleName() + "/" + _clientId.incrementAndGet();
    
    return createClient(api, to, from);
  }

  @Override
  public <T> T createActorProxy(AmpActorRef to, Class<T> api)
  {
    AmpReflectionSkeletonFactory factory = new AmpReflectionSkeletonFactory();
    
    return factory.createStub(api, 
                              to,
                              _systemContext);
  }
  
  public <T> T createClient(Class<T> api, String to, String from)
  {
    AmpReflectionSkeletonFactory factory = new AmpReflectionSkeletonFactory();
    
    AmpProxyActor proxyActor = new AmpProxyActor(from, _mailboxFactory);
    
    AmpMailbox mailbox = proxyActor.getActorContext().getMailbox();
    
    AmpActorRef toRef = getBroker().getActorRef(to);
    
    getBroker().addMailbox(from, mailbox);
    
    return factory.createStub(api, 
                              toRef,
                              _systemContext);
  }

  @Override
  public AmpActorRef addActor(String address, AmpActor actor)
  {
    AmpActorContext actorContext = createActorContext(address, actor);
    
    // AmpMailbox mailbox = getMailboxFactory().createMailbox(actorContext);
    
    return getBroker().addMailbox(address, actorContext.getMailbox());
  }
  
  @Override
  public AmpActorRef addActor(String address, Object bean)
  {
    AmpReflectionSkeletonFactory factory = new AmpReflectionSkeletonFactory();
    
    AmpActor actor = factory.createSkeleton(bean, 
                                            address,
                                            getBroker());
    
    return addActor(address, actor);
  }
  
  /*
  protected AmpMailbox createMailbox(String address, AmpActor actor)
  {
    return new SimpleAmpMailbox(createActorContext(address, actor));
  }
  */
  
  protected AmpActorContext createActorContext(String address, AmpActor actor)
  {
    return new ActorContextImpl(address, actor, getMailboxFactory());
  }
  
  protected AmpMailboxFactory getMailboxFactory()
  {
    return _mailboxFactory;
  }
}
