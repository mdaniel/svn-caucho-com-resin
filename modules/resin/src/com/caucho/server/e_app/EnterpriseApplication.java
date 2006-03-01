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
 * @author Scott Ferguson
 */

package com.caucho.server.e_app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Hashtable;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import javax.naming.Context;

import javax.servlet.jsp.el.VariableResolver;

import org.xml.sax.SAXException;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Jar;
import com.caucho.vfs.Depend;

import com.caucho.java.WorkDir;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.Environment;

import com.caucho.loader.enhancer.EnhancingClassLoader;

import com.caucho.log.Log;

import com.caucho.naming.Jndi;

import com.caucho.relaxng.CompactVerifierFactoryImpl;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.BuilderProgram;
import com.caucho.config.types.PathBuilder;

import com.caucho.jmx.Jmx;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.server.deploy.EnvironmentDeployInstance;

import com.caucho.server.webapp.ApplicationContainer;
import com.caucho.server.webapp.WebAppController;

import com.caucho.ejb.EJBServerInterface;

/**
 * An enterprise application (ear)
 */
public class EnterpriseApplication
  implements EnvironmentBean, EnvironmentDeployInstance {
  static final L10N L = new L10N(EnterpriseApplication.class);
  static final Logger log = Log.open(EnterpriseApplication.class);

  /*
  protected static EnvironmentLocal<EJBServerInterface> _localServer
    = new EnvironmentLocal<EJBServerInterface>("caucho.ejb-server");
  */

  private EnvironmentClassLoader _loader;

  private String _name;

  private String _ejbServerJndiName = "java:comp/env/cmp";
  
  private Path _rootDir;

  private Path _earPath;

  private String _prefix = "";

  private EarDeployController _controller;

  private Path _webappsPath;

  private ApplicationConfig _config;

  private ApplicationContainer _container;

  // private WarDirApplicationGenerator _warDeploy;

  private ArrayList<Path> _ejbPaths
    = new ArrayList<Path>();
  
  private ArrayList<WebAppController> _webApps
    = new ArrayList<WebAppController>();

  private Throwable _configException;

  private final Lifecycle _lifecycle;

  /**
   * Creates the application.
   */
  EnterpriseApplication(ApplicationContainer container,
			EarDeployController controller, String name)
  {
    _container = container;
    
    _controller = controller;
    _name = name;

    ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
    
    _loader = new EnvironmentClassLoader(container.getClassLoader());
    _loader.setId("EnterpriseApplication[" + name + "]");

    _webappsPath = _controller.getRootDirectory().lookup("webapps");
    WorkDir.setLocalWorkDir(_controller.getRootDirectory().lookup("META-INF/work"),
			    _loader);
    
    _lifecycle = new Lifecycle(log, toString(), Level.INFO);

    if (controller.getArchivePath() != null)
      Environment.addDependency(new Depend(controller.getArchivePath()), _loader);
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
    _loader.setId("EnterpriseApplication[" + name + "]");
  }

  /**
   * Gets the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the ejb-server jndi name.
   */
  public void setEjbServerJndiName(String name)
  {
    _ejbServerJndiName = name;
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path rootDir)
  {
    _rootDir = rootDir;
  }

  /**
   * Sets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDir;
  }

  /**
   * Returns the class loader.
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Sets the class loader.
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Sets the path to the .ear file
   */
  public void setEarPath(Path earPath)
  {
    _earPath = earPath;
  }

  /**
   * Sets the path to the expanded webapps
   */
  public void setWebapps(Path webappsPath)
  {
    _webappsPath = webappsPath;
  }

  /**
   * Sets the prefix URL for web applications.
   */
  public void setPrefix(String prefix)
  {
    _prefix = prefix;
  }
  
  /**
   * Sets the id
   */
  public void setId(String id)
  {
  }
  
  /**
   * Sets the application version.
   */
  public void setVersion(String version)
  {
  }
  
  /**
   * Sets the schema location
   */
  public void setSchemaLocation(String schema)
  {
  }

  /**
   * Sets the display name.
   */
  public void setDisplayName(String name)
  {
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the icon.
   */
  public void setIcon(Icon icon)
  {
  }
  
  /**
   * Adds a module.
   */
  public Module createModule()
  {
    return new Module();
  }

  /**
   * Adds a security role.
   */
  public void addSecurityRole(SecurityRole role)
  {
  }

  /**
   * Returns true if it's modified.
   */
  public boolean isModified()
  {
    return _loader.isModified();
  }

  /**
   * Returns true if it's modified.
   */
  public boolean isModifiedNow()
  {
    return _loader.isModifiedNow();
  }

  /**
   * Returns true if it's modified.
   */
  public boolean isDeployError()
  {
    return _configException != null;
  }

  /**
   * Returns true if the application is idle.
   */
  public boolean isDeployIdle()
  {
    return false;
  }

  /**
   * Sets the config exception.
   */
  public void setConfigException(Throwable e)
  {
    _configException = e;

    for (WebAppController controller : _webApps) {
      controller.setConfigException(e);
    }
  }

  /**
   * Gets the config exception.
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Configures the application.
   */
  public void init()
    throws Exception
  {
    if (! _lifecycle.toInit())
      return;
      
    log.fine(this + " initializing");
      
    Vfs.setPwd(_rootDir, _loader);

    if (_ejbPaths.size() != 0) {
      Object obj = Jndi.lookup(_ejbServerJndiName);

      if (! (obj instanceof Context))
	throw new ConfigException(L.l("Expected <ejb-server> configured at '{0}'",
				      _ejbServerJndiName));

      obj = ((Context) obj).lookup("resin-ejb-server");

      if (! (obj instanceof EJBServerInterface))
	throw new ConfigException(L.l("Expected <ejb-server> configured at '{0}'",
				      _ejbServerJndiName + "/resin-ejb-server"));
      
      EJBServerInterface ejbServer = (EJBServerInterface) obj;

      if (ejbServer == null && _ejbPaths.size() != 0)
	throw new ConfigException(L.l("<ejb> module needs a configured <ejb-server>"));

      if (ejbServer != null) {
	for (Path path : _ejbPaths)
	  ejbServer.addEJBJar(path);

	ejbServer.initEJBs();
      }
    }

    // updates the invocation caches
    if (_container != null)
      _container.clearCache();
  }

  /**
   * Configures the application.
   */
  public void start()
  {
    if (! _lifecycle.toStarting())
      return;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      getClassLoader().start();

      /* XXX: double start?
      for (int i = 0; i < _webApps.size(); i++) {
	WebAppController controller = _webApps.get(i);

	try {
	  controller.start();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }
      */

      for (WebAppController webApp : _webApps) {
	_container.getApplicationGenerator().update(webApp.getContextPath());
      }
    } finally {
      _lifecycle.toActive();
      
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns any matching web-app.
   */
  public WebAppController findWebAppEntry(String name)
  {
    for (int i = 0; i < _webApps.size(); i++) {
      WebAppController controller = _webApps.get(i);

      if (controller.isNameMatch(name))
	return controller;
    }

    return null;
  }

  private void addDepend(Path path)
  {
    _loader.addDependency(new com.caucho.vfs.Depend(path));
  }

  /**
   * Returns the webapps for the enterprise-application.
   */
  public ArrayList<WebAppController> getApplications()
  {
    return _webApps;
  }
  
  /**
   * Stops the e-application.
   */
  public void stop()
  {
    if (! _lifecycle.toStopping())
      return;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      //log.info(this + " stopping");

      _loader.stop();
      
      //log.fine(this + " stopped");
    } finally {
      _lifecycle.toStop();
      
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * destroys the e-application.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      log.fine(this + " destroying");

      ArrayList<WebAppController> webApps = _webApps;
      _webApps = null;

      if (webApps != null) {
	for (WebAppController webApp : webApps) {
	  _container.getApplicationGenerator().update(webApp.getContextPath());
	}
      }
    } finally {
      thread.setContextClassLoader(oldLoader);

      _loader.destroy();

      log.fine(this + " destroyed");
    }
  }

  public String toString()
  {
    return "EnterpriseApplication[" + getName() + "]";
  }

  public class Module {
    /**
     * Sets the module id.
     */
    public void setId(String id)
    {
    }
    
    /**
     * Creates a new web module.
     */
    public void addWeb(WebModule web)
      throws Exception
    {
      String webUri = web.getWebURI();
      String contextUrl = web.getContextRoot();
      Path path = _rootDir.lookup(webUri);

      if (contextUrl == null)
	contextUrl = webUri;

      WebAppController controller = null;
      if (webUri.endsWith(".war")) {
	// server/2a16
	String name = webUri.substring(0, webUri.length() - 4);
	int p = name.lastIndexOf('/');
	if (p > 0)
	  name = name.substring(p + 1);

	// XXX:
	if (contextUrl.equals(""))
	  contextUrl = "/" + name;

	if (contextUrl.endsWith(".war"))
	  contextUrl = contextUrl.substring(0, contextUrl.length() - 4);

	Path expandPath = _webappsPath;
	expandPath.mkdirs();

	controller = new WebAppController(contextUrl,
					  expandPath.lookup(name),
					  _container);

	controller.setArchivePath(path);
      } else {
	// server/2a15
	if (contextUrl.equals("")) {
	  String name = webUri;
	  int p = name.lastIndexOf('/');
	  if (p > 0)
	    name = name.substring(p + 1);
	  contextUrl = "/" + name;
	}

	// server/2a17
	if (contextUrl.endsWith(".war"))
	  contextUrl = contextUrl.substring(0, contextUrl.length() - 4);
	
	controller = new WebAppController(contextUrl, path, _container);
      }

      controller.setDynamicDeploy(true);
      if (_configException != null)
	controller.setConfigException(_configException);

      controller.setManifestClassLoader(_loader);

      _webApps.add(controller);
    }
    
    /**
     * Adds a new ejb module.
     */
    public void addEjb(Path path)
      throws Exception
    {
      _ejbPaths.add(path);
      
      _loader.addJar(path);
      // ejb/0853
      _loader.addJarManifestClassPath(path);
    }
    
    /**
     * Adds a new java module.
     */
    public void addJava(Path path)
      throws ConfigException
    {
      if (! path.canRead())
	throw new ConfigException(L.l("<java> module {0} must be a valid path.",
				      path));
    }
    
    /**
     * Adds a new connector
     */
    public void addConnector(String path)
    {
    }
    
    /**
     * Adds a new alt-dd module.
     */
    public void addAltDD(String path)
    {
    }
  }
}
