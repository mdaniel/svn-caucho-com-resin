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

package com.caucho.v5.config.program;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.TransientReference;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.candi.CreationalContextImpl;
import com.caucho.v5.config.candi.ReferenceFactory;
import com.caucho.v5.inject.Module;

/**
 * Custom bean configured by namespace
 */
@Module
public class BeanArg<T> extends Arg<T> {
  private CandiManager _injectManager;
  private Type _type;
  private Annotation []_bindings;
  private ReferenceFactory<?> _factory;
  private InjectionPoint _ip;
  private boolean _isTransientReference;

  public BeanArg(CandiManager injectManager,
                 Type type, 
                 Annotation []bindings,
                 InjectionPoint ip)
  {
    _injectManager = injectManager;
    
    _type = type;
    _bindings = bindings;
    
    _ip = ip;

    if (ip != null)
      _isTransientReference
        = ip.getAnnotated().isAnnotationPresent(TransientReference.class);
  }

  @Override
  public void bind()
  {
    if (_factory == null) {
      HashSet<Annotation> qualifiers = new HashSet<Annotation>();
      
      for (Annotation ann : _bindings) {
        qualifiers.add(ann);
      }
      
      _factory = _injectManager.getBeanManager().getReferenceFactory(_type, qualifiers, _ip);
    }
  }

  @Override
  public Object eval(CreationalContext<T> parentEnv)
  {
    if (_factory == null)
      bind();

    return _factory.create(null, (CreationalContextImpl<T>) parentEnv, _ip);
  }

  @Override
  public void destroy(Object value, CreationalContext<T> parentEnv)
  {
    if (! _isTransientReference)
      return;

    Bean bean = _factory.getBean();
    bean.destroy(value, parentEnv);
  }

  public Type getType() {
    return _type;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _factory + "]";
  }
}
