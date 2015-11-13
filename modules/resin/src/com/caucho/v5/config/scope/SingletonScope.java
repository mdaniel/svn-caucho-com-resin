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

package com.caucho.v5.config.scope;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.inject.Singleton;

import com.caucho.v5.config.candi.HandleAware;
import com.caucho.v5.config.candi.SingletonHandle;
import com.caucho.v5.inject.Module;
import com.caucho.v5.loader.Environment;
import com.caucho.v5.loader.EnvironmentClassLoader;

/**
 * The application scope value
 */
@Module
public class SingletonScope extends ScopeContextBase {
  private WeakReference<ClassLoader> _loader;
  private ContextContainer _context = new ContextContainer();

  /**
   * Returns the current application scope
   */
  public SingletonScope()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    _loader = new WeakReference<ClassLoader>(loader);
    
    Environment.addCloseListener(_context);
  }
  
  /**
   * Returns true if the scope is currently active.
   */
  @Override
  public boolean isActive()
  {
    return true;
   }

  /**
   * Returns the scope annotation type.
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    return Singleton.class;
  }

  @Override
  protected ContextContainer getContextContainer()
  {
    return _context;
  }

  @Override
  protected ContextContainer createContextContainer()
  {
    return _context;
  }

  @Override
  protected <T> T create(Contextual<T> bean, 
                         CreationalContext<T> env)
  {
    T instance;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    ClassLoader loader = _loader.get();
    
    try {
      if (loader != null) {
        // server/12bs
        thread.setContextClassLoader(loader);
      }
      
      instance = super.create(bean, env);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    if ((instance instanceof HandleAware) 
        && (bean instanceof PassivationCapable)) {
      HandleAware handleAware = (HandleAware) instance;
      PassivationCapable passiveBean = (PassivationCapable) bean;
      
      handleAware.setSerializationHandle(new SingletonHandle(passiveBean.getId()));
    }
    
    return instance;
  }
  public <T> void addDestructor(Contextual<T> comp, T value)
  {
    EnvironmentClassLoader loader = Environment.getEnvironmentClassLoader();

    if (loader != null) {
      DestructionListener listener
        = (DestructionListener) loader.getAttribute("caucho.destroy");

      if (listener == null) {
        listener = new DestructionListener();
        loader.setAttribute("caucho.destroy", listener);
        loader.addListener(listener);
      }

      listener.addValue(comp, value);
    }
  }
}
