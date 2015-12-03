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

package com.caucho.server.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import com.caucho.config.Config;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.env.deploy.DeployConfig;
import com.caucho.env.deploy.DeployControllerAdmin;
import com.caucho.env.deploy.EnvironmentDeployController;
import com.caucho.inject.Module;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.host.Host;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CurrentTime;
import com.caucho.vfs.Path;

/**
 * Manages the lifecycle of a web-app. The same WebAppController is used for
 * each web-app instantiation, for example on restarts. It's only created or
 * destroyed if the web-app-deploy indicates it should be created/destroyed.
 * 
 * Each WebAppController corresponds to a DeployNetworkService tag with the
 * name "WebApp/[host]/[context-path]"
 */
@Module
public class WebAppController
  extends EnvironmentDeployController<WebApp,WebAppConfig>
{
  private static final Logger log
    = Logger.getLogger(WebAppController.class.getName());

  private final WebAppContainer _container;

  private WebAppController _parent;

  private String _idTail;
  
  // The context path is the URL prefix for the web-app
  private String _contextPath;
  private String _version = "";

  // Any old version web-app
  private WebAppController _oldWebAppController;
  private long _oldWebAppExpireTime;

  private String _warName;

  // regexp values
  private ArrayList<String> _regexpValues;

  private boolean _isInheritSession;
  private boolean _isDynamicDeploy;

  private ArrayList<Path> _dependPathList = new ArrayList<Path>();

  private String _sourceType = "unknown";

  private final Object _statisticsLock = new Object();

  private volatile long _lifetimeConnectionCount;
  private volatile long _lifetimeConnectionTime;
  private volatile long _lifetimeReadBytes;
  private volatile long _lifetimeWriteBytes;
  private volatile long _lifetimeClientDisconnectCount;

  private WebAppAdmin _admin;

  public WebAppController(String id, 
                          Path rootDirectory, 
                          WebAppContainer container)
  {
    this(id, rootDirectory, container, "/");
  }
  
  public WebAppController(Path rootDirectory,
                          WebAppContainer container,
                          String contextPath)
  {
    this(calculateId(container, contextPath), 
         rootDirectory,
         container,
         contextPath);
  }

  public WebAppController(String id,
                          Path rootDirectory,
                          WebAppContainer container,
                          String contextPath)
  {
    super(id, rootDirectory);

    _container = container;
    
    if (container == null)
      throw new NullPointerException();
    
    if (container.getHost() == null)
      throw new NullPointerException();

    _contextPath = contextPath;
    
    _idTail = calculateIdTail(getId());
  }
  
  private static String calculateIdTail(String id)
  {
    int p1 = id.indexOf('/');
    //String stage = id.substring(0, p1);
    int p2 = id.indexOf('/', p1 + 1);
    //String type = id.substring(p1 + 1, p2);
    int p3 = id.indexOf('/', p2 + 1);
    //String host = id.substring(p2 + 1, p3);
    
    return id.substring(p3);
  }
  private static String calculateId(WebAppContainer container,
                                    String contextPath)
  {
    if (contextPath.equals("") || contextPath.equals("/"))
      contextPath = "/ROOT";
    
    String stage = container.getServer().getStage();
    String hostId = container.getHost().getIdTail();
    
    return stage + "/webapp/" + hostId + contextPath;
  }
  
  public String getName()
  {
    // return getContextPath();
    return _idTail;
  }

  /**
   * Returns the webApp's canonical context path, e.g. /foo-1.0
   */
  public String getContextPath()
  {
    return _contextPath;
  }

  void setContextPath(String contextPath)
  {
    // server/1h10

    _contextPath = contextPath;
  }
  
  protected boolean isVersioning()
  {
    return false;
  }

  /**
   * Returns the webApp's context path
   */
  public String getContextPath(String uri)
  {
    if (getConfig() == null) {
      return getContextPath();
    }

    String contextPath = getConfig().getContextPath(uri);
    
    if (contextPath != null) {
      return contextPath;
    }
    else {
      return _contextPath;
    }
  }

  /**
   * Sets the war name prefix.
   */
  public void setWarName(String warName)
  {
    _warName = warName;
  }

  /**
   * Gets the war name prefix.
   */
  public String getWarName()
  {
    return _warName;
  }

  /**
   * Gets the URL
   */
  public String getURL()
  {
    return getHost().getURL() + _contextPath;
  }

  /**
   * Returns the parent controller.
   */
  public WebAppController getParent()
  {
    return _parent;
  }

  /**
   * Returns the web-app container.
   */
  public WebAppContainer getContainer()
  {
    return _container;
  }
  
  public ServletService getWebManager()
  {
    return _container.getServer();
  }

  /**
   * Sets the parent controller.
   */
  public void setParentWebApp(WebAppController parent)
  {
    _parent = parent;
  }

  /**
   * Returns the containing host.
   */
  public Host getHost()
  {
    return _container.getHost();
  }

  /**
   * Returns the source (for backwards compatibility)
   */
  public String getSourceType()
  {
    return _sourceType;
  }

  /**
   * Sets the source (for backwards compatibility)
   */
  public void setSourceType(String type)
  {
    _sourceType = type;
  }

  /**
   * Sets the regexp values.
   */
  public void setRegexpValues(ArrayList<String> values)
  {
    _regexpValues = values;
  }

  /**
   * True for inherit-session webApps.
   */
  public boolean isInheritSession()
  {
    return _isInheritSession;
  }

  /**
   * True for inherit-session
   */
  public void setInheritSession(boolean inheritSession)
  {
    _isInheritSession = inheritSession;
  }

  /**
   * Returns the webApp object.
   */
  public WebApp getWebApp()
  {
    return getDeployInstance();
  }

  /**
   * Set true for a dynamically deployed webApp.
   */
  public void setDynamicDeploy(boolean isDynamicDeploy)
  {
    _isDynamicDeploy = isDynamicDeploy;
  }

  /**
   * Returns true for a dynamically deployed webApp.
   */
  public boolean isDynamicDeploy()
  {
    return _isDynamicDeploy;
  }

  @Override
  protected String getMBeanTypeName()
  {
    return "WebApp";
  }

  @Override
  protected String getMBeanId()
  {
    String name = getName();
    
    if (name.equals("/ROOT"))
      return "/";
    else
      return name;
  }

  /**
   * Sets the version id.
   */
  protected void setVersion(String version)
  {
    _version = version;
  }

  /**
   * Gets the version id.
   */
  public String getVersion()
  {
    return _version;
  }

  /**
   * versionAlias is true if a versioned web-app is currently acting
   * as the primary web-app.
   */
  public void setVersionAlias(boolean isVersionAlias)
  {
  }

  /**
   * versionAlias is true if a versioned web-app is currently acting
   * as the primary web-app.
   */
  public boolean isVersionAlias()
  {
    return false;
  }

  /**
   * Sets the old version web-app.
   */
  public void setOldWebApp(WebAppController oldWebApp, long expireTime)
  {
    _oldWebAppController = oldWebApp;
    _oldWebAppExpireTime = expireTime;

    WebApp webApp = getDeployInstance();

    if (webApp != null)
      webApp.setOldWebApp(oldWebApp.request(), expireTime);
  }

  /**
   * Adds a version to the controller list.
   */
  /*
  protected WebAppController addVersion(WebAppController controller)
  {
    WebAppVersioningController versioningController
      = new WebAppVersioningController(getContextPath());

    versioningController.addVersion(this);
    versioningController.addVersion(controller);

    return versioningController;
  }
  */

  /**
   * Returns the deploy admin.
   */
  @Override
  protected DeployControllerAdmin<?> getDeployAdmin()
  {
    return _admin;
  }

  @Override
  protected void initEnd()
  {
    if (! getId().startsWith("error/") && ! isVersioning()) {
      _admin = new WebAppAdmin(this);
    }
    
    super.initEnd();
  }

  /**
   * Returns the admin.
   */
  public WebAppAdmin getAdmin()
  {
    return _admin;
  }

  /**
   * Returns true if the controller matches.
   */
  @Override
  public boolean isNameMatch(String url)
  {
    if (CauchoSystem.isCaseInsensitive()) {
      if (url.equalsIgnoreCase(_contextPath))
        return true;
    }
    else {
      if (url.equals(_contextPath))
        return true;
    }

    if (getConfig() != null)
      return getConfig().isUrlMatch(url);
    else
      return false;
  }

  /**
   * Returns the var.
   */
  public Var getVar()
  {
    return new Var();
  }

  /**
   * Any extra steps needed to deploy the webApp.
   */
  protected void protectedWebApp()
    throws Exception
  {
    Path root = getRootDirectory();
    // XXX: need to re-add to control.
    root.lookup("WEB-INF").chmod(0750);
    root.lookup("META-INF").chmod(0750);
  }

  /**
   * Adding any dependencies.
   */
  @Override
  protected void addDependencies()
  {
    super.addDependencies();
  }

  /**
   * Adds a dependent file.
   */
  public void addDepend(Path path)
  {
    _dependPathList.add(path);
  }

  /**
   * Initialize the controller.
   */
  @Override
  protected void initBegin()
  {
    super.initBegin();
  }

  @Override
  protected void fillInitList(ArrayList<DeployConfig> initList)
  {
    if (_container != null) {
      for (WebAppConfig config : _container.getWebAppDefaultList()) {
        if (config.getPrologue() != null)
          initList.add(config.getPrologue());
      }

      for (WebAppConfig config : _container.getWebAppDefaultList())
        initList.add(config);
    }

    super.fillInitList(initList);
  }

  /**
   * Instantiate the webApp.
   */
  @Override
  protected WebApp instantiateDeployInstance()
  {
    return new WebApp(this);
  }

  /**
   * Creates the webApp.
   */
  @Override
  protected void configureInstanceVariables(WebApp webApp)
  {
    InjectManager beanManager = InjectManager.create();
    BeanBuilder<WebApp> factory = beanManager.createBeanFactory(WebApp.class);
    factory.type(WebApp.class);
    factory.type(ServletContext.class);
    // factory.stereotype(CauchoDeploymentLiteral.create());

    beanManager.addBean(factory.singleton(webApp));

    Config.setProperty("webApp", getVar());
    Config.setProperty("app", getVar());

    webApp.setRegexp(_regexpValues);
    webApp.setDynamicDeploy(isDynamicDeploy());

    if (_oldWebAppController != null
        && CurrentTime.getCurrentTime() < _oldWebAppExpireTime) {
      webApp.setOldWebApp(_oldWebAppController.request(),
                          _oldWebAppExpireTime);
    }

    super.configureInstanceVariables(webApp);
  }

  @Override
  protected void onStartComplete()
  {
    super.onStartComplete();
    
    clearCache();
  }

  @Override
  protected void onStop()
  {
    super.onStop();
    
    clearCache();
  }

  /**
   * Clears the 
   */
  public void clearCache()
  {
    _container.clearCache();
  }

  /**
   * Destroy the controller
   */
  @Override
  public boolean destroy()
  {
    if (! super.destroy())
      return false;

    // server/1h03
    /*
    if (_container != null)
      _container.removeWebApp(this);
      */

    return true;
  }

  @Override
  protected void extendJMXContext(Map<String,String> context)
  {
    context.put("WebApp", getMBeanId());
  }

  /**
   * Override to prevent removing of special files.
   */
  @Override
  protected void removeExpandFile(Path path, String relPath)
    throws IOException
  {
    if (relPath.equals("./WEB-INF/resin-web.xml"))
      return;

    super.removeExpandFile(path, relPath);
  }

  public long getLifetimeConnectionCount()
  {
    return _lifetimeConnectionCount;
  }

  public long getLifetimeConnectionTime()
  {
    return _lifetimeConnectionTime;
  }

  public long getLifetimeReadBytes()
  {
    return _lifetimeReadBytes;
  }

  public long getLifetimeWriteBytes()
  {
    return _lifetimeWriteBytes;
  }

  public long getLifetimeClientDisconnectCount()
  {
    return _lifetimeClientDisconnectCount;
  }

  /**
   * Update statistics with the results of one request.
   *
   * @param milliseconds the number of millesconds for the request
   * @param readBytes the number of bytes read
   * @param writeBytes the number of bytes written
   * @param isClientDisconnect true if the request ended with a client DisconnectException
   */
  public void updateStatistics(long milliseconds,
                               int readBytes,
                               int writeBytes,
                               boolean isClientDisconnect)
  {
    synchronized (_statisticsLock) {
      _lifetimeConnectionCount++;
      _lifetimeConnectionTime += milliseconds;
      _lifetimeReadBytes += readBytes;
      _lifetimeWriteBytes += writeBytes;
      if (isClientDisconnect)
        _lifetimeClientDisconnectCount++;
    }
  }

  @Override
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Returns a printable view.
   */
  @Override
  public String toString()
  {
    if (com.caucho.util.CurrentTime.isTest())
      return getClass().getSimpleName() + "[" + getId() + "]";
    else
      return getClass().getSimpleName() + "$" + System.identityHashCode(this) + "[" + getId() + "]";
  }

  /**
   * EL variables for the app.
   */
  public class Var {
    public String getUrl()
    {
      return WebAppController.this.getURL();
    }

    public String getId()
    {
      String id = WebAppController.this.getId();

      if (id != null)
        return id;
      else
        return WebAppController.this.getContextPath();
    }

    public String getName()
    {
      String name;

      if (getWarName() != null)
        name = getWarName();
      else
        name = getContextPath();

      if (name.startsWith("/"))
        return name;
      else
        return "/" + name;
    }

    public Path getAppDir()
    {
      return WebAppController.this.getRootDirectory();
    }

    public Path getDocDir()
    {
      return WebAppController.this.getRootDirectory();
    }

    public Path getRoot()
    {
      return WebAppController.this.getRootDirectory();
    }

    public Path getRootDir()
    {
      return WebAppController.this.getRootDirectory();
    }

    public String getContextPath()
    {
      return WebAppController.this.getContextPath();
    }

    public ArrayList<String> getRegexp()
    {
      return _regexpValues;
    }

    public String getVersion()
    {
      return _version;
    }

    @Override
    public String toString()
    {
      return "WebApp[" + getId() + "]";
    }
  }
}
