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

import javax.servlet.jsp.el.VariableResolver;

import org.xml.sax.SAXException;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;

import org.iso_relax.verifier.Schema;

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

import com.caucho.relaxng.CompactVerifierFactoryImpl;

import com.caucho.config.NodeBuilder;
import com.caucho.config.ConfigException;
import com.caucho.config.BuilderProgram;
import com.caucho.config.types.PathBuilder;

import com.caucho.jmx.Jmx;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.server.deploy.DeployInstance;

import com.caucho.server.webapp.ApplicationContainer;
import com.caucho.server.webapp.WebAppController;

import com.caucho.ejb.EJBServerInterface;

/**
 * An enterprise application (ear)
 */
public class EnterpriseApplication
  implements EnvironmentBean, DeployInstance {
  static final L10N L = new L10N(EnterpriseApplication.class);
  static final Logger log = Log.open(EnterpriseApplication.class);
  
  protected static EnvironmentLocal<EJBServerInterface> _localServer =
    new EnvironmentLocal<EJBServerInterface>("caucho.ejb-server");

  private static Schema _earXmlSchema;

  private EnvironmentClassLoader _loader;

  private String _name;
  
  private Path _rootDir;

  private Path _earPath;

  private String _prefix = "";

  private EarDeployController _entry;

  private Path _webappsPath;

  private ApplicationConfig _config;

  private ApplicationContainer _container;

  private EarConfig _earConfig;
  
  // The EL variable map
  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  private VariableResolver _variableResolver;

  // The JMX context properties
  private LinkedHashMap<String,String> _jmxContext;

  // private WarDirApplicationGenerator _warDeploy;
  
  private ArrayList<WebAppController> _webApps =
    new ArrayList<WebAppController>();

  private Throwable _configException;

  private final Lifecycle _lifecycle;

  /**
   * Creates the application.
   */
  EnterpriseApplication(ApplicationContainer container,
			EarDeployController entry, String name)
  {
    _container = container;
    
    _entry = entry;
    _name = name;

    ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
    
    _loader = new EnhancingClassLoader(container.getClassLoader());
    _loader.setOwner(this);
    _loader.setId("EnterpriseApplication[" + name + "]");

    VariableResolver parentResolver = EL.getEnvironment();
    _variableResolver = new MapVariableResolver(_variableMap,
						parentResolver);

    EL.setEnvironment(_variableResolver, _loader);

    Vfs.setPwd(_entry.getRootDirectory(), _loader);
    WorkDir.setLocalWorkDir(_entry.getRootDirectory().lookup("META-INF/work"),
			    _loader);

    _jmxContext = Jmx.copyContextProperties();
    _jmxContext.put("EApp", name);

    Jmx.setContextProperties(_jmxContext, _loader);
    
    _lifecycle = new Lifecycle(log, toString(), Level.INFO);

    if (entry.getArchivePath() != null)
      Environment.addDependency(new Depend(entry.getArchivePath()), _loader);
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
   * Sets the prefix URL for web applications.
   */
  public void setPrefix(String prefix)
  {
    _prefix = prefix;
  }

  /**
   * Adds the ear config.
   */
  public void addEarConfig(EarConfig earConfig)
  {
    _earConfig = earConfig;
  }

  /**
   * Returns the variable resolver.
   */
  public VariableResolver getVariableResolver()
  {
    return _variableResolver;
  }

  /**
   * Returns the map.
   */
  public Map<String,Object> getVariableMap()
  {
    return _variableMap;
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
    throws Throwable
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_loader);
      
      if (! _lifecycle.toInit())
	return;
      
      log.fine(this + " initializing");
      
      Vfs.setPwd(_rootDir, _loader);

      if (_webappsPath == null)
	_webappsPath = _rootDir.lookup("webapps");

      Path appXml = _rootDir.lookup("META-INF/application.xml");

      if (! appXml.canRead())
	throw new ConfigException(L.l("missing application.xml for ear {0}.  .ear files require a META-INF/application.xml file.",
				      _earPath));

      Depend depend = new Depend(appXml);
      Environment.addDependency(depend, _loader);

      _config = parseApplicationConfig(_rootDir, appXml);

      ArrayList<Path> ejbModules = _config.getEjbModules();

      Throwable configException = null;

      try {
	configureEJB(ejbModules);
      } catch (ConfigException e) {
	configException = e;

	log.warning(e.getMessage());
      } catch (Throwable e) {
	configException = e;
	
	log.log(Level.WARNING, e.toString(), e);
      }
      
      ArrayList<WebModule> webModules = _config.getWebModules();

      for (int i = 0; i < webModules.size(); i++) {
	WebModule web = webModules.get(i);

	String webUri = web.getWebURI();
	String contextUrl = web.getContextRoot();
	Path path = _rootDir.lookup(webUri);

	if (contextUrl == null)
	  contextUrl = webUri;

	WebAppController entry = null;
	if (webUri.endsWith(".war")) {
	  String name = webUri.substring(0, webUri.length() - 4);
	  int p = name.lastIndexOf('/');
	  if (p > 0)
	    name = name.substring(p + 1);

	  Path expandPath = _webappsPath;
	  expandPath.mkdirs();

	  entry = new WebAppController(_container, contextUrl);
	  entry.setRootDirectory(expandPath.lookup(name));
	  entry.setArchivePath(path);
	} else {
	  entry = new WebAppController(_container, contextUrl);
	  entry.setRootDirectory(path);
	}

	entry.setDynamicDeploy(true);
	entry.setConfigException(configException);

	_webApps.add(entry);

	entry.init();

	_container.getApplicationGenerator().update(entry.getName());

	if (configException != null)
	  throw configException;
      }
    } catch (ConfigException e) {
      _configException = e;
      
      throw e;
    } catch (Throwable e) {
      _configException = e;
      
      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  static ApplicationConfig parseApplicationConfig(Path rootDir, Path appXml)
    throws IOException, ConfigException, SAXException
  {
    ApplicationConfig config = new ApplicationConfig();

    NodeBuilder builder = new NodeBuilder();
    builder.setCompactSchema("com/caucho/server/e_app/ear.rnc");

    builder.configure(config, appXml);

    return config;
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
	WebAppController entry = _webApps.get(i);

	try {
	  entry.start();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }
      */
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
      WebAppController entry = _webApps.get(i);

      if (entry.isNameMatch(name))
	return entry;
    }

    return null;
  }

  private void addDepend(Path path)
  {
    _loader.addDependency(new com.caucho.vfs.Depend(path));
  }

  /**
   * Configures the EJB server.
   */
  private void configureEJB(ArrayList<Path> ejbModules)
    throws Exception
  {
    if (ejbModules.size() == 0)
      return;

    _loader.init();
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      EJBServerInterface ejbServer = _localServer.getLevel();

      if (ejbServer == null) {
	throw new ConfigException(L.l("<ear-deploy> needs a configured <ejb-server>"));
      }
	
      for (int i = 0; i < ejbModules.size(); i++) {
	Path path = ejbModules.get(i);

	_loader.addJar(path);
	// ejb/0853
	_loader.addJarManifestClassPath(path);
      }

      for (int i = 0; i < ejbModules.size(); i++) {
	Path path = ejbModules.get(i);

	ejbServer.addEJBJar(path);
      }

      ejbServer.initEJBs();
    } catch (ConfigException e) {
      e.printStackTrace();
      
      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
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

      ArrayList<WebAppController> webApps = new ArrayList<WebAppController>(_webApps);
      _webApps.clear();
      
      for (int i = 0; i < webApps.size(); i++) {
	WebAppController entry = webApps.get(i);

	// _parentContainer.undeployWebApp(entry);
	
	entry.destroy();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);

      _loader.destroy();

      log.fine(this + " destroyed");
    }
  }

  protected Schema getEarXmlSchema()
  {
    if (_earXmlSchema == null) {
      try {
        MergePath schemaPath = new MergePath();
        schemaPath.addClassPath();
      
        Path path = schemaPath.lookup("com/caucho/server/e_app/resin-ear-xml.rnc");
        if (path.canRead()) {
          ReadStream is = path.openRead();

          try {
            // VerifierFactory factory = VerifierFactory.newInstance("http://caucho.com/ns/compact-relax-ng/1.0");
          
            CompactVerifierFactoryImpl factory;
            factory = new CompactVerifierFactoryImpl();

            _earXmlSchema = factory.compileSchema(is);
          } finally {
            is.close();
          }
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return _earXmlSchema;
  }

  public String toString()
  {
    return "EnterpriseApplication[" + getName() + "]";
  }
}
