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

import java.io.*;
import java.net.*;

import com.caucho.config.*;
import com.caucho.config.types.*;
import com.caucho.lifecycle.*;
import com.caucho.server.cluster.*;
import com.caucho.server.connection.*;
import com.caucho.server.host.*;
import com.caucho.server.http.*;
import com.caucho.server.port.*;
import com.caucho.server.resin.*;
import com.caucho.server.webapp.*;
import com.caucho.vfs.*;

/**
 * Embeddable version of the Resin server.
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * HttpEmbed http = new Http(8080);
 * resin.addPort(http);
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/home/ferg/ws/foo");
 *
 * resin.addWebApp(webApp);
 *
 * resin.start();
 *
 * resin.join();
 * </pre></code>
 */
public class ResinEmbed
{
  private static final String EMBED_CONF
    = "com/caucho/resin/resin-embed.xml";
  
  private Resin _resin = new Resin();
  private Cluster _cluster;
  private ClusterServer _clusterServer;
  private Host _host;
  private Server _server;

  private Lifecycle _lifecycle = new Lifecycle();
  
  /**
   * Creates a new resin server.
   */
  public ResinEmbed()
  {
    InputStream is = null;
    try {
      Config config = new Config();
      
      is = _resin.getClassLoader().getResourceAsStream(EMBED_CONF);

      config.configure(_resin, is);
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      try {
	is.close();
      } catch (IOException e) {
      }
    }

    _cluster = _resin.findCluster("");
    _clusterServer = _cluster.findServer("");
  }

  //
  // Configuration/Injection methods

  /**
   * Adds a port to the server, e.g. a HTTP port.
   *
   * @param port the embedded port to add to the server
   */
  public void addPort(PortEmbed port)
  {
    port.bindTo(_clusterServer);
  }

  /**
   * Sets a list of ports.
   */
  public void setPorts(PortEmbed []ports)
  {
    for (PortEmbed port : ports)
      addPort(port);
  }

  /**
   * Adds a web-app to the server.
   */
  public void addWebApp(WebAppEmbed webApp)
  {
    try {
      start();

      WebAppConfig config = new WebAppConfig();
      config.setContextPath(webApp.getContextPath());
      config.setRootDirectory(new RawString(webApp.getRootDirectory()));

      _host.addWebApp(config);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Sets a list of webapps
   */
  public void setWebApps(WebAppEmbed []webApps)
  {
    for (WebAppEmbed webApp : webApps)
      addWebApp(webApp);
  }

  //
  // Lifecycle
  //

  /**
   * Starts the embedded server
   */
  public void start()
  {
    if (! _lifecycle.toActive())
      return;
      
    try {
      _resin.start();
      _server = _resin.getServer();
      HostConfig hostConfig = new HostConfig();
      _server.addHost(hostConfig);
      _host = _server.getHost("", 0);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Stops the embedded server
   */
  public void stop()
  {
    if (! _lifecycle.toStop())
      return;
      
    try {
      _resin.stop();
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Destroys the embedded server
   */
  public void destroy()
  {
    if (! _lifecycle.toDestroy())
      return;
      
    try {
      _resin.destroy();
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }

  //
  // Testing API
  //

  /**
   * Sends a HTTP request to the embedded server for testing.
   *
   * @param httpRequest HTTP request string, e.g. "GET /test.jsp"
   * @param os output stream to receive the request
   */
  public void request(String httpRequest, OutputStream os)
    throws Exception
  {
    start();

    TestConnection conn = createConnection();

    conn.request(httpRequest, os);
  }

  /**
   * Sends a HTTP request to the embedded server for testing.
   *
   * @param httpRequest HTTP request string, e.g. "GET /test.jsp"
   *
   * @return the HTTP result string
   */
  public String request(String httpRequest)
    throws Exception
  {
    start();

    TestConnection conn = createConnection();

    return conn.request(httpRequest);
  }

  /**
   * Creates a test connection to the server
   */
  private TestConnection createConnection()
  {
    TestConnection conn = new TestConnection();

    return conn;
  }

  protected void finalize()
    throws Throwable
  {
    super.finalize();
    
    destroy();
  }

  /**
   * Test HTTP connection
   */
  private class TestConnection {
    StreamConnection _conn;
    HttpRequest _request;
    VfsStream _vfsStream;
    ReadStream _readStream;
    WriteStream _writeStream;
    InetAddress _localAddress;
    InetAddress _remoteAddress;
    int _port = 6666;
    char []_chars = new char[1024];
    byte []_bytes = new byte[1024];

    TestConnection()
    {
      _conn = new StreamConnection();
      // _conn.setVirtualHost(_virtualHost);
      
      _request = new HttpRequest(_resin.getServer(), _conn);
      _request.init();
      
      _vfsStream = new VfsStream(null, null);
      _readStream = new ReadStream();
      _writeStream = new WriteStream();
      
      // _conn.setSecure(_isSecure);
      
      try {
        _localAddress = InetAddress.getByName("127.0.0.1");
        _remoteAddress = InetAddress.getByName("127.0.0.1");
      } catch (IOException e) {
      }
    }

    public HttpRequest getRequest()
    {
      return _request;
    }

    public void setVirtualHost(String virtualHost)
    {
      _conn.setVirtualHost(virtualHost);
    }

    public void setPort(int port)
    {
      if (port > 0)
        _port = port;
    }

    public void setLocalIP(String ip)
      throws IOException
    {
      _localAddress = InetAddress.getByName(ip);
    }

    public void setRemoteIP(String ip)
      throws IOException
    {
      _remoteAddress = InetAddress.getByName(ip);
    }

    public void setSecure(boolean isSecure)
    {
      _conn.setSecure(isSecure);
    }

    public String request(String input) throws Exception
    {
      OutputStream os = new ByteArrayOutputStream();

      request(input, os);

      return os.toString();
    }

    public boolean allocateKeepalive()
    {
      return true;
    }

    public void request(String input, OutputStream os)
      throws Exception
    {
      Thread.yield();
      
      ByteArrayInputStream is;

      int len = input.length();
      if (_chars.length < len) {
        _chars = new char[len];
        _bytes = new byte[len];
      }

      input.getChars(0, len, _chars, 0);
      for (int i = 0; i < len; i++)
        _bytes[i] = (byte) _chars[i];

      is = new ByteArrayInputStream(_bytes, 0, len);

      _vfsStream.init(is, os);
      _conn.setStream(is, os);
      _conn.setLocalAddress(_localAddress);
      _conn.setLocalPort(_port);
      _conn.setRemoteAddress(_remoteAddress);
      _conn.setRemotePort(9666);
      // _conn.setSecure(_isSecure);

      Thread.sleep(10);

      ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
      try {
        while (_request.handleRequest()) {
        }
      } catch (EOFException e) {
      } finally {
        Thread.currentThread().setContextClassLoader(oldLoader);
      }
    }

    public void close()
    {
    }
  }
}
