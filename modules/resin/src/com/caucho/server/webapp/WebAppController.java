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
import javax.management.JMException;
import javax.management.MalformedObjectNameException;

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

import com.caucho.relaxng.CompactVerifierFactoryImpl;

import com.caucho.loader.EnvironmentListener;

import com.caucho.jmx.Jmx;
import com.caucho.jmx.IntrospectionMBean;

import com.caucho.server.deploy.EnvironmentDeployController;

import com.caucho.server.webapp.mbean.WebAppMBean;

/**
 * A configuration entry for a web-app.
 */
public class WebAppController
  extends EnvironmentDeployController<Application,WebAppConfig> {
  private static final L10N L = new L10N(WebAppController.class);
  private static final Logger log = Log.open(WebAppController.class);

  private ApplicationContainer _container;

  private WebAppController _parent;

  // The context path is the URL prefix for the web-app
  private String _contextPath;

  // regexp values
  private ArrayList<String> _regexpValues;
  
  private boolean _isInheritSession;
  private boolean _isDynamicDeploy;
  
  private ArrayList<Path> _dependPathList = new ArrayList<Path>();

  private String _sourceType = "unknown";

  private WebAppAdmin _admin = new WebAppAdmin(this);

  public WebAppController()
  {
    this("/", null, null);
  }
  
  public WebAppController(String contextPath,
			  Path rootDirectory,
			  ApplicationContainer container)
  {
    super(contextPath, rootDirectory);

    _container = container;
      
    setContextPath(contextPath);

    if (container != null) {
      for (WebAppConfig config : container.getWebAppDefaultList())
	addConfigDefault(config);
    }

    getVariableMap().put("app", new Var());
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
    if (getConfig() == null || getConfig().getURLRegexp() == null)
      return getContextPath();

    Pattern regexp = getConfig().getURLRegexp();
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
    if (_container != null)
      return _container.getURL() + _contextPath;
    else
      return _contextPath;
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
  public ApplicationContainer getContainer()
  {
    return _container;
  }
  
  /**
   * Sets the parent controller.
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
   * Returns the application object.
   */
  public Application getApplication()
  {
    return getDeployInstance();
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

  protected String getMBeanTypeName()
  {
    return "WebApp";
  }

  protected String getMBeanId()
  {
    String name = _contextPath;
    if (_contextPath.equals(""))
      name = "/";

    return name;
  }

  /**
   * Creates the managed object.
   */
  protected Object createMBean()
    throws JMException
  {
    return new IntrospectionMBean(_admin, WebAppMBean.class);
  }

  /**
   * Returns the admin.
   */
  public WebAppMBean getAdmin()
  {
    // XXX: possibly return the proxy?
    return _admin;
  }

  /**
   * Returns true if the controller matches.
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
  protected WebAppController merge(WebAppController newController)
  {
    if (getConfig() != null && getConfig().getURLRegexp() != null)
      return newController;
    else if (newController.getConfig() != null &&
	     newController.getConfig().getURLRegexp() != null)
      return this;
    else {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
	thread.setContextClassLoader(getParentClassLoader());

	WebAppController mergedController
	  = new WebAppController(_contextPath, getRootDirectory(), _container);

	mergedController.mergeController(newController);
	mergedController.mergeController(this);

	return mergedController;
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

    if (_container != null)
      _container.removeWebApp(this);
    
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
    return new Application(this);
  }

  /**
   * Creates the application.
   */
  protected void configureInstanceVariables(Application app)
    throws Throwable
  {
    app.setRegexp(_regexpValues);
    app.setDynamicDeploy(isDynamicDeploy());

    super.configureInstanceVariables(app);

    getVariableMap().put("app-dir", app.getAppDir());
  }

  protected Path calculateRootDirectory()
    throws ELException
  {
    Path appDir = null;

    if (appDir == null && getConfig() != null) {
      String path = getConfig().getRootDirectory();

      if (path != null)
        appDir = PathBuilder.lookupPath(path);
    }

    if (appDir == null && _container != null)
      appDir = _container.getDocumentDirectory().lookup("./" + _contextPath);

    if (appDir == null && getDeployInstance() != null)
      appDir = getDeployInstance().getAppDir();

    return appDir;
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
  
  /**
   * Returns a printable view.
   */
  public String toString()
  {
    return "WebAppController$" + System.identityHashCode(this) + "[" + getId() + "]";
  }

  /**
   * EL variables for the app.
   */
  public class Var {
    public String getURL()
    {
      return WebAppController.this.getURL();
    }

    public String getId()
    {
      return getName();
    }

    public String getName()
    {
      String id = WebAppController.this.getId();
      
      if (id != null)
	return id;
      
      return WebAppController.this.getContextPath();
    }

    public Path getAppDir()
    {
      return WebAppController.this.getRootDirectory();
    }

    public Path getDocDir()
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

    public String toString()
    {
      return "WebApp[" + getURL() + "]";
    }
  }
}
