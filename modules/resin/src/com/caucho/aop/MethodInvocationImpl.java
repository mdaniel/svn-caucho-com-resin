/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.aop;

import java.lang.reflect.Method;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;

import org.aopalliance.intercept.MethodInvocation;

import com.caucho.util.L10N;

/**
 * Basic method generation.
 */
abstract public class MethodInvocationImpl implements MethodInvocation {
  private static final L10N L = new L10N(MethodInvocationImpl.class);

  private Object _this;
  private Object []_arguments;
  
  /**
   * Creates the invocation.
   */
  public MethodInvocationImpl(Object obj, Object []arguments)
  {
    _this = obj;
    _arguments = arguments;
  }

  /**
   * Returns the method.
   */
  abstract public Method getMethod();

  /**
   * Returns the static part.
   */
  public AccessibleObject getStaticPart()
  {
    return getMethod();
  }

  /**
   * Returns the current object.
   */
  public Object getThis()
  {
    return _this;
  }

  /**
   * Returns the arguments as an array.
   */
  public Object []getArguments()
  {
    return _arguments;
  }

  /**
   * Proceed to the next interceptor.
   */
  public Object proceed()
    throws Throwable
  {
    try {
      return getSuperMethod().invoke(getThis(), _arguments);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  /**
   * Returns the super method.
   */
  abstract protected Method getSuperMethod();

  /**
   * Returns the matching method.
   */
  protected static Method getMethod(Class cl,
				    String name,
				    Class []types)
  {
    if (cl == null)
      return null;

    loop:
    for (Method method : cl.getDeclaredMethods()) {
      if (! name.equals(method.getName()))
	continue;

      Class []declaredTypes = method.getParameterTypes();
      if (types.length != declaredTypes.length)
	continue;

      for (int i = 0; i < types.length; i++) {
	if (! types[i].equals(declaredTypes[i]))
	  continue loop;
      }

      method.setAccessible(true);

      return method;
    }

    for (Class iface : cl.getInterfaces()) {
      Method method = getMethod(iface, name, types);

      if (method != null)
	return method;
    }
    
    return getMethod(cl.getSuperclass(), name, types);
  }
}
