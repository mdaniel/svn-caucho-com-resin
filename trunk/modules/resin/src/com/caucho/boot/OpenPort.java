/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.boot;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.annotation.NoAspect;
import com.caucho.config.program.ConfigProgram;
import com.caucho.network.listen.TcpPort;
import com.caucho.util.CurrentTime;
import com.caucho.vfs.JsseSSLFactory;
import com.caucho.vfs.QJniServerSocket;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.SSLFactory;

/**
 * Represents a protocol connection.
 */
@NoAspect
public class OpenPort
{
  private static final Logger log = Logger.getLogger(OpenPort.class.getName());
  
  private String _protocol;
  
  private String _address;
  private InetAddress _socketAddress;
  private int _port;
  private boolean _isJsse;
  
  public OpenPort()
  {
    // super.setClass(HttpProtocol.class); // dummy
  }

  /*
  public void setClass(Class cl)
  {
  }
  */
  
  public String getProtocolName()
  {
    return _protocol;
  }
  
  public void setProtocolName(String protocol)
  {
    _protocol = protocol;
  }
  
  public void setPort(int port)
  {
    _port = port;
  }
  
  public int getPort()
  {
    return _port;
  }

  /**
   * Sets the address
   */
  @Configurable
  public void setAddress(String address)
    throws UnknownHostException
  {
    if ("*".equals(address))
      address = null;

    _address = address;

    if (address != null)
      _socketAddress = InetAddress.getByName(address);
  }
  
  public String getAddress()
  {
    return _address;
  }

  /**
   * Sets the SSL factory
   */
  public void addOpenssl(ConfigProgram program)
  {
  }

  public void addJsse(ConfigProgram program)
  {
    _isJsse = true;
  }

  /**
   * binds for the watchdog.
   */
  public QServerSocket bindForWatchdog()
    throws java.io.IOException
  {
    QServerSocket ss;
    
    if (_port <= 0) {
      // server/6e0k
      return null;
    }

    // use same method for ports for testability reasons
    if (_port >= 1024 && ! CurrentTime.isTest()) {
      return null;
    }
    /*
    if (_port >= 1024)
      return null;
    else
    */
    
    if (_isJsse) {
      if (_port < 1024) {
        log.warning(this + " cannot bind jsse in watchdog");
      }
      
      return null;
    }

    if (_socketAddress != null) {
      ss = QJniServerSocket.createJNI(_socketAddress, _port);

      if (ss == null)
        return null;

      log.fine(this + " watchdog binding to " + _socketAddress.getHostName() + ":" + _port);
    }
    else {
      ss = QJniServerSocket.createJNI(null, _port);

      if (ss == null)
        return null;

      log.fine(this + " watchdog binding to *:" + _port);
    }

    if (! ss.isJni()) {
      ss.close();

      return ss;
    }

    /*
    ss.setTcpNoDelay(_isTcpNoDelay);
    ss.setTcpKeepalive(_isTcpKeepalive);
    ss.setTcpCork(_isTcpCork);

    ss.setConnectionSocketTimeout((int) getSocketTimeout());
    */

    return ss;
  }

  public void addBuilderProgram(ConfigProgram program)
  {
  }

  public static class DummyOpenSSLFactory implements SSLFactory {
    public void addBuilderProgram(ConfigProgram program)
    {
    }

    public QServerSocket create(InetAddress host, int port)
    {
      return null;
    }
  
    public QServerSocket bind(QServerSocket ss)
    {
      return null;
    }
  }
}
