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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amp.AmpException;
import com.caucho.amp.actor.AbstractAmpActor;
import com.caucho.amp.actor.AmpActorRef;
import com.caucho.amp.actor.AmpMethodRef;
import com.caucho.amp.broker.AmpBroker;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;
import com.caucho.amp.stream.AmpHeaders;
import com.caucho.amp.stream.NullEncoder;

/**
 * Creates MPC skeletons and stubs.
 */
class AmpReflectionSkeleton extends AbstractAmpActor
{
  private static final Logger log
    = Logger.getLogger(AmpReflectionSkeleton.class.getName());
  
  private HashMap<String,Method> _methodMap = new HashMap<String,Method>();
  
  private final String _address;
  private final Object _bean;
  
  AmpReflectionSkeleton(String address,
                        Object bean)
  {
    _address = address;
    _bean = bean;
    
    for (Method method : bean.getClass().getDeclaredMethods()) {
      _methodMap.put(method.getName(), method);
    }
  }

  @Override
  public AmpMethodRef getMethod(String methodName, AmpEncoder encoder)
  {
    Method method = _methodMap.get(methodName);
    
    if (method == null) {
      throw new NullPointerException(methodName);
    }
    
    AmpActorRef to = null;
    
    return new SkeletonMethodRef(to, _bean, method);
  }
  
  @Override
  public void send(AmpActorRef to,
                   AmpActorRef from,
                   AmpEncoder encoder,
                   String methodName,
                   Object ...args)
  {
    try {
      invokeMethod(encoder, methodName, args);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
      
      from.error(from, NullEncoder.ENCODER, new AmpError());
    }
  }

  @Override
  public void query(long id,
                    AmpActorRef to,
                    AmpActorRef from,
                    AmpEncoder encoder,
                    String methodName,
                    Object ...args)
  {
    try {
      Object result = invokeMethod(encoder, methodName, args);
    
      from.reply(id, to, NullEncoder.ENCODER, result);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
      
      from.queryError(id, to, NullEncoder.ENCODER, new AmpError());
    }
  }
  
  private Object invokeMethod(AmpEncoder encoder,
                              String methodName, 
                              Object []args)
    throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
  {
    Method method = _methodMap.get(methodName);
    
    if (method == null)
      throw new IllegalStateException("unknown method: " + methodName);
    
    return method.invoke(_bean, args);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "," + _bean + "]";
  }
  
  static class SkeletonMethodRef implements AmpMethodRef {
    private final AmpActorRef _to;
    private final Object _bean;
    private final Method _method;

    SkeletonMethodRef(AmpActorRef to, Object bean, Method method)
    {
      _to = to;
      _bean = bean;
      _method = method;
    }
    
    @Override
    public void send(AmpActorRef from, Object... args)
    {
      try {
        _method.invoke(_bean, args);
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
        
        from.error(null, NullEncoder.ENCODER, new AmpError());
      }
    }

    @Override
    public void query(long id, AmpActorRef from, Object... args)
    {
      try {
        if (log.isLoggable(Level.FINER)) {
          log.finer(_bean + " " + _method.getName() + " call " + id + " from " + from);
        }
        
        Object result = _method.invoke(_bean, args);
      
        from.reply(id, _to, NullEncoder.ENCODER, result);
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
        
        from.queryError(id, _to, NullEncoder.ENCODER, new AmpError());
      }
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _method.getName() + ",to=" + _to + "]";
    }
  }
}
