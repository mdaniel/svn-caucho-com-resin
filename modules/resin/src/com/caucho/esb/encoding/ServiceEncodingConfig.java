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

import java.util.HashMap;

import com.caucho.config.BuilderProgram;

import com.caucho.esb.WebService;
import com.caucho.esb.transport.ServiceTransport;
import com.caucho.esb.transport.ServiceTransportConfig;

/**
 * Configures a service encoder.
 */
public class ServiceEncodingConfig {
  private static HashMap<String,Class> _encodings = new HashMap<String,Class>();

  private ServiceEncoding _encoding;
  private WebService _webService;

  public ServiceEncodingConfig(WebService webService)
  {
    _webService = webService;
  }

  public WebService getWebService()
  {
    return _webService;
  }

  public void setType(String type)
    throws UnknownServiceEncodingException, InstantiationException, 
           IllegalAccessException
  {
    Class encodingClass = _encodings.get(type);

    if (encodingClass == null)
      throw new UnknownServiceEncodingException(type);

    _encoding = (ServiceEncoding) encodingClass.newInstance();
    _encoding.setWebService(_webService);
  }

  public ServiceTransportConfig createTransport()
  {
    return new ServiceTransportConfig(_webService);
  }

  public void addTransport(ServiceTransport transport)
  {
    transport.setEncoding(_encoding);
  }

  public void addBuilderProgram(BuilderProgram program)
    throws Throwable
  {
    program.configure(_encoding);
  }

  public ServiceEncoding replaceObject()
  {
    return _encoding;
  }

  static {
    _encodings.put("hessian", HessianEncoding.class);
    _encodings.put("soap", SoapEncoding.class);
  }
}
