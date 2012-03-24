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
import com.caucho.amp.actor.AmpActor;
import com.caucho.amp.mailbox.AmpMailbox;
import com.caucho.amp.mailbox.SimpleAmpMailbox;
import com.caucho.amp.router.AmpRouter;
import com.caucho.amp.router.HashMapAmpRouter;
import com.caucho.amp.skeleton.AmpReflectionSkeletonFactory;

/**
 * Creates MPC skeletons and stubs.
 */
public class AmpManagerImpl implements AmpManager
{
  private final AtomicLong _clientId = new AtomicLong();
  
  private HashMapAmpRouter _router = new HashMapAmpRouter();
  
  @Override
  public AmpRouter getRouter()
  {
    return _router;
  }

  @Override
  public <T> T createClient(Class<T> api, String to)
  {
    String from = "urn:amp:client:/" + api.getSimpleName() + "/" + _clientId.incrementAndGet();
    
    return createClient(api, to, from);
  }
  
  public <T> T createClient(Class<T> api, String to, String from)
  {
    AmpReflectionSkeletonFactory factory = new AmpReflectionSkeletonFactory();
    
    return factory.createStub(api, getRouter(), to, from);
  }

  @Override
  public void addActor(AmpActor actor)
  {
    getRouter().addMailbox(createMailbox(actor));
  }
  
  public void addActorBean(Object bean, String address)
  {
    AmpReflectionSkeletonFactory factory = new AmpReflectionSkeletonFactory();
    
    AmpActor actor = factory.createSkeleton(bean, address);
    
    addActor(actor);
  }
  
  protected AmpMailbox createMailbox(AmpActor actor)
  {
    return new SimpleAmpMailbox(actor);
  }
}
