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

package com.caucho.ejb.gen;

import com.caucho.ejb.cfg.*;
import com.caucho.util.L10N;
import com.caucho.java.*;
import com.caucho.java.gen.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import javax.interceptor.*;
import javax.ejb.*;

/**
 * Generates the skeleton for a bean.
 */
abstract public class BeanGenerator extends GenClass {
  private static final L10N L = new L10N(BeanGenerator.class);

  protected final ApiClass _ejbClass;
  
  private Method _aroundInvokeMethod;

  private ArrayList<Class> _defaultInterceptors
    = new ArrayList<Class>();

  protected BeanGenerator(String fullClassName, ApiClass ejbClass)
  {
    super(fullClassName);
    
    _ejbClass = ejbClass;
  }

  protected ApiClass getEjbClass()
  {
    return _ejbClass;
  }
  
  /**
   * Sets the remote name
   */
  public void setRemoteHome(ApiClass homeClass)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
 
  /**
   * Sets the local name
   */
  public void setLocalHome(ApiClass homeClass)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds a remote
   */
  public void addRemote(ApiClass remoteApi)
  {
  }

  /**
   * Adds a local
   */
  public void addLocal(ApiClass localApi)
  {
  }

  /**
   * Introspects the bean.
   */
  public void introspect()
  {
    _aroundInvokeMethod = findAroundInvokeMethod(_ejbClass.getJavaClass());
  }

  private static Method findAroundInvokeMethod(Class cl)
  {
    if (cl == null)
      return null;

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class)
	  && method.getParameterTypes().length == 1
	  && method.getParameterTypes()[0].equals(InvocationContext.class)) {
	return method;
      }
    }

    return findAroundInvokeMethod(cl.getSuperclass());
  }

  /**
   * Returns the around-invoke method
   */
  public Method getAroundInvokeMethod()
  {
    return _aroundInvokeMethod;
  }

  /**
   * Sets the around-invoke method
   */
  public void setAroundInvokeMethod(Method method)
  {
    _aroundInvokeMethod = method;
  }

  /**
   * Adds a default interceptor
   */
  public void addInterceptor(Class cl)
  {
    _defaultInterceptors.add(cl);
  }

  /**
   * Gets the default interceptor
   */
  public ArrayList<Class> getDefaultInterceptors()
  {
    return _defaultInterceptors;
  }

  /**
   * Returns the views.
   */
  public ArrayList<View> getViews()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates the views for the bean
   */
  public void createViews()
  {
  }

  /**
   * Generates the view contents
   */
  public void generateViews(JavaWriter out)
    throws IOException
  {
    for (View view : getViews()) {
      out.println();

      view.generate(out);
    }
  }

  /**
   * Generates the view contents
   */
  public void generateDestroyViews(JavaWriter out)
    throws IOException
  {
    for (View view : getViews()) {
      out.println();

      view.generateDestroy(out);
    }
  }

  /**
   * Returns true if the method is implemented.
   */
  protected boolean hasMethod(String methodName, Class []paramTypes)
  {
    return _ejbClass.hasMethod(methodName, paramTypes);
  }

  private String generateTypeCasting(String value, Class cl, boolean isEscapeString)
  {
    if (cl.equals(String.class)) {
      if (isEscapeString)
        value = "\"" + value + "\"";
    } else if (cl.equals(Character.class))
      value = "'" + value + "'";
    else if (cl.equals(Byte.class))
      value = "(byte) " + value;
    else if (cl.equals(Short.class))
      value = "(short) " + value;
    else if (cl.equals(Integer.class))
      value = "(int) " + value;
    else if (cl.equals(Long.class))
      value = "(long) " + value;
    else if (cl.equals(Float.class))
      value = "(float) " + value;
    else if (cl.equals(Double.class))
      value = "(double) " + value;

    return value;
  }
}
