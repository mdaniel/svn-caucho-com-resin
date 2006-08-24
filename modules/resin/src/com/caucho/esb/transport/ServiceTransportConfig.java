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

package com.caucho.esb.transport;

import java.util.HashMap;

import com.caucho.esb.WebService;
import com.caucho.config.BuilderProgram;

/**
 * An transport for a (web) service.
 */
public class ServiceTransportConfig {
  private static HashMap<String,Class> _transports
    = new HashMap<String,Class>();

  private ServiceTransport _transport;
  private WebService _webService;

  public ServiceTransportConfig(WebService webService)
  {
    _webService = webService;
  }

  public void setType(String type)
    throws UnknownServiceTransportException, InstantiationException, 
           IllegalAccessException
  {
    Class transportClass = _transports.get(type);

    if (transportClass == null)
      throw new UnknownServiceTransportException(type);

    _transport = (ServiceTransport) transportClass.newInstance();
    _transport.setWebService(_webService);
  }

  public void setSendResponse(boolean sendResponse)
  {
    _transport.setSendResponse(sendResponse);
  }

  public void addBuilderProgram(BuilderProgram program)
    throws Throwable
  {
    program.configure(_transport);
  }

  public ServiceTransport replaceObject()
    throws Throwable
  {
    _transport.init();

    return _transport;
  }

  static {
    _transports.put("http", HttpTransport.class);
    _transports.put("jms", JmsTransport.class);
  }
}
