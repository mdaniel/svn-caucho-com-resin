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

package com.caucho.iiop.orb;

import java.lang.reflect.*;
import java.util.*;

/**
 * Proxy implementation for ORB clients.
 */
public class IiopProxyHandler implements InvocationHandler {
  private final HashMap<String,MethodMarshal> _methodMap
    = new HashMap<String,MethodMarshal>();

  private ORBImpl _orb;
  private org.omg.CORBA.portable.ObjectImpl _stub;
  private StubMarshal _stubMarshal;

  public IiopProxyHandler(ORBImpl orb,
        org.omg.CORBA.portable.ObjectImpl stub,
        StubMarshal stubMarshal)
  {
    _orb = orb;
    _stub = stub;
    _stubMarshal = stubMarshal;
  }

  public StubImpl getStub()
  {
    return (StubImpl) _stub;
  }

  /**
   * Handles the object invocation.
   *
   * @param proxy the proxy object to invoke
   * @param method the method to call
   * @param args the arguments to the proxy object
   */
  public Object invoke(Object proxy, Method method, Object []args)
    throws Throwable
  {
    String methodName = method.getName();

    MethodMarshal marshal = _stubMarshal.get(method);

    if (marshal != null) {
      return marshal.invoke(_stub, args);
    }

    Class []params = method.getParameterTypes();

    if (methodName.equals("toString") && params.length == 0)
      return "IiopProxy[" + _orb + "," + _stub + "]";

    // XXX: ejb30/bb/session/stateful/equals/annotated, needs QA
    if (methodName.equals("equals") && params.length == 1) {
      IiopProxyHandler otherProxy = (IiopProxyHandler) java.lang.reflect.Proxy.getInvocationHandler(args[0]);

      // XXX: Is the stub enough to be equal ???
      if (_stub.equals(otherProxy.getStub()))
        return true;

      return false;
    }

    throw new RuntimeException("method not found: " + methodName);
  }

  public String toString()
  {
    return "IiopProxyHandler[" + _orb + ", " + _stub + "]";
  }
}
