/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.server.cdi;

import java.util.Hashtable;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.naming.NamingException;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.candi.ObjectFactoryNaming;
import com.caucho.v5.inject.Module;

/**
 * Jndi proxy class for injection.
 */
@Module
public class JndiBeanProxy<T> implements ObjectFactoryNaming {
  private CandiManager _injectManager;
  private Bean<T> _bean;
  
  JndiBeanProxy(CandiManager manager, Bean<T> bean)
  {
    _injectManager = manager;
    _bean = bean;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public Object createObject(Hashtable env) throws NamingException
  {
    CreationalContext<T> cxt = _injectManager.createCreationalContext(_bean);
    
    return _injectManager.getReference(_bean, _bean.getBeanClass(), cxt);
  }
}
