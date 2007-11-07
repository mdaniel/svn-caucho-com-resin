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

package com.caucho.iiop.marshal;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.logging.*;

import com.caucho.ejb.*;
import com.caucho.iiop.*;
import com.caucho.iiop.orb.*;

/**
 * Remote object marshaller
 */
public class RemoteMarshal extends Marshal
{
  private static final Logger log
    = Logger.getLogger(RemoteMarshal.class.getName());
  private Class _cl;

  public RemoteMarshal(Class cl)
  {
    _cl = cl;
  }

  @Override
  public void marshal(org.omg.CORBA_2_3.portable.OutputStream os,
                      Object value)
  {
    if (value instanceof IiopProxy) {
      IiopProxyHandler handler
        = (IiopProxyHandler) Proxy.getInvocationHandler(value);

      StubImpl stub = handler.getStub();

      // IOR ior = stub.getIor();

      os.write_Object(new DummyObjectImpl(stub.getIOR()));
    }
    else if (value instanceof AbstractEJBObject) {
      AbstractEJBObject absObj = (AbstractEJBObject) value;


      AbstractServer server = absObj.__caucho_getServer();
      String local = absObj.__caucho_getId();

      // XXX TCK: ejb30/bb/session/stateless/sessioncontext/annotated/passBusinessObjectRemote1
      String url = server.getProtocolId(_cl) + "?" + local;

      String typeName = "RMI:" + _cl.getName() + ":0";

      ORBImpl orb = (ORBImpl) os.orb();

      IOR ior = new IOR(typeName, orb.getHost(), orb.getPort(), url);
      //writer.write_boolean(true);
      os.write_Object(new DummyObjectImpl(ior));
    }
    else {
      //writer.write_boolean(false);
      os.write_value((Serializable) value);
    }
  }

  @Override
  public Object unmarshal(org.omg.CORBA_2_3.portable.InputStream is)
  {
    Object value = is.read_Object(_cl);

    if (value instanceof StubImpl) {
      Class type = _cl;
      StubImpl stub = (StubImpl) value;

      IOR ior = stub.getIOR();
      String typeId = ior.getTypeId();

      if (typeId.startsWith("RMI:")) {
        int p = typeId.lastIndexOf(':');
        String typeName = typeId.substring(4, p);

        try {
          ClassLoader loader = Thread.currentThread().getContextClassLoader();
          Class typeClass = Class.forName(typeName, false, loader);

          if (typeClass.isInterface())
            type = typeClass;
        } catch (Exception e) {
          log.finer(e.toString());
        }
      }

      // XXX: check is_a

      StubMarshal stubMarshal = new StubMarshal(type);

      IiopProxyHandler handler =
        new IiopProxyHandler(stub.getORBImpl(),
                             stub,
                             stubMarshal);

      return Proxy.newProxyInstance(type.getClassLoader(),
                                    new Class[] { type, IiopProxy.class },
                                    handler);
    }

    return value;
  }
}
