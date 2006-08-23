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

import com.caucho.jms.services.HessianListener;

import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.loader.StartListener;

/**
 * A child of &lt;web-service&gt; that describes a published interface to
 * the service.
 */
public class HessianJmsTransport extends JmsTransport {
  private HessianInterface _hessianInterface;

  public HessianJmsTransport(HessianInterface hessianInterface)
  {
    super(hessianInterface.getWebService());
  }

  public void initService(Destination destination)
  {
    Object service = _hessianInterface.getWebService().getServiceInstance();
    Class apiClass = _hessianInterface.getAPIClass();

    HessianListener hessianListener = new HessianListener();

    hessianListener.setConnectionFactory(getConnectionFactory());
    hessianListener.setDestination(destination);
    hessianListener.setService(service);
    hessianListener.setAPIClass(apiClass);

    hessianListener.init();

    Environment.addEnvironmentListener(new StartListener(hessianListener));
    Environment.addClassLoaderListener(new CloseListener(hessianListener));
  }
}
