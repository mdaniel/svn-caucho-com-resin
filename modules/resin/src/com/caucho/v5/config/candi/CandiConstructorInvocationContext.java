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
 * @author Alex Rojkov
 */

package com.caucho.v5.config.candi;

import com.caucho.v5.config.gen.InterceptorException;
import com.caucho.v5.util.L10N;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CandiConstructorInvocationContext extends AbstractInvocationContext
{
  private static Logger log
    = Logger.getLogger(CandiConstructorInvocationContext.class.getName());

  private static L10N L = new L10N(CandiConstructorInvocationContext.class);

  private CandiManager _injectManager;
  private CreationalContext<?> _parentCtx;
  private AnnotatedConstructor _annotatedConstructor;
  private Constructor _constructor;
  private Object _target;
  private Map<String,Object> _contextData;
  private Interceptor []_interceptors;
  private int _index = 0;

  private final Interceptor []_chainMethods;
  private final Object []_chainObjects;
  private final int []_chainIndex;

  public CandiConstructorInvocationContext(CandiManager injectManager,
                                           CreationalContext<?> parentCtx,
                                           AnnotatedConstructor<?> annotatedConstructor,
                                           Object []parameters,
                                           Interceptor []chainMethods,
                                           Object []chainObjects,
                                           int []chainIndex)
  {
    super(parameters);

    _injectManager = injectManager;
    _parentCtx = parentCtx;
    _annotatedConstructor = annotatedConstructor;
    _constructor = annotatedConstructor.getJavaMember();

    _chainMethods = chainMethods;
    _chainObjects = chainObjects;
    _chainIndex = chainIndex;
  }

  @Override
  public Constructor<?> getConstructor()
  {
    return _annotatedConstructor.getJavaMember();
  }

  @Override
  public Object getTarget()
  {
    return _target;
  }

  @Override
  public Object getTimer()
  {
    return null;
  }

  @Override
  public Method getMethod()
  {
    return null;
  }

  @Override
  protected Class<?> []getParameterTypes()
  {
    return _annotatedConstructor.getJavaMember().getParameterTypes();
  }

  @Override
  protected Class<?> getDeclaringClass()
  {
    return _annotatedConstructor.getJavaMember().getDeclaringClass();
  }

  @Override
  protected String getName()
  {
    return _annotatedConstructor.getJavaMember().getName();
  }

  @Override
  public Map<String,Object> getContextData()
  {
    if (_contextData == null)
      _contextData = new HashMap<>();

    return _contextData;
  }

  @Override
  public Object proceed() throws Exception
  {
    try {
      if (_index < _chainIndex.length) {
        int i = _index++;
        if (_chainObjects[_chainIndex[i]] == null)
          throw new NullPointerException(i
                                         + " index[i]="
                                         + _chainIndex[i]
                                         + " "
                                         + InterceptionType.AROUND_CONSTRUCT
                                         + " "
                                         + _chainMethods[i]);

        _chainMethods[i].intercept(InterceptionType.AROUND_CONSTRUCT,
                                   _chainObjects[_chainIndex[i]],
                                   this);
      }
      else {
        _target = _constructor.newInstance(getParameters());
      }
    } catch (final InterceptorException e){
      Throwable cause = e.getCause();

      if (cause instanceof RuntimeException)
        throw (RuntimeException) cause;

      throw e;
    }
    catch (final RuntimeException e) {
      throw e;
    } catch (final InvocationTargetException e) {
      Throwable cause = e.getCause();

      if (cause instanceof RuntimeException)
        throw (RuntimeException) cause;

      throw (Exception) cause;
    }

    return null;
  }
}
