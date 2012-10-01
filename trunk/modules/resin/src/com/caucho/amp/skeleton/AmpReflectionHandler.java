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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

import com.caucho.amp.AmpQueryCallback;
import com.caucho.amp.actor.ActorContextImpl;
import com.caucho.amp.actor.AmpActorContext;
import com.caucho.amp.actor.AmpActorRef;
import com.caucho.amp.actor.AmpMethodRef;
import com.caucho.amp.actor.AmpQueryFuture;
import com.caucho.amp.broker.AmpBroker;
import com.caucho.amp.stream.NullEncoder;

/**
 * Creates AMP skeletons and stubs.
 */
class AmpReflectionHandler implements InvocationHandler
{
  private HashMap<Method,Call> _callMap = new HashMap<Method,Call>();
  
  private AmpActorRef _to;
  private AmpActorContext _systemContext;
  private long _timeout = 60000; 
    
  AmpReflectionHandler(Class<?> api, 
                       AmpActorRef to,
                       AmpActorContext systemContext)
  {
    _to = to;
    _systemContext = systemContext;
    
    if (systemContext == null) {
      throw new NullPointerException();
    }
      
    for (Method m : api.getMethods()) {
      if (m.getDeclaringClass() == Object.class) {
        continue;
      }
      
      AmpMethodRef methodRef = _to.getMethod(m.getName(), NullEncoder.ENCODER);
      
      Call call = null;
      
      Class<?> []param = m.getParameterTypes();
      
      if (param.length > 0
          && AmpQueryCallback.class.isAssignableFrom(param[param.length - 1])) {
        call = new QueryCallbackCall(methodRef, param.length - 1);
      }
      else if (void.class.equals(m.getReturnType())) {
        call = new MessageCall(methodRef);
      }
      else {
        call = new QueryCall(methodRef);
      }
      
      _callMap.put(m, call);
    }
  }
  
  @Override
  public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable
  {
    Call call = _callMap.get(method);

    if (call != null) {
      return call.invoke(_systemContext, _to, args, _timeout);
    }
    
    String name = method.getName();
    
    int size = args != null ? args.length : 0;
    
    if ("toString".equals(name) && size == 0) {
      return "BamProxyHandler[" + _to + "]";
    }
    
    return null;
  }
  
  abstract static class Call {
    abstract Object invoke(AmpActorContext systemContext,
                           AmpActorRef to,
                           Object []args,
                           long timeout);
  }
  
  static class QueryCall extends Call {
    private final AmpMethodRef _methodRef;
    
    QueryCall(AmpMethodRef methodRef)
    {
      _methodRef = methodRef;
    }
    
    @Override
    Object invoke(AmpActorContext systemContext,
                  AmpActorRef to, 
                  Object []args,
                  long timeout)
    {
      AmpQueryFuture future = new AmpQueryFuture(timeout);

      AmpActorContext context = AmpActorContext.getCurrent(systemContext);
      
      context.query(_methodRef, args, future, timeout);
      
      return future.get();
    }
  }
  
  static class QueryCallbackCall extends Call {
    private final AmpMethodRef _methodRef;
    private final int _paramLen;
    
    QueryCallbackCall(AmpMethodRef methodRef, int paramLen)
    {
      _methodRef = methodRef;
      _paramLen = paramLen;
    }
    
    @Override
    Object invoke(AmpActorContext systemContext,
                  AmpActorRef to, 
                  Object []args,
                  long timeout)
    {
      Object []param = new Object[args.length - 1];
      System.arraycopy(args, 0, param, 0, param.length);
      
      AmpQueryCallback cb = (AmpQueryCallback) args[_paramLen];
      
      AmpActorContext context = AmpActorContext.getCurrent(systemContext);
      
      context.query(_methodRef, args, cb, timeout);
      
      return null;
    }
  }

  static class MessageCall extends Call {
    private final AmpMethodRef _methodRef;
    
    MessageCall(AmpMethodRef methodRef)
    {
      _methodRef = methodRef;
    }

    @Override
    Object invoke(AmpActorContext systemContext,
                  AmpActorRef to,
                  Object []args,
                  long timeout)
    {
      AmpActorContext context = AmpActorContext.getCurrent(systemContext);
      
      context.send(_methodRef, args);
      
      return null;
    }
  }
}
