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

package com.caucho.env.actor;

import com.caucho.util.L10N;
import com.caucho.util.RingItem;
import com.caucho.util.RingItemFactory;

/**
 * Interface for an actor queue
 */
public class ActorQueuePreallocBuilder<T extends RingItem>
  extends AbstractActorQueueBuilder<T>
{
  private static final L10N L = new L10N(ActorQueuePreallocBuilder.class);
  
  private RingItemFactory<T> _factory;
  
  public ActorQueuePreallocBuilder<T> factory(RingItemFactory<T> factory)
  {
    _factory = factory;
    
    return this;
  }
  
  public RingItemFactory<T> getFactory()
  {
    return _factory;
  }
  
  @Override
  protected void validateBuilder()
  {
    super.validateBuilder();
    
    if (_factory == null) {
      throw new IllegalStateException(L.l("itemFactory is required"));
    }
  }
  
  public ActorQueuePreallocApi<T> build()
  {
    validateBuilder();
    
    return new ActorQueue<T>(getCapacity(),
                             getFactory(),
                             getProcessors());
  }
}
