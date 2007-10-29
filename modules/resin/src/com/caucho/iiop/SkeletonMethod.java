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

package com.caucho.iiop;

import com.caucho.iiop.marshal.AnyMarshal;
import com.caucho.iiop.marshal.Marshal;
import com.caucho.iiop.orb.EjbSessionObjectMarshal;
import com.caucho.iiop.orb.MarshalFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.rmi.RemoteException;

public class SkeletonMethod {
  private static final Logger log
    = Logger.getLogger(SkeletonMethod.class.getName());

  private IiopSkeleton _skeleton;
  private Method _method;

  private Marshal []_marshalArgs;
  private Marshal _marshalReturn;

  private Class []_exnTypes;

  SkeletonMethod(IiopSkeleton skeleton, Method method, boolean isJava)
  {
    _skeleton = skeleton;
    _method = method;

    MarshalFactory factory = MarshalFactory.create();

    // System.out.println("M: " + method);

    Class []paramTypes = method.getParameterTypes();

    _marshalArgs = new Marshal[paramTypes.length];

    for (int i = 0; i < _marshalArgs.length; i++)
      _marshalArgs[i] = factory.create(paramTypes[i], ! isJava);

    _marshalReturn = factory.create(method.getReturnType(), ! isJava);

    _exnTypes = method.getExceptionTypes();
  }

  void service(Object object, IiopReader reader, IiopWriter writer)
    throws Throwable
  {
    Object []args = new Object[_marshalArgs.length];

    // ejb/1231
    if (args.length > 0)
      reader.alignMethodArgs();

    Class paramTypes[] = _method.getParameterTypes();

    for (int i = 0; i < args.length; i++) {
      if (_marshalArgs[i] instanceof AnyMarshal) {
        _marshalArgs[i] = new com.caucho.iiop.marshal.RemoteMarshal(paramTypes[i]);
      }

      args[i] = _marshalArgs[i].unmarshal(reader);
    }

    try {
      Object result = _method.invoke(object, args);

      writer.startReplyOk(reader.getRequestId());

      // XXX
      // If the method return type is a EJB 3.0 remote interface, it is difficult to
      // find the correct marshaller. Since EJB 3.0 remote interfaces do not need to
      // have well-known superinterfaces, the method return type might not help.
      // For now, we rely on the result object type.

      // TCK: ejb30/bb/session/stateful/sessioncontext/annotated/getBusinessObjectRemote1
      
      // See RemoteMarshal.  The following breaks several TCK
      /*
      if (result != null && (result instanceof com.caucho.ejb.session.SessionObject))
        _marshalReturn = new EjbSessionObjectMarshal(_method.getReturnType(), _skeleton);
      */

      _marshalReturn.marshal(writer, result);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();

      if (cause instanceof RemoteException)
        throw cause;

      for (int i = 0; i < _exnTypes.length; i++) {
        if (_exnTypes[i].isAssignableFrom(cause.getClass()))
          throw cause;
      }

      throw new RemoteException(cause.toString(), cause);
    }
  }
}
