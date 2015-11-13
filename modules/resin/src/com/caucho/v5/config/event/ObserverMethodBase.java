/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;

import com.caucho.v5.inject.Module;

/**
 *
 */
@Module
abstract public class ObserverMethodBase<T>
  implements ObserverMethod<T>, Comparable<ObserverMethod<?>>
{
  @Override
  public Class<?> getBeanClass()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Type getObservedType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Set<Annotation> getObservedQualifiers()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Reception getReception()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public TransactionPhase getTransactionPhase()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public final void notify(T event)
  {
    notify(event, null);
  }

  public abstract void notify(T event, InjectionPoint injectionPoint);
  
  @Override
  public int compareTo(ObserverMethod<?> peer)
  {
    int cmp = getObservedType().toString().compareTo(peer.getObservedType().toString());
    
    if (cmp != 0) {
      return cmp;
    }
    
    cmp = getBeanClass().getName().compareTo(peer.getBeanClass().getName());
    
    if (cmp != 0) {
      return cmp;
    }
    
    return System.identityHashCode(this) - System.identityHashCode(peer);
  }
}
