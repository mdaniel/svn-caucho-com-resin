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

package com.caucho.v5.config.candi;

import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.v5.config.bytecode.ScopeAdapter;

/**
 * factory for bean instances.
 */
public class ReferenceFactoryNormalContext<T> extends ReferenceFactory<T> {
  private Bean<T> _bean;
  private CandiManager _manager;
  private ScopeAdapterBean<T> _scopeAdapterBean;
  private Context _context;
  private T _scopeAdapter;

  ReferenceFactoryNormalContext(CandiManager manager,
                                Bean<T> bean,
                                ScopeAdapterBean<T> scopeAdapterBean,
                                Context context)
  {
    _bean = bean;
    _scopeAdapterBean = scopeAdapterBean;

    _manager = manager;
    _context = context;
    
    BeanManagerBase beanManager = manager.getBeanManager(bean);

    ScopeAdapter scopeAdapter = ScopeAdapter.create(bean);
    _scopeAdapter = scopeAdapter.wrap(beanManager.createNormalInstanceFactory(bean));
  }

  protected void validate()
  {
    _manager.validateNormal(_bean);
  }

  @Override
  public Bean<T> getBean()
  {
    return _bean;
  }

  @Override
  public T create(CreationalContextImpl<T> env,
                  CreationalContextImpl<?> parentEnv,
                  InjectionPoint ip)
  {
    return _scopeAdapter;
  }
}

