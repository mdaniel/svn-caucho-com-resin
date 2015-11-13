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

package com.caucho.v5.config.extension;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.inject.Module;

import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ObserverMethod;

@Module
public class AfterBeanDiscoveryImpl implements AfterBeanDiscovery
{
  private CandiManager _cdiManager;
 
  AfterBeanDiscoveryImpl(CandiManager cdiManager)
  {
    _cdiManager = cdiManager;
  }
  
  public void addBean(Bean<?> bean)
  {
    _cdiManager.addBeanDiscover(bean);
  }

  @Override
  public void addContext(Context context)
  {
    _cdiManager.addContext(context);
  }

  @Override
  public void addObserverMethod(ObserverMethod<?> observerMethod)
  {
    _cdiManager.getEventManager().addObserver(observerMethod);
  }

  @Override
  public void addDefinitionError(Throwable t)
  {
    _cdiManager.addDefinitionError(t);
  }

  public boolean hasDefinitionError()
  {
    return false;
  }

  @Override
  public <T> AnnotatedType<T> getAnnotatedType(Class<T> type, String id)
  {
    return _cdiManager.getAnnotatedType(type, id);
  }

  @Override
  public <T> Iterable<AnnotatedType<T>> getAnnotatedTypes(Class<T> type)
  {
    return _cdiManager.getAnnotatedTypes(type);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cdiManager + "]";
  }
}
