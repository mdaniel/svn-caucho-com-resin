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

package com.caucho.v5.resin;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.logging.LogManager;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.types.RawString;
import com.caucho.v5.deploy.ConfigInstanceBuilder;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.http.host.HostConfig;
import com.caucho.v5.http.webapp.DeployGeneratorWebAppSingle;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.http.webapp.WebAppBuilder;
import com.caucho.v5.http.webapp.WebAppConfig;
import com.caucho.v5.http.webapp.WebAppContainer;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.server.container.ArgsServerBase;
import com.caucho.v5.server.container.ServerBase;
import com.caucho.v5.server.container.ServerBuilderResin;
import com.caucho.v5.server.resin.EmbedArgs;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Vfs;

/**
 * Embeddable version of the Resin server.
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * HttpEmbed http = new HttpEmbed(8080);
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
public class ResinEmbed implements Closeable
{
  private static final L10N L = new L10N(ResinEmbed.class);

  private static final String EMBED_CONF
    = "classpath:com/caucho/resin/resin-embed.xml";

  private final ArgsServerBase _args;
  private ServerBase _resin;
  
  private String _configFile = EMBED_CONF;

  private Host _host;
  private HttpContainerServlet _server;

  private String _serverHeader;

  private final ArrayList<BeanEmbed> _beanList
    = new ArrayList<BeanEmbed>();

  private final ArrayList<WebAppEmbed> _webAppList
    = new ArrayList<WebAppEmbed>();
  
  private final ArrayList<PortEmbed> _portList
    = new ArrayList<PortEmbed>();

  private Lifecycle _lifecycle = new Lifecycle();
  
  private boolean _isScanRoot;
  private boolean _isConfig;
  private boolean _isDevelopmentMode;
  private boolean _isIgnoreClientDisconnect;

  private ServerBuilderResin _serverBuilder;

  /**
   * Creates a new resin server.
   */
  public ResinEmbed()
  {
    _args = new EmbedArgs(new String[] { "start" });
    // args.setServerId("embed");
    _args.setServerId("default");
    _args.setRootDirectory(Vfs.lookup());
    
    _args.initHomeClassPath();
  }

  /**
   * Creates a new resin server.
   */
  public ResinEmbed(String configFile)
  {
    this();
    
    setConfig(configFile);
  }

  //
  // Configuration/Injection methods
  //
  
  /**
   * Sets the root directory
   */
  public void setRootDirectory(String rootUrl)
  {
    _args.setRootDirectory(Vfs.lookup(rootUrl));
  }
  
  /**
   * Sets the root directory
   */
  public void setServerId(String id)
  {
    _args.setServerId(id);
  }

  /**
   * Sets the config file
   */
  public void setConfig(String configFile)
  {
    _configFile = configFile;
  }
  
  /**
   * Adds a http port to the server.
   */
  public ResinEmbed addHttpPort(int port)
  {
    addPort(new HttpEmbed(port));
    
    return this;
  }

  /**
   * Adds a port to the server, e.g. a HTTP port.
   *
   * @param port the embedded port to add to the server
   */
  public void addPort(PortEmbed port)
  {
    _portList.add(port);
    
    if (_serverBuilder != null) {
      port.bindTo(_serverBuilder);
    }
    
    /*
    // server/1e00
    if (_clusterServer == null)
      initConfig(_configFile);
    
    // XXX: port.bindTo(_clusterServer);
     */
  }

  /**
   * Sets a list of ports.
   */
  public void setPorts(PortEmbed []ports)
  {
    for (PortEmbed port : ports) {
      addPort(port);
    }
  }

  /**
   * Sets the server header
   */
  public void setServerHeader(String serverName)
  {
    _serverHeader = serverName;
  }
  
  public WebAppEmbed createWebApp(String contextPath, String rootDirectory)
  {
    WebAppEmbed webApp = new WebAppEmbed(contextPath, rootDirectory);
    
    addWebApp(webApp);
    
    return webApp;
  }

  /**
   * Adds a web-app to the server.
   */
  public void addWebApp(WebAppEmbed webApplication)
  {
    if (webApplication == null) {
      throw new NullPointerException();
    }

    _webAppList.add(webApplication);

    // If the server has already been started, the web application will need to
    // be explicitly deployed.
    if (_lifecycle.isActive()) {
      deployWebApplication(webApplication);
    }
  }
  
  public void removeWebApp(WebAppEmbed webApplication)
  {
    if (webApplication == null) {
      throw new NullPointerException();
    }

    _webAppList.remove(webApplication);

    // If the server has already been started, the web application will need
    // to be undeployed.
    if (_lifecycle.isActive()) {
      undeployWebApplication(webApplication);
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

  /**
   * Adds a web bean.
   */
  public void addBean(BeanEmbed bean)
  {
    _beanList.add(bean);

    if (_lifecycle.isActive()) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(_server.classLoader());

        bean.configure();
      } catch (RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw ConfigException.wrap(e);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }
  }
  
  public void setDevelopmentMode(boolean isDevelopment)
  {
    _isDevelopmentMode = isDevelopment;
  }
  
  public void setIgnoreClientDisconnect(boolean isIgnore)
  {
    _isIgnoreClientDisconnect = isIgnore;
  }

  /**
   * Initialize the Resin environment
   */
  public void initializeEnvironment()
  {
    EnvLoader.initializeEnvironment();
  }

  /**
   * Set log handler
   */
  public void resetLogManager()
  {
    LogManager.getLogManager().reset();
  }

  public void addScanRoot()
  {
    _isScanRoot = true;
  }

  //
  // Lifecycle
  //

  /**
   * Starts the embedded server
   */
  public void start()
  {
    if (! _lifecycle.toActive()) {
      return;
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      EnvLoader.initializeEnvironment();

      _serverBuilder = new ServerBuilderResin(_args);
      
      _resin = _serverBuilder.build();
      // _resin.preConfigureInit();
      
      thread.setContextClassLoader(_resin.getClassLoader());
      
      if (_isScanRoot) {
        _resin.getClassLoader().addScanRoot();
      }

      initConfig(_configFile);

      // _server = _resin.createHttpContainer();
      
      _server = (HttpContainerServlet) _resin.getHttp();
      
      thread.setContextClassLoader(_resin.getClassLoader());
      
      for (PortEmbed port : _portList) {
        port.bindTo(_serverBuilder);
      }

      if (_serverHeader != null)
        _server.setServerHeader(_serverHeader);
      
      _server.setDevelopmentModeErrorPage(_isDevelopmentMode);
      _server.setIgnoreClientDisconnect(_isIgnoreClientDisconnect);

      for (BeanEmbed beanEmbed : _beanList) {
        beanEmbed.configure();
      }

      // _resin.start();

      _host = _server.getHost("", 0);
      if (_host == null) {
        HostConfig hostConfig = new HostConfig();
        _server.addHost(hostConfig);
        _host = _server.getHost("", 0);
      }

      if (_host == null) {
        throw new ConfigException(L.l("ResinEmbed requires a <host> to be configured in the resin.xml, because the webapps must belong to a host."));
      }

      for (WebAppEmbed webApplication : _webAppList) {
        deployWebApplication(webApplication);
      }
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Waits for the Resin process to exit.
   */
  public void join()
  {
    while (! _resin.isClosed()) {
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
      }
    }
  }

  /**
   * Destroys the embedded server
   */
  public void destroy()
  {
    if (! _lifecycle.toDestroy()) {
      return;
    }

    try {
      _resin.close();
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw ConfigException.wrap(e);
    }
  }
  
  @Override
  public void close()
  {
    destroy();
  }

  //
  // Testing API
  //

  /**
   * Sends a HTTP request to the embedded server for testing.
   *
   * @param is input stream containing the HTTP request
   * @param os output stream to receive the request
   */
  /*
  public void request(InputStream is, OutputStream os)
    throws IOException
  {
    start();

    TestConnection conn = createConnection();

    conn.request(is, os);
  }
  */

  /**
   * Sends a HTTP request to the embedded server for testing.
   *
   * @param httpRequest HTTP request string, e.g. "GET /test.jsp"
   * @param os output stream to receive the request
   */
  /*
  public void request(String httpRequest, OutputStream os)
    throws IOException
  {
    start();

    TestConnection conn = createConnection();

    conn.request(httpRequest, os);
  }
  */

  /**
   * Sends a HTTP request to the embedded server for testing.
   *
   * @param httpRequest HTTP request string, e.g. "GET /test.jsp"
   *
   * @return the HTTP result string
   */
  /*
  public String request(String httpRequest)
    throws IOException
  {
    start();

    TestConnection conn = createConnection();

    return conn.request(httpRequest);
  }
  */

  /**
   * Creates a test connection to the server
   */
  /*
  private TestConnection createConnection()
  {
    TestConnection conn = new TestConnection();

    return conn;
  }
*/
  //
  // utilities
  //

  private void initConfig(String configFile)
  {
    try {
      if (_isConfig)
        return;
      _isConfig = true;

      // XXX: _resin.configureFile(Vfs.lookup(configFile));
      
      // _resin.createServer();
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }

    /*
    CloudSystem cloudSystem = TopologyService.getCurrent().getSystem();

    if (cloudSystem.getClusterList().length == 0)
      throw new ConfigException(L.l("Resin needs at least one defined <cluster>"));

    String clusterId = cloudSystem.getClusterList()[0].getId();

    _cluster = cloudSystem.findCluster(clusterId);

    if (_cluster.getPodList().length == 0)
      throw new ConfigException(L.l("Resin needs at least one defined <server> or <cluster-pod>"));

    if (_cluster.getPodList()[0].getServerList().length == 0)
      throw new ConfigException(L.l("Resin needs at least one defined <server>"));

    CloudServer cloudServer = _cluster.getPodList()[0].getServerList()[0];
    */ 
    // _clusterServer = cloudServer.getData(ClusterServer.class);
    
    /*
    if (cloudServer != null)
      _resin.setServerId(cloudServer.getId());
      */
  }

  private void deployWebApplication(WebAppEmbed webApplication)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      // Web applications need to be loaded under the host class-loader.
      thread.setContextClassLoader(_host.getClassLoader());

      WebAppConfig configuration = new WebAppConfig();
      configuration.setContextPath(webApplication.getContextPath());
      configuration.setRootDirectory(new RawString(webApplication
          .getRootDirectory()));

      if (webApplication.getArchivePath() != null) {
        configuration.setArchivePath(new RawString(webApplication
            .getArchivePath()));
      }

      configuration.addBuilderProgram(new WebAppProgram(webApplication));

      //_host.getWebAppContainer().addWebApp(configuration);
      
      WebAppContainer container = _host.getWebAppContainer();
      
      DeployGeneratorWebAppSingle deployGenerator = container.createDeployGenerator(configuration);
      webApplication.setDeployGenerator(deployGenerator);
      
      container.addWebApp(deployGenerator);
      
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private void undeployWebApplication(WebAppEmbed webApplication)
  {
    //webApplication.getWebApp().getController().destroy();
    //webApplication.getWebApp().destroy();
    _host.getWebAppContainer().removeWebApp(webApplication.getDeployGenerator());
  }

  protected void finalize()
    throws Throwable
  {
    super.finalize();

    destroy();
  }

  /**
   * Basic embedding server.
   */
  public static void main(String []args)
    throws Exception
  {
    ResinEmbed resin = new ResinEmbed();

    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("--port=")) {
        int port = Integer.parseInt(args[i].substring("--port=".length()));

        HttpEmbed http = new HttpEmbed(port);
        resin.addPort(http);
      }
      else if (args[i].startsWith("--config=")) {
        String config = args[i].substring("--config=".length());

        resin.setConfig(config);
      }
      else if (args[i].startsWith("--deploy:")) {
        String valueString = args[i].substring("--deploy:".length());

        String []values = valueString.split("[=,]");

        String role = null;

        for (int j = 0; j < values.length; j += 2) {
          if (values[j].equals("role"))
            role = values[j + 1];
        }

        WebAppLocalDeployEmbed webApp = new WebAppLocalDeployEmbed();
        if (role != null)
          webApp.setRole(role);

        resin.addWebApp(webApp);
      }
    }

    resin.resetLogManager();

    resin.start();

    resin.join();
  }

  /**
   * Test HTTP connection
   */
  /*
  private class TestConnection {
    ConnectionSocketStream _conn;
    RequestProtocolHttp _request;
    VfsStream _vfsStream;
    InetAddress _localAddress;
    InetAddress _remoteAddress;
    int _port = 6666;
    char []_chars = new char[1024];
    byte []_bytes = new byte[1024];

    TestConnection()
    {
      _conn = new ConnectionSocketStream();
      // _conn.setVirtualHost(_virtualHost);

      HttpProtocol httpProtocol = new HttpProtocol(_resin.getHttp());
      _request = null;//new RequestProtocolHttp(httpProtocol, _conn);
      _request.init();

      _vfsStream = new VfsStream(null, null);

      // _conn.setSecure(_isSecure);

      try {
        _localAddress = InetAddress.getByName("127.0.0.1");
        _remoteAddress = InetAddress.getByName("127.0.0.1");
      } catch (IOException e) {
      }
    }

    public String request(String input) throws IOException
    {
      OutputStream os = new ByteArrayOutputStream();

      request(input, os);

      return os.toString();
    }

    public void request(String input, OutputStream os)
      throws IOException
    {
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

      request(is, os);
    }

    public void request(InputStream is, OutputStream os)
      throws IOException
    {
      Thread.yield();

      WriteStream out = Vfs.openWrite(os);
      out.setDisableClose(true);

      ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
      try {
        _vfsStream.init(is, os);
        _conn.setStream(is, os);
        _conn.setLocalAddress(_localAddress);
        _conn.setLocalPort(_port);
        _conn.setRemoteAddress(_remoteAddress);
        _conn.setRemotePort(9666);
        // _conn.setSecure(_isSecure);

        try {
          Thread.sleep(10);
        } catch (Exception e) {
        }

        while (! _request.service().isClose()) {
          out.flush();
        }
      } catch (EOFException e) {
      } finally {
        out.flush();

        Thread.currentThread().setContextClassLoader(oldLoader);
      }
    }
  }
*/
  static class WebAppProgram extends ConfigProgram {
    private final WebAppEmbed _config;

    WebAppProgram(WebAppEmbed webAppConfig)
    {
      super(ConfigContext.getCurrent());
      
      _config = webAppConfig;
    }

    /**
     * Configures the object.
     */
    @Override
    public <T> void inject(T bean, InjectContext env)
      throws ConfigException
    {
      if (bean instanceof ConfigInstanceBuilder) {
        ConfigInstanceBuilder builder = (ConfigInstanceBuilder) bean;
        
        builder.addContentProgram(this);
      }
      else if (bean instanceof WebAppBuilder) {
        WebAppBuilder builder = (WebAppBuilder) bean;
        
        builder.addContentProgram(this);
      }
      else {
        _config.configure((WebApp) bean);
      }
    }
  }
}
