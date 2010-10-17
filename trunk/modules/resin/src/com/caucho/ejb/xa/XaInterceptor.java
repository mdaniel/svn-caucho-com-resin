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

package com.caucho.ejb.xa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.transaction.Transaction;

import com.caucho.config.program.ConfigProgram;
import com.caucho.ejb.util.XAManager;

/**
 * Server container for a session bean.
 */
abstract public class XaInterceptor extends ConfigProgram {
  private static final Object []NULL_ARGS = new Object[0];
  
  private static final XAManager _xa = new XAManager();

  private Method _javaMethod;
  
  protected XaInterceptor(AnnotatedMethod<?> method)
  {
    _javaMethod = method.getJavaMember();
  }
  
  public static ConfigProgram create(AnnotatedMethod<?> method)
  {
    TransactionAttribute xaAttr 
      = method.getAnnotation(TransactionAttribute.class);
    
    TransactionAttribute xaClassAttr 
      = method.getDeclaringType().getAnnotation(TransactionAttribute.class);
    
    TransactionAttributeType xaType = null;
    
    if (xaClassAttr != null)
      xaType = xaClassAttr.value();
    
    if (xaAttr != null)
      xaType = xaAttr.value();
    
    if (xaType == null)
      return null;
    
    switch (xaType) {
    case REQUIRED:
      return new RequiresInterceptor(method);
      
    default:
      return null;
    }
  }
  
  protected void invokeMethod(Object instance, Object []args)
  {
    try {
      _javaMethod.invoke(instance, args);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(_javaMethod.getName() + ": " + e, e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(_javaMethod.getName() + ": " + e, e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(_javaMethod.getName() + ": " + e.getCause(),
                                 e.getCause());
    }
  }
  
  static class RequiresInterceptor extends XaInterceptor {
    RequiresInterceptor(AnnotatedMethod<?> method)
    {
      super(method);
    }
    
    @Override
    public <T> void inject(T bean, CreationalContext<T> createContext)
    {
      Transaction xa = null;
      boolean isXAValid = false;

      try {
        xa = _xa.beginRequired();
        
        invokeMethod(bean, NULL_ARGS);
        
        isXAValid = true;
      } catch (RuntimeException e) {
        isXAValid = true;
        
        if (_xa.systemException(e)) {
          _xa.rethrowEjbException(e, xa != null);
        }
      } finally {
        if (! isXAValid)
          _xa.markRollback();
        
        if (xa == null)
          _xa.commit();
      }
    }

  }
}
