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

/**
 * Interface for an actor queue
 */
abstract class AbstractActorQueueBuilder<T>
{
  private static final L10N L = new L10N(AbstractActorQueueBuilder.class);
  
  private ActorProcessor<? super T> []_processors;
  private int _initial;
  private int _capacity;
  private boolean _isMultiworker;
  private int _multiworkerOffset = 4;
  
  public AbstractActorQueueBuilder<T>
  processors(ActorProcessor<? super T> ...processors)
  {
    if (processors == null)
      throw new NullPointerException();
    else if (processors.length == 0)
      throw new IllegalArgumentException();
    
    _processors = processors;
    
    return this;
  }
  
  public ActorProcessor<? super T> []getProcessors()
  {
    return _processors;
  }
  
  public AbstractActorQueueBuilder<T> capacity(int capacity)
  {
    _capacity = capacity;
    
    return this;
  }
  
  public int getCapacity()
  {
    return _capacity;
  }
  
  public AbstractActorQueueBuilder<T> initial(int initial)
  {
    _initial = initial;
    
    return this;
  }
  
  public int getInitial()
  {
    return _initial;
  }
  
  public AbstractActorQueueBuilder<T> multiworker(boolean isMultiworker)
  {
    _isMultiworker = isMultiworker;
    
    return this;
  }
  
  public boolean isMultiworker()
  {
    return _isMultiworker;
  }
  
  public AbstractActorQueueBuilder<T> multiworkerOffset(int offset)
  {
    _multiworkerOffset = offset;
    
    return this;
  }
  
  public int getMultiworkerOffset()
  {
    return _multiworkerOffset;
  }

  protected void validateFullBuilder()
  {
    if (_processors == null) {
      throw new IllegalStateException(L.l("processors is required"));
    }
    
    validateBuilder();
  }

  protected void validateBuilder()
  {
    if (_capacity <= 0) {
      throw new IllegalStateException(L.l("capacity is required"));
    }
  }
}
