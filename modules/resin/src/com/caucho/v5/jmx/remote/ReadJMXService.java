/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.jmx.remote;

import java.io.IOException;

import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.caucho.v5.hessian.server.GenericService;

/**
 * JMX service.
 */
public class ReadJMXService extends GenericService implements RemoteJMX {
  private MBeanServerConnection _server;

  /**
   * Sets the mbean server.
   */
  public void setMBeanServer(MBeanServerConnection server)
  {
    _server = server;
  }

  /**
   * Initialize the server.
   */
  public void init()
  {
    if (_server == null)
      throw new NullPointerException("mbean-server is required");
  }
  
  /**
   * Returns the mbean info
   */
  public MBeanInfo getMBeanInfo(String objectName)
    throws JMException, IOException
  {
    return _server.getMBeanInfo(new ObjectName(objectName));
  }
  
  /**
   * Returns an attribute.
   */
  public Object getAttribute(String objectName, String attributeName)
    throws JMException, IOException
  {
    return _server.getAttribute(new ObjectName(objectName), attributeName);
  }
}