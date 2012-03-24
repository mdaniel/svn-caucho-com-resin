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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

import com.caucho.bam.actor.AbstractActorSender;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.broker.Broker;

/**
 * The Skeleton introspects and dispatches messages for a
 * {@link com.caucho.bam.actor.SimpleActor}
 * or {@link com.caucho.bam.actor.SkeletonActorFilter}.
 */
class BamProxyHandler implements InvocationHandler
{
  private HashMap<Method,Call> _callMap = new HashMap<Method,Call>();
  
  private String _to;
  private ProxySender _sender; 
  
  BamProxyHandler(Class<?> api, String to, Broker broker)
  {
    _to = to;
    String from = "from";
    _sender = new ProxySender(from, broker);
    
    for (Method m : api.getMethods()) {
      if (m.getDeclaringClass() == Object.class) {
        continue;
      }
      
      Call call = null;
      
      if (void.class.equals(m.getReturnType())) {
        call = new MessageCall(m.getName());
      }
      else {
        call = new QueryCall(m.getName());
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
      return call.invoke(_sender, _to, args);
    }
    
    String name = method.getName();
    
    int size = args != null ? args.length : 0;
    
    if ("toString".equals(name) && size == 0) {
      return "BamProxyHandler[" + _to + "]";
    }
    
    return null;
  }
  
  abstract static class Call {
    abstract Object invoke(ActorSender sender, String to, Object []args);
  }
  
  static class QueryCall extends Call {
    private final String _name;
    
    QueryCall(String name)
    {
      _name = name;
    }
    
    @Override
    Object invoke(ActorSender sender, String to, Object []args)
    {
      CallPayload payload = new CallPayload(_name, args);
      
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
    Object invoke(ActorSender sender, String to, Object []args)
    {
      CallPayload payload = new CallPayload(_name, args);
      System.out.println("SEND: " + sender + " " + to);
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
}
