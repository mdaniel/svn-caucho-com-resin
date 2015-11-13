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

package com.caucho.v5.config.gen;

import java.io.IOException;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.inject.Module;
import com.caucho.v5.java.JavaWriter;

/**
 * Represents a stateless local business method
 */
@Module
public class AspectBeanFactoryLifecycle<X> implements AspectBeanFactory<X>
{
  private AspectBeanFactory<X> _next;
  private CandiManager _manager;
  private AnnotatedType<X> _beanType;
  private AspectFactory<X> _factory;
  
  public AspectBeanFactoryLifecycle(AspectBeanFactory<X> next,
                                    CandiManager manager,
                                    AnnotatedType<X> beanType)
                                    
  {
    _next = next;
    _manager = manager;
    _beanType = beanType;
    
    _factory = createAspectFactory();
  }
  
  protected CandiManager getManager()
  {
    return _manager;
  }
  
  @Override
  public boolean isProxy()
  {
    return _next.isProxy();
  }

  @Override
  public AnnotatedType<X> getBeanType()
  {
    return _beanType;
  }
  

  @Override
  public String getBeanInstance()
  {
    return _next.getBeanInstance();
  }

  @Override
  public String getInterceptorInstance()
  {
    return _next.getInterceptorInstance();
  }
  
  /**
   * Generates the proxy object.
   */
  @Override
  public String getBeanProxy()
  {
    return _next.getBeanProxy();
  }
  
  @Override
  public String getBeanSuper()
  {
    return _next.getBeanSuper();
  }
  
  /**
   * Generates data associated with the bean
   */
  @Override
  public String getBeanInfo()
  {
    return _next.getBeanInfo();
  }
  
  /**
   * Returns the generated bean name
   */
  @Override
  public String getGeneratedClassName()
  {
    return _next.getGeneratedClassName();
  }
  
  /**
   * Returns the generated bean name
   */
  @Override
  public String getInstanceClassName()
  {
    return _next.getInstanceClassName();
  }

  @Override
  public AspectFactory<X> getHeadAspectFactory()
  {
    return _factory;
  }

  @Override
  public boolean isEnhanced()
  {
    return _factory.isEnhanced();
  }
  
  protected AspectFactory<X> createAspectFactory()
  {
    CandiManager manager = _manager;
    
    AspectFactory<X> next = new LifecycleMethodTailFactory<X>(this);
    
    next = new InterceptorFactory<X>(this, next, manager);
    
    return new LifecycleMethodHeadFactory<X>(this, next);
  }

  @Override
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method)
  {
    return _factory.create(method, true);
  }

  @Override
  public void generateEpilogue(JavaWriter out, HashMap<String, Object> map)
      throws IOException
  {
    _factory.generateEpilogue(out, map);
  }

  @Override
  public void generateInject(JavaWriter out, HashMap<String, Object> map)
      throws IOException
  {
    _factory.generateInject(out, map);
  }

  @Override
  public void generatePostConstruct(JavaWriter out,
                                    HashMap<String, Object> map)
      throws IOException
  {
  }

  @Override
  public void generatePreDestroy(JavaWriter out, HashMap<String, Object> hashMap)
      throws IOException
  {
  }
}
