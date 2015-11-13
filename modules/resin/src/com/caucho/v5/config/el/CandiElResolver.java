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

package com.caucho.v5.config.el;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.candi.CreationalContextImpl;
import com.caucho.v5.config.candi.OwnerCreationalContext;
import com.caucho.v5.config.candi.ReferenceFactory;
import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.el.ELUtil;

/**
 * Variable resolution for CDI variables
 */
public class CandiElResolver extends ELResolver {
  private static final ThreadLocal<ContextHolder> _envLocal
    = new ThreadLocal<>();
  
  private CandiManager _injectManager;
  
  public CandiElResolver(CandiManager injectManager)
  {
    _injectManager = injectManager;
  }
  
  public CandiElResolver()
  {
    this(CandiManager.create());
  }
  
  protected CandiManager getInjectManager()
  {
    return _injectManager;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context,
                                        Object base)
  {
    return Object.class;
  }

  @Override
  public Iterator<FeatureDescriptor>
    getFeatureDescriptors(ELContext context, Object base)
  {
    ArrayList<FeatureDescriptor> list = new ArrayList<FeatureDescriptor>();

    return list.iterator();
  }

  @Override
  public Class<?> getType(ELContext context,
                          Object base,
                          Object property)
  {
    if (! (property instanceof String) || base != null)
      return null;

    String name = (String) property;
    
    CandiManager manager = getInjectManager();
    
    ReferenceFactory<?> factory = manager.getReferenceFactory(name);
    
    if (factory == null || ! factory.isResolved())
      return null;
    
    return factory.getBean().getBeanClass();
  }

  @Override
  public Object getValue(ELContext context,
                         Object base,
                         Object property)
    throws PropertyNotFoundException,
           ELException
  {
    if (! (property instanceof String) || base != null)
      return null;

    String name = (String) property;
    
    CandiManager manager = getInjectManager();
    
    if (manager == null)
      return null;
    
    ReferenceFactory<?> factory = manager.getReferenceFactory(name);
    
    if (factory == null || ! factory.isResolved())
      return null;
    
    ContextConfig env = ContextConfig.getCurrent();

    ContextHolder holder = _envLocal.get();
    
    CreationalContextImpl<?> cxtCdi = null;
    
    if (holder != null && holder.isActive()) {
      cxtCdi = holder.getEnv();
      
      if (cxtCdi == null) {
        cxtCdi = new OwnerCreationalContext<Object>(null);
        holder.setEnv(cxtCdi);
      }
    }
    
    if (cxtCdi == null && env != null) {
      cxtCdi = (CreationalContextImpl<?>) env.getCreationalContext();
    }

    ELUtil.setPropertyResolved(context, base, property);
    // context.setPropertyResolved(base, property);

    return factory.create(null, cxtCdi, null);
  }

  @Override
  public boolean isReadOnly(ELContext context,
                            Object base,
                            Object property)
    throws PropertyNotFoundException,
           ELException
  {
    return true;
  }

  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
    throws PropertyNotFoundException,
           PropertyNotWritableException,
           ELException
  {
  }
  
  public static final void startContext()
  {
    ContextHolder holder = _envLocal.get();
    
    if (holder == null) {
      holder = new ContextHolder();
      _envLocal.set(holder);
    }
    
    holder.setActive();
  }
  
  public static final void finishContext()
  {
    ContextHolder holder = _envLocal.get();
    
    holder.free();
  }
  
  static class ContextHolder {
    private boolean _isActive;
    private CreationalContextImpl<?> _env;

    void setActive()
    {
      _isActive = true;
    }
    
    boolean isActive()
    {
      return _isActive;
      
    }
    
    CreationalContextImpl<?> getEnv()
    {
      return _env;
    }

    void setEnv(CreationalContextImpl<?> env)
    {
      _env = env;
    }
    
    void free()
    {
      CreationalContextImpl<?> env = _env;
      _env = null;
      _isActive = false;

      if (env != null)
        env.release();
    }
  }
}
