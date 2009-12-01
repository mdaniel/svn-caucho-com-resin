/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.resin;

import com.caucho.config.ConfigException;
import com.caucho.server.cluster.*;
import com.caucho.server.connection.Port;
import com.caucho.server.port.*;

/**
 * Embeddable version of a HTTP port
 */
public class HttpEmbed extends PortEmbed
{
  private Port _port;
  
  /**
   * Creates a new HttpEmbed configuration.
   */
  public HttpEmbed()
  {
  }
  
  /**
   * Creates a new HttpEmbed configuration with a specified port.
   *
   * @param port the TCP port of the embedded HTTP port.
   */
  public HttpEmbed(int port)
  {
    setPort(port);
  }
  
  /**
   * Creates a new HttpEmbed configuration with a specified port.
   *
   * @param port the TCP port of the embedded HTTP port.
   * @param address the TCP IP address of the embedded HTTP port.
   */
  public HttpEmbed(int port, String ipAddress)
  {
    setPort(port);
    setAddress(ipAddress);
  }

  /**
   * Returns the local, bound port
   */
  public int getLocalPort()
  {
    if (_port != null)
      return _port.getLocalPort();
    else
      return getPort();
  }
  
  /**
   * Binds the port to the server
   */
  public void bindTo(ClusterServer server)
  {
    try {
      _port = server.createHttp();

      _port.setPort(getPort());
      _port.setAddress(getAddress());
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
