/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.util;

import com.caucho.config.manager.InjectManager;
import com.caucho.util.L10N;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import javax.inject.manager.Decorator;
import javax.inject.manager.Interceptor;
import javax.inject.manager.InterceptionType;

/**
 * Utilities
 */
public class EjbUtil {
  private static final L10N L = new L10N(EjbUtil.class);
  
  public static final Object []NULL_OBJECT_ARRAY = new Object[0];
  
  private EjbUtil()
  {
  }

  public static int []createInterceptors(InjectManager manager,
					 ArrayList<Interceptor> beans,
					 InterceptionType type,
					 Annotation ...bindings)
  {
    List<Interceptor> interceptors;

    if (bindings != null && bindings.length > 0) {
      interceptors = manager.resolveInterceptors(type, bindings);
    }
    else
      interceptors = new ArrayList<Interceptor>();

    int []indexList = new int[interceptors.size()];

    for (int i = 0; i < interceptors.size(); i++) {
      Interceptor interceptor = interceptors.get(i);

      int index = beans.indexOf(interceptor);
      if (index >= 0)
	indexList[i] = index;
      else {
	indexList[i] = beans.size();
	beans.add(interceptor);
      }
    }

    return indexList;
  }

  public static Method []createMethods(ArrayList<Interceptor> beans,
				       InterceptionType type,
				       int []indexChain)
  {
    Method []methods = new Method[indexChain.length];

    for (int i = 0; i < indexChain.length; i++) {
      int index = indexChain[i];
      
      Method method = beans.get(index).getMethod(type);

      if (method == null)
	throw new IllegalStateException(L.l("'{0}' is an unknown interception method in '{1}'",
					   type, beans.get(index)));

      methods[i] = method;
    }
    
    return methods;
  }

  public static Method getMethod(Class cl,
				 String methodName,
				 Class paramTypes[])
    throws Exception
  {
    Method method = null;
    Exception firstException = null;

    do {
      try {
	method = cl.getDeclaredMethod(methodName, paramTypes);
      } catch (Exception e) {
	if (firstException == null)
	  firstException = e;

	cl = cl.getSuperclass();
      }
    } while (method == null && cl != null);

    if (method == null)
      throw firstException;

    method.setAccessible(true);
    
    return method;
  }

  public static Object generateDelegate(List<Decorator> beans,
					Object tail)
  {
    InjectManager webBeans = InjectManager.create();

    for (int i = beans.size() - 1; i >= 0; i--) {
      Decorator bean = beans.get(i);

      Object instance = webBeans.getInstance(bean);

      bean.setDelegate(instance, tail);

      tail = instance;
    }
    
    return tail;
  }

  public static Object []generateProxyDelegate(InjectManager webBeans,
					       List<Decorator> beans,
					       Object proxy)
  {
    Object []instances = new Object[beans.size()];

    for (int i = 0; i < beans.size(); i++) {
      Decorator bean = beans.get(i);

      Object instance = webBeans.getInstance(bean);

      bean.setDelegate(instance, proxy);

      instances[beans.size() - 1 - i] = instance;
    }
    
    return instances;
  }

  public static int nextDelegate(Object []beans,
				 Class api,
				 int index)
  {
    for (index--; index >= 0; index--) {
      if (api.isAssignableFrom(beans[index].getClass()))
	return index;
    }

    return index;
  }

  public static int nextDelegate(Object []beans,
				 Class []apis,
				 int index)
  {
    for (index--; index >= 0; index--) {
      for (Class api : apis) {
	if (api.isAssignableFrom(beans[index].getClass()))
	  return index;
      }
    }

    return index;
  }
}
