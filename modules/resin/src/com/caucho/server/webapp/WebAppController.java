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

package com.caucho.server.webapp;

import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;

import javax.management.ObjectName;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import org.iso_relax.verifier.Schema;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.log.Log;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.types.PathBuilder;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.relaxng.CompactVerifierFactoryImpl;

import com.caucho.loader.EnvironmentListener;

import com.caucho.jmx.Jmx;
import com.caucho.jmx.IntrospectionMBean;

import com.caucho.server.session.SessionManager;

import com.caucho.server.deploy.ExpandDeployController;

import com.caucho.server.webapp.mbean.WebAppMBean;

/**
 * A configuration entry for a web-app.
 */
public class WebAppController extends ExpandDeployController<Application>
  implements EnvironmentListener {
  private static final L10N L = new L10N(WebAppController.class);
  private static final Logger log = Log.open(WebAppController.class);

  private static Schema _webXmlSchema;
  
  private ApplicationContainer _container;

  private WebAppController _parent;

  // The entry id
  private String _id;
  
  // The context path is the URL prefix for the web-app
  private String _contextPath;
  
  // The JMX identity
  private LinkedHashMap<String,String> _jmxContext;

  private Object _mbean;

  private ObjectName _mbeanName;

  // The default configurations
  private ArrayList<WebAppConfig> _webAppDefaults =
    new ArrayList<WebAppConfig>();
  
  // The configuration
  private WebAppConfig _config;

  private BuilderProgram _initProgram;

  private VariableResolver _variableResolver;

  // The variable mapping
  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  // regexp values
  private ArrayList<String> _regexpValues;
  
  private Application _app;
  private boolean _isInheritSession;
  private boolean _isDynamicDeploy;
  
  private ArrayList<Path> _dependPathList = new ArrayList<Path>();

  private boolean _isInit;

  private String _sourceType = "unknown";
  private Throwable _configException;

  public WebAppController(ApplicationContainer container, String contextPath)
  {
    _container = container;

    setId(contextPath);
    
    if (contextPath.length() > 0 && ! contextPath.startsWith("/"))
      contextPath = '/' + contextPath;
    
    setName(contextPath);

    if (contextPath.equals("/ROOT") ||
        CauchoSystem.isCaseInsensitive() &&
        contextPath.equalsIgnoreCase("/root") ||
        contextPath.equals("/"))
      contextPath = "";
      
    setContextPath(contextPath);

    if (contextPath.equals("")) {
      if (CauchoSystem.isCaseInsensitive())
        setId("root");
      else
        setId("ROOT");
    }

    VariableResolver parentResolver = EL.getEnvironment(getParentClassLoader());
    _variableResolver = new MapVariableResolver(_variableMap, parentResolver);

    _jmxContext = Jmx.copyContextProperties(getParentClassLoader());

    /*
    if (_jmxContext.get("J2EEApplication") == null)
      _jmxContext.put("J2EEApplication", "null");
    */
  }

  /**
   * Sets the id.
   */
  public void setId(String id)
  {
    _id = id;
  }

  /**
   * Gets the id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Returns the application's context path
   */
  public String getContextPath()
  {
    return _contextPath;
  }

  /**
   * Sets the application's context path
   */
  public void setContextPath(String contextPath)
  {
    if (! contextPath.equals("") && ! contextPath.startsWith("/"))
      contextPath = "/" + contextPath;

    if (contextPath.endsWith("/"))
      contextPath = contextPath.substring(0, contextPath.length() - 1);
    
    _contextPath = contextPath;
  }

  /**
   * Returns the application's context path
   */
  public String getContextPath(String uri)
  {
    if (_config == null || _config.getURLRegexp() == null)
      return _contextPath;

    Pattern regexp = _config.getURLRegexp();
    Matcher matcher = regexp.matcher(uri);

    int tail = 0;
    while (tail >= 0 && tail <= uri.length()) {
      String prefix = uri.substring(0, tail);

      matcher.reset(prefix);
      
      if (matcher.find() && matcher.start() == 0)
	return matcher.group();

      if (tail < uri.length()) {
	tail = uri.indexOf('/', tail + 1);
	if (tail < 0)
	  tail = uri.length();
      }
      else
	break;
    }

    return _contextPath;
  }
  
  /**
   * Gets the URL
   */
  public String getURL()
  {
    if (_parent != null)
      return _parent.getURL() + _contextPath;
    else
      return _contextPath;
  }

  /**
   * Returns the parent entry.
   */
  public WebAppController getParent()
  {
    return _parent;
  }

  /**
   * Sets the parent entry.
   */
  public void setParentWebApp(WebAppController parent)
  {
    _parent = parent;
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
   * Sets the WebAppConfig
   */
  public void setWebAppConfig(WebAppConfig config)
  {
    _config = config;
    
    if (config != null) {
      addWebAppDefault(config);
    }
  }

  /**
   * Gets the WebAppConfig
   */
  public WebAppConfig getWebAppConfig()
  {
    return _config;
  }

  /**
   * Adds a WebAppDefault.
   */
  public void addWebAppDefault(WebAppConfig config)
  {
    if (! _webAppDefaults.contains(config)) {
      _webAppDefaults.add(config);

      if (config.getStartupMode() != null)
	setStartupMode(config.getStartupMode());

      if (config.getRedeployMode() != null)
	setRedeployMode(config.getRedeployMode());
    }
  }

  /**
   * Returns the path variable map.
   */
  public HashMap<String,Object> getVariableMap()
  {
    return _variableMap;
  }

  /**
   * Sets the regexp values.
   */
  public void setRegexpValues(ArrayList<String> values)
  {
    _regexpValues = values;
  }

  /**
   * True for inherit-session applications.
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
   * Returns the application's resin.conf configuration node.
   */
  public BuilderProgram getInitProgram()
  {
    return _initProgram;
  }

  /**
   * Sets the application's init program
   */
  public void setInitProgram(BuilderProgram initProgram)
  {
    _initProgram = initProgram;
  }

  /**
   * Returns the application object.
   */
  public Application getApplication()
  {
    return _app;
  }

  /**
   * Set true for a dynamically deployed application.
   */
  public void setDynamicDeploy(boolean isDynamicDeploy)
  {
    _isDynamicDeploy = isDynamicDeploy;
  }

  /**
   * Returns true for a dynamically deployed application.
   */
  public boolean isDynamicDeploy()
  {
    return _isDynamicDeploy;
  }

  /**
   * Returns the mbean.
   */
  Object getMBean()
  {
    return _mbean;
  }

  /**
   * Sets the config exception (e.g. from a .ear)
   */
  public void setConfigException(Throwable e)
  {
    _configException = e;
  }

  /**
   * Initialize the entry.
   */
  public boolean init()
  {
    if (! super.init())
      return false;
    
    try {
      if (_mbeanName == null) {
	LinkedHashMap<String,String> properties;

	properties = Jmx.copyContextProperties(_container.getClassLoader());

	/*
	if (_jmxContext.get("J2EEServer") != null)
	  properties.put("J2EEServer", _jmxContext.get("J2EEServer"));
	if (_jmxContext.get("J2EEApplication") != null)
	  properties.put("J2EEApplication", _jmxContext.get("J2EEApplication"));
	properties.put("j2eeType", "WebModule");
	*/

	String name = _contextPath;
	if (_contextPath.equals(""))
	  name = "/";

	properties.put("type", "WebApp");
	properties.put("name", name);

	_mbean = new IntrospectionMBean(new Admin(), WebAppMBean.class);

	_mbeanName = Jmx.getObjectName("resin", properties);
      
	Jmx.register(_mbean, _mbeanName);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return true;
  }

  /**
   * Returns true if the entry matches.
   */
  public boolean isNameMatch(String url)
  {
    if (CauchoSystem.isCaseInsensitive())
      return url.equalsIgnoreCase(_contextPath);
    else
      return url.equals(_contextPath);
  }

  /**
   * Merges two entries.
   */
  protected WebAppController merge(WebAppController newEntry)
  {
    if (_config != null && _config.getURLRegexp() != null)
      return newEntry;
    else if (newEntry._config != null &&
	     newEntry._config.getURLRegexp() != null)
      return this;
    else {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
	thread.setContextClassLoader(getParentClassLoader());

	WebAppController mergedEntry = new WebAppController(_container, _contextPath);

	mergedEntry.setRootDirectory(getRootDirectory());
      
	mergedEntry._webAppDefaults.addAll(_webAppDefaults);
	mergedEntry._webAppDefaults.addAll(newEntry._webAppDefaults);
	if (newEntry._config != null)
	  mergedEntry._webAppDefaults.add(newEntry._config);
	mergedEntry._config = _config;
	if (newEntry._config != null)
	  _config = newEntry._config;

	if (getArchivePath() != null)
	  mergedEntry.setArchivePath(getArchivePath());
      
	if (newEntry.getArchivePath() != null)
	  mergedEntry.setArchivePath(newEntry.getArchivePath());

	mergedEntry.setStartupMode(getStartupMode());
	mergedEntry.mergeStartupMode(newEntry.getStartupMode());

	mergedEntry.setRedeployMode(getRedeployMode());
	mergedEntry.mergeRedeployMode(newEntry.getRedeployMode());

	return mergedEntry;
      } finally {
	thread.setContextClassLoader(oldLoader);
      }
    }
  }
  
  /**
   * Returns the application object.
   */
  public boolean destroy()
  {
    if (! super.destroy())
      return false;
    
    Environment.removeEnvironmentListener(this, getParentClassLoader());

    _container.removeWebApp(this);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());
    
      try {
	ObjectName mbeanName = _mbeanName;
	_mbeanName = null;

	if (mbeanName != null)
	  Jmx.unregister(mbeanName);
      } catch (Exception e) {
	log.log(Level.FINER, e.toString(), e);
      }
    
      synchronized (this) {
	Application app = _app;

	if (_isInit && app != null) {
	  _app = null;
	  _isInit = false;

	  app.destroy();
	}
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return true;
  }

  /**
   * Any extra steps needed to deploy the application.
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
  protected void addDependencies()
    throws Exception
  {
  }
  
  /**
   * Sets the application object.
   */
  public void setApplication(Application application)
  {
    _app = application;
  }

  /**
   * Adds a dependent file.
   */
  public void addDepend(Path path)
  {
    _dependPathList.add(path);
  }

  /**
   * Instantiate the application.
   */
  protected Application instantiateDeployInstance()
  {
    return new Application(_container, this, _contextPath);
  }

  /**
   * Creates the application.
   */
  protected void configureInstance(Application app)
    throws Throwable
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    Path appDir = null;
    try {
      thread.setContextClassLoader(app.getClassLoader());

      log.fine(app + " initializing");

      // set from external error, like .ear
      app.setConfigException(_configException);
      
      Map<String,Object> varMap = app.getVariableMap();
      varMap.putAll(_variableMap);
      app.setRegexp(_regexpValues);
      _app = app;
      
      app.setDynamicDeploy(isDynamicDeploy());

      appDir = getAppDir();

      if (appDir == null)
	throw new NullPointerException("Null app dir");

      if (! appDir.isFile()) {
      }
      else if (appDir.getPath().endsWith(".jar") ||
	       appDir.getPath().endsWith(".war")) {
	throw new ConfigException(L.l("document-directory `{0}' must specify a directory.  It may not be a .jar or .war.",
				      appDir.getPath()));
      }
      else
	throw new ConfigException(L.l("app-dir `{0}' may not be a file.  app-dir must specify a directory.",
				      appDir.getPath()));

      app.setAppDir(appDir);
      varMap.put("app-dir", appDir);

      ArrayList<WebAppConfig> initList = new ArrayList<WebAppConfig>();
      
      if (_container != null) {
	ArrayList<WebAppConfig> defaultList;
	defaultList = _container.getWebAppDefaultList();

	for (int i = 0; i < defaultList.size(); i++) {
	  WebAppConfig init = defaultList.get(i);

	  initList.add(init);
	}
      }

      for (int i = 0; i < _webAppDefaults.size(); i++) {
	WebAppConfig init = _webAppDefaults.get(i);

	initList.add(init);
      }

      thread.setContextClassLoader(app.getClassLoader());
      Vfs.setPwd(appDir);

      for (int i = 0; i < _dependPathList.size(); i++) {
	Path path = _dependPathList.get(i);

	Environment.addDependency(path);
      }

      if (getArchivePath() != null)
	Environment.addDependency(getArchivePath());
	
      addDependencies();

      for (int i = 0; i < initList.size(); i++) {
	WebAppConfig config = initList.get(i);
	BuilderProgram program = config.getBuilderProgram();

	if (program != null)
	  program.configure(app);
      }

      app.init();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  protected Path getAppDir()
    throws ELException
  {
    Path appDir = null;

    if (appDir == null)
      appDir = getRootDirectory();
    
    if (appDir == null && _config != null) {
      String path = _config.getAppDir();
      
      if (path != null)
        appDir = PathBuilder.lookupPath(path, _variableResolver);
    }

    /*
    if (appDir == null)
      appDir = _cfgAppDir;
    */

    if (appDir == null && _container != null)
      appDir = _container.getDocumentDirectory().lookup("./" + _contextPath);

    if (appDir == null && _app != null)
      appDir = _app.getAppDir();

    return appDir;
  }

  public Path getArchivePath()
  {
    Path path = super.getArchivePath();

    if (path != null)
      return path;
    
    if (_config != null) {
      String pathString = _config.getArchivePath();
      
      if (pathString != null) {
	try {
	  path = PathBuilder.lookupPath(pathString, _variableResolver);
	} catch (ELException e) {
	  throw new RuntimeException(e);
	}
      }

      setArchivePath(path);
    }

    return path;
  }

  /**
   * Override to prevent removing of special files.
   */
  protected void removeExpandFile(Path path, String relPath)
    throws IOException
  {
    if (relPath.equals("./WEB-INF/resin-web.xml"))
      return;

    super.removeExpandFile(path, relPath);
  }

  protected Schema getWebXmlSchema()
  {
    if (_webXmlSchema == null) {
      try {
        MergePath schemaPath = new MergePath();
        schemaPath.addClassPath();
      
        Path path = schemaPath.lookup("com/caucho/server/webapp/resin-web-xml.rnc");
        if (path.canRead()) {
          ReadStream is = path.openRead();

          try {
            // VerifierFactory factory = VerifierFactory.newInstance("http://caucho.com/ns/compact-relax-ng/1.0");
          
            CompactVerifierFactoryImpl factory;
            factory = new CompactVerifierFactoryImpl();

            _webXmlSchema = factory.compileSchema(is);
          } finally {
            is.close();
          }
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return _webXmlSchema;
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    try {
      start();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    stop();
  }
  
  /**
   * Returns a printable view.
   */
  public String toString()
  {
    return "WebAppController$" + System.identityHashCode(this) + "[" + _contextPath + "]";
  }

  public class Admin implements WebAppMBean {
    /**
     * Returns the web-app directory.
     */
    public String getRootDirectory()
    {
      try {
	Path path = getAppDir();

	if (path != null)
	  return path.getNativePath();
	else
	  return null;
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);

	return null;
      }
    }

    /**
     * Returns the context path
     */
    public String getContextPath()
    {
      return _contextPath;
    }

    /**
     * Returns the controller state.
     */
    public String getState()
    {
      return WebAppController.this.getState();
    }

    /**
     * Returns the active sessions.
     */
    public int getActiveSessionCount()
    {
      Application app = getApplication();

      if (app == null)
	return 0;

      SessionManager manager = app.getSessionManager();
      if (manager == null)
	return 0;

      return manager.getActiveSessionCount();
    }
  
    /**
     * Returns the time of the last start
     */
    public Date getStartTime()
    {
      return new Date(WebAppController.this.getStartTime());
    }

    /**
     * Stops the server.
     */
    public void stop()
      throws Exception
    {
      WebAppController.this.stop();
    }

    /**
     * Starts the server.
     */
    public void start()
      throws Exception
    {
      WebAppController.this.start();
    }

    /**
     * Restarts the server.
     */
    public void update()
      throws Exception
    {
      WebAppController.this.update();
    }
  }
}
