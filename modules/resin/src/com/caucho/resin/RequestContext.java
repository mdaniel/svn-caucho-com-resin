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

package com.caucho.resin;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.SchemaBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.loader.CompilingLoader;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.Bean;

/**
 * Request context for an embedded Resin request.
 */
public class RequestContext
{
  private ClassLoader _oldClassLoader;
  private RequestContext _oldContext;
  private ResinBeanContainer _cdiContainer;
  
  private HashMap<Contextual<?>,Object> _requestScope
    = new HashMap<Contextual<?>,Object>(8);

  RequestContext(ResinBeanContainer cdiContainer,
                 ClassLoader oldClassLoader,
                 RequestContext oldContext)
  {
    _cdiContainer = cdiContainer;
    _oldClassLoader = oldClassLoader;
    _oldContext = oldContext;
  }
  
  ClassLoader getOldClassLoader()
  {
    return _oldClassLoader;
  }
  
  RequestContext getOldContext()
  {
    return _oldContext;
  }
  
  @SuppressWarnings("unchecked")
  <T> T get(Contextual<T> bean)
  {
    return (T) _requestScope.get(bean);
  }
  
  @SuppressWarnings("unchecked")
  <T> T get(Contextual<T> bean,
            CreationalContext<T> env,
            InjectManager manager)
  {
    Object value = _requestScope.get(bean);
    
    if (value != null)
      return (T) value;
    
    value = bean.create(env);
    
    _requestScope.put(bean, value);
    
    return (T) value;
  }
  
  public void close()
  {
    ResinBeanContainer cdiContainer = _cdiContainer;
    _cdiContainer = null;
    
    if (cdiContainer != null)
      cdiContainer.completeRequest(this);
  }
}
