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
 * @author Emil Ong
 */

package com.caucho.esb.encoding;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.esb.WebService;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;

import com.caucho.services.server.GenericService;

/**
 * Invokes a service based on a Hessian-encoded request.
 */
public class HessianEncoding implements ServiceEncoding {
  protected static Logger log
    = Logger.getLogger(HessianEncoding.class.getName());

  private Class _serviceAPI;
  private Object _serviceImpl;
  
  private HessianSkeleton _skeleton;

  private WebService _webService;

  private SerializerFactory _serializerFactory;

  public void setWebService(WebService webService)
  {
    _webService = webService;
  }

  /**
   * Sets the service class.
   */
  public void setService(Object serviceImpl)
  {
    _serviceImpl = serviceImpl;
  }

  /**
   * Sets the api-class.
   */
  public void setAPIClass(Class serviceAPI)
  {
    _serviceAPI = serviceAPI;
  }

  /**
   * Gets the api-class.
   */
  public Class getAPIClass()
  {
    return _serviceAPI;
  }

  /**
   * Sets the serializer factory.
   */
  public void setSerializerFactory(SerializerFactory factory)
  {
    _serializerFactory = factory;
  }

  /**
   * Gets the serializer factory.
   */
  public SerializerFactory getSerializerFactory()
  {
    if (_serializerFactory == null)
      _serializerFactory = new SerializerFactory();

    return _serializerFactory;
  }

  /**
   * Sets the serializer send collection java type.
   */
  public void setSendCollectionType(boolean sendType)
  {
    getSerializerFactory().setSendCollectionType(sendType);
  }

  public void init()
  {
    if (_serviceImpl == null)
      _serviceImpl = this;

    if (_serviceImpl != null) {
      _serviceAPI = findRemoteAPI(_serviceImpl.getClass());

      if (_serviceAPI == null)
        _serviceAPI = _serviceImpl.getClass();
    }

    _skeleton = new HessianSkeleton(_serviceImpl, _serviceAPI);
  }
  
  private Class findRemoteAPI(Class implClass)
  {
    if (implClass == null || implClass.equals(GenericService.class))
      return null;
    
    Class []interfaces = implClass.getInterfaces();

    if (interfaces.length == 1)
      return interfaces[0];

    return findRemoteAPI(implClass.getSuperclass());
  }

  public void invoke(InputStream is, OutputStream os)
  {
    try {
      Hessian2Input in = new Hessian2Input(is);
      AbstractHessianOutput out;

      SerializerFactory serializerFactory = getSerializerFactory();

      in.setSerializerFactory(serializerFactory);

      int code = in.read();

      if (code != 'c') {
        // XXX: deflate
        throw new IOException("expected 'c' in hessian input at " + code);
      }

      int major = in.read();
      int minor = in.read();

      if (major >= 2)
        out = new Hessian2Output(os);
      else
        out = new HessianOutput(os);

      out.setSerializerFactory(serializerFactory);

      if (_skeleton == null)
        throw new Exception("skeleton is null!");

      _skeleton.invoke(in, out);

      out.close();
    } catch (IOException e) {
      log.log(Level.INFO, "Unable to process request: ", e);
    } catch (Throwable e) {
      log.log(Level.INFO, "Unable to process request: ", e);
    }
  }
}
