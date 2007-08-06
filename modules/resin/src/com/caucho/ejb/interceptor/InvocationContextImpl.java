/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.interceptor;

import com.caucho.bytecode.JClass;
import com.caucho.util.L10N;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * InvocationContext implementation.
 */
public class InvocationContextImpl implements InvocationContext {
  private static final L10N L = new L10N(InvocationContextImpl.class);

  private Object _target;
  private Object _home;
  private Object _parameters[];

  private Method _method;
  private String _methodName;
  private Class _parameterTypes[];

  private HashMap<String, Object> _contextData;

  public InvocationContextImpl(Object target, Object home, String methodName, Class parameterTypes[])
  {
    _target = target;
    _home = home;
    _methodName = methodName;
    _parameterTypes = parameterTypes;
  }

  public Object getHome()
  {
    return _home;
  }

  public Object getTarget()
  {
    return _target;
  }

  public Method getMethod()
  {
    if (_method == null) {
      Class cl = _target.getClass();

      try {
        _method = cl.getMethod(_methodName, _parameterTypes);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }

    return _method;
  }

  public Object[] getParameters()
    throws IllegalStateException
  {
    return _parameters;
  }

  public void setParameters(Object[] parameters)
    throws IllegalStateException
  {
    _parameters = parameters;
  }

  public Map<String, Object> getContextData()
  {
    if (_contextData == null) {
      _contextData = new HashMap<String, Object>();
    }

    return _contextData;
  }

  public Object proceed()
    throws Exception
  {
    return null;
  }
}
