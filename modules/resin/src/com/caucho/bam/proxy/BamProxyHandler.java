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
 * @author Emil Ong
 */

package com.caucho.bam.proxy;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

import com.caucho.bam.BamError;
import com.caucho.bam.BamException;
import com.caucho.bam.actor.AbstractActorSender;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.BamActorRef;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.query.QueryCallback;
import com.caucho.bam.query.QueryFutureCallback;
import com.caucho.bam.query.QueryManager;
import com.caucho.bam.stream.MessageStream;
import com.caucho.util.L10N;

/**
 * The Skeleton introspects and dispatches messages for a
 * {@link com.caucho.bam.actor.SimpleActor}
 * or {@link com.caucho.bam.actor.SkeletonActorFilter}.
 */
class BamProxyHandler implements InvocationHandler
{
  private static final L10N L = new L10N(BamProxyHandler.class);
  
  private static final HashMap<Class<?>,Object> _nullResultMap
    = new HashMap<Class<?>,Object>();
  
  private final Class<?> _api;
  private final HashMap<Method,Call> _callMap = new HashMap<Method,Call>();
  
  private final ActorSender _sender;
  private final BamActorRef _to;
  private final long _timeout;
  
  BamProxyHandler(Class<?> api,
                  ActorSender sender,
                  BamActorRef to,
                  long timeout)
  {
    _api = api;
    _sender = sender;
    _to = to;
    _timeout = timeout;
    
    for (Method m : api.getMethods()) {
      if (m.getDeclaringClass() == Object.class) {
        continue;
      }
      
      Call call = null;
      
      Class<?> []param = m.getParameterTypes();
      
      Object nullResult = _nullResultMap.get(m.getReturnType());
      
      if (param.length > 0
          && ReplyCallback.class.isAssignableFrom(param[param.length - 1])) {
        call = new ReplyCallbackCall(m.getName(), 
                                     param.length - 1,
                                     nullResult);
      }
      else if (param.length > 0
            && QueryCallback.class.isAssignableFrom(param[param.length - 1])) {
        call = new QueryCallbackCall(m.getName(), param.length - 1);
      }
      else if (void.class.equals(m.getReturnType())) {
        call = new MessageCall(m.getName());
      }
      else {
        call = new QueryCall(m.getName(), nullResult);
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
      return call.invoke(_sender, _to, args, _timeout);
    }
    
    String name = method.getName();
    
    int size = args != null ? args.length : 0;
    
    if ("toString".equals(name) && size == 0) {
      return "BamProxyHandler[" + _to + "," + _api.getSimpleName() + "]";
    }
    
    return null;
  }
  
  abstract static class Call {
    abstract Object invoke(ActorSender sender,
                           BamActorRef to,
                           Object []args,
                           long timeout);
  }
  
  static class QueryCall extends Call {
    private final String _name;
    private final Object _nullResult;
    
    QueryCall(String name, Object nullResult)
    {
      _name = name;
      _nullResult = nullResult;
    }
    
    @Override
    Object invoke(ActorSender sender,
                  BamActorRef to,
                  Object []args,
                  long timeout)
    {
      CallPayload payload = new CallPayload(_name, args);
      
      QueryFutureCallback cb = new QueryFutureCallback();
      
      sender.query(to, payload, cb);
      
      Object result = cb.get(timeout);

      if (result instanceof ReplyPayload) {
        return ((ReplyPayload) result).getValue();
      }
      else if (result == null) {
        return _nullResult;
      }
      else
        throw new IllegalStateException(L.l("'{0}' is an unexpected bam proxy result class.",
                                            result.getClass().getName()));
    }
  }
  
  static class ReplyCallbackCall extends Call {
    private final String _name;
    private final int _paramLen;
    private final Object _nullResult;
    
    ReplyCallbackCall(String name, int paramLen, Object nullResult)
    {
      _name = name;
      _paramLen = paramLen;
      _nullResult = nullResult;
    }
    
    @Override
    Object invoke(ActorSender sender,
                  BamActorRef to,
                  Object []args,
                  long timeout)
    {
      Object []param = new Object[args.length - 1];
      System.arraycopy(args, 0, param, 0, param.length);
      
      ReplyCallback cb = (ReplyCallback) args[_paramLen];
      
      CallPayload payload = new CallPayload(_name, param);
      
      CallReplyCallback callCb = new CallReplyCallback(cb);

      sender.query(to, payload, callCb);
      
      return _nullResult;
    }
  }
  
  static class QueryCallbackCall extends Call {
    private final String _name;
    private final int _paramLen;
    
    QueryCallbackCall(String name, int paramLen)
    {
      _name = name;
      _paramLen = paramLen;
    }
    
    @Override
    Object invoke(ActorSender sender,
                  BamActorRef to, 
                  Object []args,
                  long timeout)
    {
      Object []param = new Object[args.length - 1];
      System.arraycopy(args, 0, param, 0, param.length);
      
      QueryCallback cb = (QueryCallback) args[_paramLen];
      
      CallPayload payload = new CallPayload(_name, param);

      sender.query(to, payload, cb);
      
      return null;
    }
  }

  static class MessageCall extends Call {
    private final String _name;
    
    MessageCall(String name)
    {
      _name = name;
    }

    @Override
    Object invoke(ActorSender sender,
                  BamActorRef to,
                  Object []args,
                  long timeout)
    {
      CallPayload payload = new CallPayload(_name, args);

      sender.message(to, payload);
      
      return null;
    }
  }

  static class ProxySender extends AbstractActorSender {
    private String _address;
    private Broker _broker;

    ProxySender(String address, Broker broker)
    {
      _address = address;
      _broker = broker;
    }
    
    @Override
    public String getAddress()
    {
      return _address;
    }

    @Override
    public Broker getBroker()
    {
      return _broker;
    }
  }
  
  static class CallReplyCallback implements QueryCallback {
    private final ReplyCallback _delegate;
    
    CallReplyCallback(ReplyCallback delegate)
    {
      _delegate = delegate;
    }

    @Override
    public void onQueryResult(String to, String from, Serializable payload)
    {
      if (payload == null) {
        _delegate.onReply(null);
      }
      else if (payload instanceof ReplyPayload) {
        ReplyPayload reply = (ReplyPayload) payload;
        
        _delegate.onReply(reply.getValue());
      }
      else {
        _delegate.onReply(payload);
      }

    }

    @Override
    public void onQueryError(String to, 
                             String from, 
                             Serializable payload,
                             BamError error)
    {
      _delegate.onError(error);
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _delegate + "]";
    }
  }
  
  static {
    _nullResultMap.put(boolean.class, Boolean.FALSE); 
    _nullResultMap.put(byte.class, new Byte((byte) 0));
    _nullResultMap.put(char.class, new Character((char) 0));
    _nullResultMap.put(short.class, new Short((short) 0));
    _nullResultMap.put(int.class, new Integer(0));
    _nullResultMap.put(long.class, new Long(0));
    _nullResultMap.put(float.class, new Float(0));
    _nullResultMap.put(double.class, new Double(0));
  }
}
