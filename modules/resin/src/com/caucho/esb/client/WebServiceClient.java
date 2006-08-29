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

package com.caucho.esb.client;

import java.util.ArrayList;

import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.naming.Jndi;

/**
 */
public class WebServiceClient {
  private static final Logger log 
    = Logger.getLogger(WebServiceClient.class.getName());

  private Class _serviceClass;
  private String _jndiName;
  private String _url;

  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;
  }

  public void setInterface(Class serviceClass)
  {
    _serviceClass = serviceClass;
  }

  public void setUrl(String url)
  {
    _url = url;
  }

  public void init()
    throws Throwable
  {
    if (_jndiName == null)
      throw new ConfigException("jndi-name not set for <web-service-client>");

    Object proxy = ProxyManager.getWebServiceProxy(_serviceClass, _url);

    Jndi.bindDeepShort(_jndiName, proxy);
  }
}

