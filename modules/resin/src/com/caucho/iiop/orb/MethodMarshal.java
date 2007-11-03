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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop.orb;

import java.util.ArrayList;
import java.lang.reflect.Method;

import com.caucho.iiop.*;
import com.caucho.iiop.orb.EjbSessionObjectMarshal;
import com.caucho.iiop.marshal.AnyMarshal;
import com.caucho.iiop.marshal.Marshal;
import com.caucho.iiop.RemoteUserException;

/**
 * Proxy implementation for ORB clients.
 */
public class MethodMarshal {
  private String _name;
  private String _overloadName;
  private Marshal []_args;
  private Marshal _ret;

  private Class []_exceptionTypes;
  private Method _method;

  MethodMarshal(MarshalFactory factory, Method method, Class cl)
  {
    _method = method;
    _name = method.getName();

    if (isOverload(method, cl))
      _overloadName = IiopSkeleton.mangle(method);
    else
      _overloadName = _name;

    Class []params = method.getParameterTypes();

    boolean isIdl = false;

    _args = new Marshal[params.length];

    for (int i = 0; i < params.length; i++)
      _args[i] = factory.create(params[i], isIdl);

    _ret = factory.create(method.getReturnType(), isIdl);

    ArrayList<Class> exnList = new ArrayList<Class>();

    for (Class exn : method.getExceptionTypes()) {
      if (RuntimeException.class.isAssignableFrom(exn))
        continue;
      if (Error.class.isAssignableFrom(exn))
        continue;

      exnList.add(exn);
    }

    _exceptionTypes = new Class[exnList.size()];
    exnList.toArray(_exceptionTypes);
  }

  private boolean isOverload(Method method, Class cl)
  {
    for (Method declMethod : cl.getMethods()) {
      if (method.getName().equals(declMethod.getName())
          && ! method.equals(declMethod))
        return true;
    }

    return false;
  }

  public Object invoke(org.omg.CORBA.portable.ObjectImpl obj,
                       Object []args)
    throws Throwable
  {
    org.omg.CORBA_2_3.portable.InputStream is = null;

    try {
      org.omg.CORBA_2_3.portable.OutputStream os
        = ((org.omg.CORBA_2_3.portable.OutputStream) obj._request(_overloadName, true));

      // ejb/1331
      if (_args.length > 0)
        ((IiopWriter) os).alignMethodArgs();

      for (int i = 0; i < _args.length; i++) {
        // XXX TCK: ejb30/bb/session/stateful/sessioncontext/annotated/passBusinessObjectRemote1
        if (_args[i] instanceof AnyMarshal) {
          Class type = null;

          if (args[i] != null) {
            type = args[i].getClass();

            _args[i] = new com.caucho.iiop.marshal.RemoteMarshal(type);
          }
        }

        _args[i].marshal(os, args[i]);
      }

      is = ((org.omg.CORBA_2_3.portable.InputStream) obj._invoke(os));

      // XXX TCK: ejb30/bb/session/stateful/sessioncontext/annotated/getBusinessObjectRemote1
      // See also: SkeletonMethod.service()
      if (_ret instanceof AnyMarshal) {
        // XXX TCK: ejb30/bb/session/stateless/callback/defaultinterceptor/descriptor/defaultInterceptorsForCallbackBean1
        if (! _method.getReturnType().getName().startsWith("java."))
          _ret = new EjbSessionObjectMarshal(_method.getReturnType(), null);
      }

      return _ret.unmarshal(is);
    } catch (RemoteUserException e) {
      // unwrap remote exceptions

      Throwable cause = e.getCause();

      if (cause instanceof RuntimeException)
        throw cause;

      for (int i = 0; i < _exceptionTypes.length; i++) {
        if (_exceptionTypes[i].isAssignableFrom(cause.getClass()))
          throw cause;
      }

      throw e;
    } finally {
      if (is != null)
        is.close();
    }
  }
}
