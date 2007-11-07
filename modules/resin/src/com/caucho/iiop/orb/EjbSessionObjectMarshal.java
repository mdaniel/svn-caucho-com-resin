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
 * @author Rodrigo Westrupp
 */

package com.caucho.iiop.orb;

import com.caucho.ejb.AbstractEJBObject;
import com.caucho.iiop.IiopSkeleton;
import com.caucho.iiop.marshal.Marshal;

import java.lang.reflect.Method;
import javax.rmi.PortableRemoteObject;

/**
 * EJB session object marshaller
 */
public class EjbSessionObjectMarshal extends CorbaObjectMarshal {
  Class _type;
  IiopSkeleton _skeleton;

  public EjbSessionObjectMarshal(Class type, IiopSkeleton skeleton)
  {
    _type = type;
    _skeleton = skeleton;
  }

  @Override
  public void marshal(org.omg.CORBA_2_3.portable.OutputStream os,
                      Object value)
  {
    AbstractEJBObject sessionObj = (AbstractEJBObject) value;
    String url = sessionObj.__caucho_getServer().getProtocolId();

    // XXX: check multiple business interfaces.
    java.util.ArrayList<Class> apiList = new java.util.ArrayList<Class>();
    apiList.add(_type);

    // Adds the suffix "#com_sun_ts_tests_ejb30_common_sessioncontext_Three1IF";
    url = url + "#" + _type.getName().replace(".", "_");

    IiopSkeleton resSkeleton = new IiopSkeleton(value,
                                                apiList,
                                                Thread.currentThread().getContextClassLoader(),
                                                _skeleton.getIOR().getHost(),
                                                _skeleton.getIOR().getPort(),
                                                url);

    super.marshal(os, resSkeleton);
  }

  @Override
  public Object unmarshal(org.omg.CORBA_2_3.portable.InputStream is)
  {
    Object result = super.unmarshal(is);

    if (result != null)
      result = PortableRemoteObject.narrow(result, _type);

    return result;
  }
}
