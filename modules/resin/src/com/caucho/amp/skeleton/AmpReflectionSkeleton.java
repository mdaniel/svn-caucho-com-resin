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

package com.caucho.amp.skeleton;

import java.lang.reflect.Method;
import java.util.HashMap;

import com.caucho.amp.actor.AbstractAmpActor;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpHeaders;

/**
 * Creates MPC skeletons and stubs.
 */
class AmpReflectionSkeleton extends AbstractAmpActor
{
  private HashMap<String,Method> _methodMap = new HashMap<String,Method>();
  
  private final String _address;
  private final Object _bean;
  
  AmpReflectionSkeleton(Object bean, String address)
  {
    _address = address;
    _bean = bean;
    
    for (Class<?> api : bean.getClass().getInterfaces()) {
      for (Method method : api.getMethods()) {
        _methodMap.put(method.getName(), method);
      }
    }
  }
  
  @Override
  public String getAddress()
  {
    return _address;
  }

  @Override
  public void send(String to,
                   String from,
                   AmpHeaders headers,
                   AmpEncoder encoder,
                   String methodName,
                   Object ...args)
  {
    invokeMethod(encoder, methodName, args);
  }
  
  private Object invokeMethod(AmpEncoder encoder,
                              String methodName, 
                              Object []args)
  {
    Method method = _methodMap.get(methodName);
    
    if (method == null)
      throw new IllegalStateException("unknown method: " + methodName);
    
    try {
      return method.invoke(_bean, args);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
