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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.session;

import javax.ejb.TimerService;

import com.caucho.config.gen.CandiEnhancedBean;
import com.caucho.config.inject.CreationalContextImpl;

/**
 * Abstract base class for an session context
 */
public class SingletonContext<X,T> extends AbstractSessionContext<X,T> {
  private T _proxy;
  
  public SingletonContext(SingletonManager<X> manager,
                          Class<T> api)
  {
    super(manager, api);
  }

  /**
   * Returns the server which owns this bean.
   */
  @Override
  public SingletonManager<X> getServer()
  {
    return (SingletonManager<X>) super.getServer();
  }
  
  @Override
  public T createProxy(CreationalContextImpl<T> env)
  {
    if (_proxy == null) {
      T proxy = super.createProxy(env);

      if (env != null)
        env.push(proxy);

      if (_proxy == null)
        _proxy = proxy;
      
      // ejb/6032
      getServer().initProxy(proxy, env);
    }
    
    return _proxy;
  }

  /**
   * Returns the timer service.
   */
  @Override
  public TimerService getTimerService()
    throws IllegalStateException
  {
    throw new IllegalStateException("Singleton session beans cannot call SessionContext.getTimerService()");
  }
  
  @Override
  public void destroy() throws Exception
  {
    super.destroy();
    
    T proxy = _proxy;
    _proxy = null;
    
    if (proxy instanceof CandiEnhancedBean) {
      CandiEnhancedBean bean = (CandiEnhancedBean) proxy;

      bean.__caucho_destroy(null);
    }
  }
}
