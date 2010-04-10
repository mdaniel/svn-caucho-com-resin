/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.program;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

/**
 * Custom bean configured by namespace
 */
@Module
public class BeanArg<T> extends Arg<T> {
  private InjectManager _beanManager;
  private Type _type;
  private Annotation []_bindings;
  private Bean<?> _bean;

  public BeanArg(Type type, Annotation []bindings)
  {
    _beanManager = InjectManager.create();
    
    _type = type;
    _bindings = bindings;
  }

  @Override
  public void bind()
  {
    if (_bean == null) {
      HashSet<Annotation> bindings = new HashSet<Annotation>();
      
      for (Annotation ann : _bindings) {
	bindings.add(ann);
      }
      
      _bean = _beanManager.resolveByInjectionPoint(_type, bindings, null);
      /*
      for (Bean bean : _beanManager.getBeans(_type, _bindings)) {
	_bean = bean;
      }

      if (_bean == null)
	throw new ConfigException(L.l("No matching bean for '{0}' with bindings {1}",
				      _type, toList(_bindings)));
      */
    }
  }

  @Override
  public Object eval(CreationalContext<T> parentEnv)
  {
    if (_bean == null)
      bind();

    CreationalContext<T> beanEnv = new CreationalContextImpl(_bean, parentEnv);
    
    // XXX: getInstance for injection?
    return _beanManager.getReference(_bean, _type, beanEnv);
  }
}
