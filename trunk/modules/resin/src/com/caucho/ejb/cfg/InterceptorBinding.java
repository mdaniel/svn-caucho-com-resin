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
 *
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Rodrigo Westrupp
 */

package com.caucho.ejb.cfg;

import java.util.ArrayList;

import javax.enterprise.inject.spi.AnnotatedMethod;

/**
 * Configuration for interceptor-binding.
 */
public class InterceptorBinding {
  private String _ejbName;

  private boolean _isExcludeDefaultInterceptors;
  private boolean _isExcludeClassInterceptors;

  private InterceptorOrder _interceptorOrder;

  private ArrayList<Class<?>> _interceptors = new ArrayList<Class<?>>();
  private ArrayList<EjbMethod> _methodList = new ArrayList<EjbMethod>();

  public InterceptorBinding()
  {
  }

  public String getEjbName()
  {
    return _ejbName;
  }

  public InterceptorOrder getInterceptorOrder()
  {
    return _interceptorOrder;
  }

  public ArrayList<Class<?>> getInterceptors()
  {
    return _interceptors;
  }

  public boolean isExcludeDefaultInterceptors()
  {
    return _isExcludeDefaultInterceptors;
  }

  public boolean isExcludeClassInterceptors()
  {
    return _isExcludeClassInterceptors;
  }

  public void setEjbName(String ejbName)
  {
    _ejbName = ejbName;
  }
  
  public boolean isDefault()
  {
    return _ejbName == null || "*".equals(_ejbName); 
  }

  public void setExcludeDefaultInterceptors(boolean b)
  {
    _isExcludeDefaultInterceptors = b;
  }
  
  public void setExcludeClassInterceptors(boolean value)
  {
    _isExcludeClassInterceptors = value;
  }

  public void setInterceptorOrder(InterceptorOrder interceptorOrder)
  {
    _interceptorOrder = interceptorOrder;
    
    _interceptors.addAll(interceptorOrder.getInterceptorClasses());
  }

  public void addInterceptorClass(Class<?> interceptorClass)
  {
    _interceptors.add(interceptorClass);
  }
  
  public ArrayList<EjbMethod> getMethodList()
  {
    return _methodList;
  }
  
  public InterceptorsLiteral getAnnotation()
  {
    Class<?> []values = new Class<?>[_interceptors.size()];
    
    _interceptors.toArray(values);
    
    return new InterceptorsLiteral(values);
  }
  
  public InterceptorsLiteral mergeAnnotation(AnnotatedMethod<?> m)
  {
    javax.interceptor.Interceptors interceptors;
    
    interceptors = m.getAnnotation(javax.interceptor.Interceptors.class);
    
    ArrayList<Class<?>> classList = new ArrayList<Class<?>>(_interceptors);
    
    if (interceptors != null) {
      for (Class<?> cl : interceptors.value())
        classList.add(cl);
    }
    
    Class<?> []values = new Class<?>[classList.size()];
    
    classList.toArray(values);
    
    return new InterceptorsLiteral(values);
  }
  
  public boolean isMatch(AnnotatedMethod<?> method)
  {
    for (EjbMethod ejbMethod : _methodList) {
      if (ejbMethod.isMatch(method))
        return true;
    }
    
    return false;
  }
  
  public void addMethod(EjbMethod method)
  {
    _methodList.add(method);
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _ejbName + ", " + _methodList
           + " " + _interceptors + "]");
  }
}
