/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.embed;

import java.util.logging.Logger;

import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.server.port.Port;

import com.caucho.server.resin.ServletServer;

import com.caucho.util.Log;
import com.caucho.util.L10N;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Main facade for embedding Resin in an application.
 */
public class EmbedResinServer implements EnvironmentBean {
  private static final Logger log = Log.open(EmbedResinServer.class);
  private static final L10N L = new L10N(EmbedResinServer.class);

  private ServletServer _server;

  /**
   * Creates a new resin server.
   */
  public EmbedResinServer()
    throws Exception
  {
    _server = new ServletServer();
  }

  /**
   * Returns the environment class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _server.getClassLoader();
  }

  /**
   * Adds a HTTP protocol.
   */
  public void addHttp(String host, int port)
    throws Exception
  {
    Port httpPort = new Port();
    httpPort.setHost(host);
    httpPort.setPort(port);

    _server.addHttp(httpPort);
  }

  /**
   * Adds a web-app.
   */
  public EmbedWebApp addWebApp(String contextPath, String rootDirectory)
  {
    Path pwd = Vfs.getPwd(getClassLoader());
    
    EmbedWebApp webApp = new EmbedWebApp();

    return webApp;
  }

  /**
   * Starts the server.
   */
  public void start()
    throws Throwable
  {
    if (_server == null)
      throw new IllegalStateException(L.l("tried to start destroyed server"));
    
    _server.start();
  }

  /**
   * Stops the server.
   */
  public void stop()
    throws Throwable
  {
    if (_server == null)
      throw new IllegalStateException(L.l("tried to stop destroyed server"));
    
    _server.stop();
  }

  /**
   * Destroys the server.
   */
  public void destroy()
  {
    ServletServer server = _server;
    _server = null;

    if (server != null)
      server.destroy();
  }
}
