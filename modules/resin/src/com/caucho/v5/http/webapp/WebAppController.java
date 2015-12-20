/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.config.Config;
import com.caucho.v5.config.inject.InjectManager;
import com.caucho.v5.deploy.ConfigInstanceBuilder;
import com.caucho.v5.deploy.DeployController;
import com.caucho.v5.deploy.DeployControllerAdmin;
import com.caucho.v5.deploy.DeployControllerEnvironment;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.deploy.DeployInstanceBuilder;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.http.pod.PodConfigApp;
import com.caucho.v5.inject.Module;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.Crc64;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.WriteStream;

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
  extends DeployControllerEnvironment<WebApp,WebAppConfig>
{
  private static final Logger log
    = Logger.getLogger(WebAppController.class.getName());

  private final WebAppContainer _container;

  // private WebAppController _parent;

  private String _idTail;
  
  // The context path is the URL prefix for the web-app
  private String _contextPath;
  private String _version = "";

  private String _warName;

  // regexp values
  private ArrayList<String> _regexpValues;

  private boolean _isInheritSession;
  private boolean _isDynamicDeploy;
  private boolean _isUseContainerDefaults = true;
  
  private String _podName;
  private ArrayList<PodConfigApp> _podConfigList = new ArrayList<>();
  private ArrayList<Path> _archivePathList = new ArrayList<>();

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
  
  /*
  public WebAppController(Path rootDirectory,
                          WebAppContainer container,
                          String contextPath)
  {
    this(calculateId(container, contextPath), 
         rootDirectory,
         container,
         contextPath);
  }
  */

  public WebAppController(String id,
                          Path rootDirectory,
                          WebAppContainer container,
                          String contextPath)
  {
    super(id, rootDirectory, container.getClassLoader());
    
    Objects.requireNonNull(container);
    Objects.requireNonNull(container.getHost());

    _container = container;
    
    if (contextPath.equals("/")) {
      // servlet/1338
      contextPath = "";
    }

    _contextPath = contextPath;
     
    _idTail = calculateIdTail(getId());
    
    // setArchiveExtension(".war");
  }
  
  private static String calculateIdTail(String id)
  {
    int p1 = id.indexOf('/');
    //String type = id.substring(0, p1);
    int p2 = id.indexOf('/', p1 + 1);
    //String host = id.substring(p2 + 1, p3);
      
    return id.substring(p2);
  }
  
  public static String calculateId(WebAppContainer container,
                                    String contextPath)
  {
    if (contextPath.equals("") || contextPath.equals("/")) {
      contextPath = "/ROOT";
    }
    
    // String cluster = container.getHttpContainer().getClusterName();
    String hostId = container.getHost().getIdTail();

    return "webapps/" + hostId + contextPath;
  }
  
  /*
  @Override
  protected WebAppHandle createHandle()
  {
    return new WebAppHandle(this);
  }
  */
  
  void initConfig(WebAppConfig config)
  {
    setPrologue(config.getPrologue());
    setStartupPriority(config.getStartupPriority());
    setControllerType(config.getControllerType());
  }
  
  public void setPodName(String podName)
  {
    _podName = podName;
  }
  
  public String getPodName()
  {
    return _podName;
  }

  public void addPodConfigDefault(PodConfigApp podConfig)
  {
    _podConfigList.add(podConfig);
    
    for (Path archivePath : podConfig.getArchivePaths()) {
      addArchivePath(archivePath);
    }
  }
  
  private void addArchivePath(Path archivePath)
  {
    _archivePathList.add(archivePath);
    
    addDepend(archivePath);
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
  
  @Override
  public String getArchiveExtension()
  {
    return ".war";
  }

  /**
   * Returns the webApp's context path
   */
  public String getContextPath(String uri)
  {
    if (getConfig() == null || getConfig().getURLRegexp() == null) {
      return getContextPath();
    }

    Pattern regexp = getConfig().getURLRegexp();
    Matcher matcher = regexp.matcher(uri);

    int tail = 0;
    while (tail >= 0 && tail <= uri.length()) {
      String prefix = uri.substring(0, tail);

      matcher.reset(prefix);

      if (matcher.find() && matcher.start() == 0) {
        return matcher.group();
      }

      if (tail < uri.length()) {
        tail = uri.indexOf('/', tail + 1);
        
        if (tail < 0) {
          tail = uri.length();
        }
      }
      else
        break;
    }

    return _contextPath;
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
  /*
  public WebAppController getParent()
  {
    return _parent;
  }
  */

  /**
   * Returns the web-app container.
   */
  public WebAppContainer getContainer()
  {
    return _container;
  }
  
  public HttpContainerServlet getHttpContainer()
  {
    return _container.getHttpContainer();
  }

  /**
   * Sets the parent controller.
   */
  /*
  public void setParentWebApp(WebAppController parent)
  {
    _parent = parent;
  }
  */

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
  /*
  public WebApp getWebApp()
  {
    return getDeployInstance();
  }
  */

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

  /**
   * Set true for a dynamically deployed webApp.
   */
  public void setUseContainerDefaults(boolean isEnable)
  {
    _isUseContainerDefaults = isEnable;
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
  
  @Override
  protected boolean isBartenderArchive()
  {
    if (super.isBartenderArchive()) {
      return true;
    }
    
    return (_archivePathList.size() > 0);
  }
  
  @Override
  protected long calculateDigest()
  {
    long crc = super.calculateDigest();
    
    for (Path path : _archivePathList) {
      crc = Crc64.generate(crc, path.getCrc64());
    }
    
    return crc;
  }
    
  @Override
  protected void extractBartender(Path rootDir)
    throws IOException
  {
    super.extractBartender(rootDir);

    for (Path path : _archivePathList) {
      expandBarToPath(path, rootDir);
    }
  }
  
  private void expandBarToPath(Path src, Path dst)
    throws IOException
  {
    try (ReadStream is = src.openRead()) {
      try (ZipInputStream zis = new ZipInputStream(is)) {
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
          String name = entry.getName();

          if (entry.isDirectory()) {
            continue;
          }

          Path subPath;
          
          if (name.startsWith("web/")) {
            subPath = dst.lookup(name.substring("web/".length()));
          }
          else {
            subPath = dst.lookup("WEB-INF").lookup(name);
          }
          
          subPath.getParent().mkdirs();

          try (WriteStream os = subPath.openWrite()) {
            os.writeStream(zis);
          }

          zis.closeEntry();
        }
      }
    }
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
  /*
  public void setOldWebApp(WebAppController oldWebApp, long expireTime)
  {
    _oldWebAppController = oldWebApp;
    _oldWebAppExpireTime = expireTime;

    WebApp webApp = getDeployInstance();

    if (webApp != null) {
      webApp.getDispatcher().setOldWebApp(oldWebApp.request(), expireTime);
    }
  }
  */

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
  protected DeployControllerAdmin<WebApp,WebAppController>
  getDeployAdmin(DeployHandle<WebApp> handle)
  {
    if (! getId().startsWith("error/") && ! isVersioning()) {
      return new WebAppAdmin(handle);
    }
    else {
      return null;
    }
    
    // return _admin;
  }
  
  @Override
  public void merge(DeployController<WebApp> oldControllerDeploy)
  {
    super.merge(oldControllerDeploy);
    
    WebAppController oldController = (WebAppController) oldControllerDeploy;
  }

  @Override
  protected void initEnd()
  {
    /*
    if (! getId().startsWith("error/") && ! isVersioning()) {
      _admin = new WebAppAdmin(this);
    }
    */
    
    super.initEnd();
  }

  @Override
  protected void onStartComplete()
  {
    super.onStartComplete();
    
    ServiceManagerAmp manager = AmpSystem.getCurrentManager();

    OnWebAppStart onStart
      = manager.lookup("event:///" + OnWebAppStart.class.getName())
               .as(OnWebAppStart.class);

    onStart.onWebAppStart(getId());
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
    if (CauchoUtil.isCaseInsensitive()) {
      if (url.equalsIgnoreCase(_contextPath)) {
        return true;
      }
    }
    else {
      if (url.equals(_contextPath)) {
        return true;
      }
    }

    if (getConfig() != null) {
      return getConfig().isUrlMatch(url);
    }
    /*
    else if (url.equals(_podAlias)) {
      return true;
    }
    */
    else {
      return false;
    }
  }

  /**
   * Returns the var.
   */
  public VarWebApp getVar()
  {
    return new VarWebApp();
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
  /*
  @Override
  protected void initDependencies()
  {
    super.initDependencies();
  }
  */

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
  protected void fillInitList(ConfigInstanceBuilder builder)
  {
    if (_isUseContainerDefaults) {
      for (WebAppConfig config : _container.getWebAppDefaultList()) {
        if (config.getPrologue() != null) {
          config.getPrologue().configure(builder);
        }
      }
      
      // XXX: need to split up prologue

      for (WebAppConfig config : _container.getWebAppDefaultList()) {
        config.getBuilderProgram().configure(builder);
      }
    }

    super.fillInitList(builder);
  }

  /**
   * Instantiate the webApp.
   */
  @Override
  protected DeployInstanceBuilder<WebApp> createInstanceBuilder()
  {
    return getHttpContainer().createWebAppBuilder(this);
  }

  /**
   * Creates the webApp.
   */
  @Override
  protected void configureInstanceVariables(DeployInstanceBuilder<WebApp> builder)
  {
    InjectManager inject = InjectManager.current();
    
    //CandiManager beanManager = CandiManager.create();
    //BeanBuilder<WebApp> factory = beanManager.createBeanBuilder(WebApp.class);
    //factory.type(WebApp.class);
    // factory.type(ServletContext.class);
    // factory.stereotype(CauchoDeploymentLiteral.create());
    
    WebAppBuilder webAppBuilder = (WebAppBuilder) builder;
    
    WebApp webApp = webAppBuilder.getWebApp();

    // XXX:
    // beanManager.addBean(factory.singleton(webApp));

    Config.setProperty("webApp", getVar());
    Config.setProperty("app", getVar());

    webAppBuilder.setRegexp(_regexpValues);
    webAppBuilder.setDynamicDeploy(isDynamicDeploy());

    /*
    if (_oldWebAppController != null
        && CurrentTime.getCurrentTime() < _oldWebAppExpireTime) {
      webApp.getDispatcher().setOldWebApp(_oldWebAppController.request(),
                                          _oldWebAppExpireTime);
    }
    */

    super.configureInstanceVariables(builder);
  }

  /*
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
  */

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
    if (com.caucho.v5.util.CurrentTime.isTest())
      return getClass().getSimpleName() + "[" + getId() + "]";
    else
      return getClass().getSimpleName() + "$" + System.identityHashCode(this) + "[" + getId() + "]";
  }

  /**
   * EL variables for the app.
   */
  public class VarWebApp {
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
