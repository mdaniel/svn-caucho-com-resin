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
 * @author Alex Rojkov
 */

package com.caucho.v5.config.extension;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.v5.config.candi.BeanManagerBase;
import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.inject.Module;

@Module
public class AfterTypeDiscoveryImpl implements AfterTypeDiscovery
{
  private CandiManager _cdiManager;
  private List<Class<?>> _decorators;
  private List<Class<?>> _interceptors;
  private List<Class<?>> _alternatives;

  AfterTypeDiscoveryImpl(CandiManager cdiManager)
  {
    _cdiManager = cdiManager;
    
    BeanManagerBase beanManager = _cdiManager.getBeanManager();

    List<Class<?>> decorators = beanManager.getDecorators();
    _decorators = new ArrayList<>(decorators);
    
    List<Class<?>> interceptors = beanManager.getInterceptors();
    _interceptors = new ArrayList<>(interceptors);

    List<Class<?>> alternatives= beanManager.getAlternatives();
    _alternatives = new ArrayList<>(alternatives);
  }

  @Override
  public void addAnnotatedType(AnnotatedType<?> annType, String id)
  {
    _cdiManager.addSyntheticAfterTypeDiscovery(annType, id);
  }

  @Override
  public List<Class<?>> getAlternatives()
  {
    return _alternatives;
  }

  @Override
  public List<Class<?>> getInterceptors()
  {
    return _interceptors;
  }

  @Override
  public List<Class<?>> getDecorators()
  {
    return _decorators;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cdiManager + "]";
  }
}
