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

import com.caucho.v5.util.L10N;

/**
 * factory for bean instances.
 */
public class ReferenceFactoryContext<T> extends ReferenceFactory<T> {
  private static final L10N L = new L10N(ReferenceFactoryContext.class);
  
  private Bean<T> _bean;
  private Context _context;

  ReferenceFactoryContext(Bean<T> bean,
                          Context context)
  {
    _bean = bean;
    _context = context;
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
    Bean<T> bean = _bean;

    T instance = CreationalContextImpl.find(parentEnv, bean);

    if (instance != null) {
      return instance;
    }

    if (env == null) {
      env = new OwnerCreationalContext<T>(bean, parentEnv);
    }

    instance = _context.get(bean, env);
    
    if (instance == null) {
      throw new NullPointerException(L.l("null instance returned by '{0}' for bean '{1}'",
                                         _context, bean));
    }

    return instance;
  }
}

