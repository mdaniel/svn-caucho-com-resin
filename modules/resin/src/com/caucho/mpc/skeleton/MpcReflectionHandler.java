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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.mpc.skeleton;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

import com.caucho.mpc.stream.MpcStream;

/**
 * Creates MPC skeletons and stubs.
 */
class MpcReflectionHandler implements InvocationHandler
{
  private Class<?> _api;
  private MpcStream _broker;
  private String _to;
  private String _from;
  private HashMap<String,Method> _methodMap = new HashMap<String,Method>();
  
  MpcReflectionHandler(Class<?> api, 
                       MpcStream broker,
                       String to,
                       String from)
  {
    if (! api.isInterface())
      throw new IllegalArgumentException();
    
    _api = api;
    _broker = broker;
    _to = to;
    _from = from;
    
    for (Method method : api.getMethods()) {
      _methodMap.put(method.getName(), method);
    }
  }

  @Override
  public Object invoke(Object bean,
                       Method method,
                       Object []args)
    throws Throwable
  {
    String methodName = method.getName();
    Method targetMethod = _methodMap.get(methodName);
    
    if (targetMethod != null) {
      _broker.message(_to, _from, methodName, args);
      return null;
    }
    else {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
}
