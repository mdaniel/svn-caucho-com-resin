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

package com.caucho.server.dispatch;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.caucho.jms.services.ServicesListener;

import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.loader.StartListener;

/**
 * A child of &lt;web-service&gt; that describes a published interface to
 * the service.
 */
public class SoapJmsTransport extends JmsTransport {
  private SoapInterface _soapInterface;

  public SoapJmsTransport(SoapInterface soapInterface)
  {
    super(soapInterface.getWebService());

    _soapInterface = soapInterface;
  }

  public void initService(Destination destination)
  {
    Object service = _soapInterface.getWebService().getServiceInstance();
    Class interfaceClass = _soapInterface.getInterfaceClass();

    ServicesListener servicesListener = new ServicesListener();

    servicesListener.setConnectionFactory(getConnectionFactory());
    servicesListener.setDestination(destination);
    servicesListener.setInterface(interfaceClass);
    servicesListener.setService(service);

    servicesListener.init();

    Environment.addEnvironmentListener(new StartListener(servicesListener));
    Environment.addClassLoaderListener(new CloseListener(servicesListener));
  }
}
