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

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * factory for bean instances.
 */
public class ReferenceFactoryDependent<T> extends ReferenceFactory<T>
{
  private Bean<T> _bean;

  ReferenceFactoryDependent(Bean<T> bean)
  {
    _bean = bean;
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
      if (parentEnv != null)
        env = new DependentCreationalContext<T>(bean, parentEnv, ip);
      else {
        env = new OwnerCreationalContext<T>(bean);

        if (ip != null) {
          env = new DependentCreationalContext<T>(bean, env, ip);
        }
      }
    }

    instance = bean.create(env);

    env.push(instance);

    /*
      if (env.isTop() && ! (bean instanceof CdiStatefulBean)) {
        bean.destroy(instance, env);
      }
     */

    return instance;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bean + "]";
  }
}

