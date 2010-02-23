/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.EjbRef;
import com.caucho.ejb.EJBClientInterface;
import com.caucho.ejb.session.AbstractStubLoader;
import com.caucho.java.WorkDir;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.naming.Jndi;
import com.caucho.server.deploy.DeployInstance;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An enterprise application (ear)
 */
public class EntAppClient implements DeployInstance, EnvironmentBean {
  /* implements EnvironmentBean, EnvironmentListener, AlarmListener */

  static final L10N L = new L10N(EntAppClient.class);
  private static final Logger log
    = Logger.getLogger(EntAppClient.class.getName());

  private EnvironmentClassLoader _loader;

  private String _name;

  private Path _rootDir;

  private Path _archivePath;

  private String _mainClass;

  private String _prefix = "";

  private AppClientDeployController _entry;

  private AbstractStubLoader _stubLoader;

  private ApplicationConfig _config;

  private JarPath _clientJar;
  
  private EJBClientInterface _ejbClient;
  
  private AppClientConfig _appClientConfig;

  private ArrayList<EjbLink> _links = new ArrayList<EjbLink>();

  private HashMap<String,EjbRef> _ejbRefMap = new HashMap<String,EjbRef>();

  private Throwable _configException;

  private Alarm _alarm;
  private final Lifecycle _lifecycle;

  private static void log(String message)
  {
    System.out.println(EntAppClient.class.getSimpleName() + ": " + message);
  }

  /**
   * Creates the application.
   */
  EntAppClient(AppClientDeployController entry, String name)
  {
    _entry = entry;
    _name = name;

    ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();

    String id = getClass().getSimpleName() + "[" + name + "]";
    _loader = EnvironmentClassLoader.create(parentLoader, id);
    //_loader.setOwner(this);

    _lifecycle = new Lifecycle(log, toString(), Level.INFO);

    if (entry.getArchivePath() != null)
      Environment.addDependency(new Depend(entry.getArchivePath()), _loader);

    // _alarm = new Alarm(this);
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
    _loader.setId("EntAppClient[" + name + "]");
  }

  /**
   * Gets the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the ejb client.
   */
  public EJBClientInterface getEJBClient()
    throws ClassNotFoundException,
	   InstantiationException,
	   IllegalAccessException
  {
    if (_ejbClient == null) {
      Class cl = Class.forName("com.caucho.iiop.IiopClient");
      _ejbClient = (EJBClientInterface) cl.newInstance();
    }

    return _ejbClient;
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
   * Sets the archive path
   */
  public void setArchivePath(Path archivePath)
  {
    _archivePath = archivePath;
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the display-name.
   */
  public void setDisplayName(String displayName)
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
   * Adds an ejb-reference.
   */
  public void addEjbRef(EjbRef ejbRef)
  {
    _ejbRefMap.put(ejbRef.getEjbRefName(), ejbRef);

    addIiopStub(ejbRef.getHome());
    addIiopStub(ejbRef.getRemote());
  }

  private void addIiopStub(Class stubClass)
  {
    if (stubClass == null)
      return;

    try {
      if (_stubLoader == null) {
	Class iiopClass = Class.forName("com.caucho.iiop.IiopStubLoader");
	_stubLoader = (AbstractStubLoader) iiopClass.newInstance();
	_stubLoader.setPath(WorkDir.getLocalWorkDir());
	_loader.addLoader(_stubLoader);
      }

      _stubLoader.addStubClass(stubClass.getName());
    } catch (Throwable e) {
      e.printStackTrace();
      
      log.info(e.toString());
    }
  }

  /**
   * Gets an ejb home.
   */
  Class getEjbHome(String ejbName)
    throws ConfigException
  {
    EjbRef ref = _ejbRefMap.get(ejbName);
    
    if (ref != null)
      return ref.getHome();
    else
      return null;
  }

  /**
   * Sets the main class.
   */
  public void setMainClass(String mainClass)
  {
    _mainClass = mainClass;
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
   * Adds an EjbLink.
   */
  public EjbLink createEjbLink()
  {
    EjbLink link = new EjbLink();

    _links.add(link);

    return link;
  }

  /**
   * Adds an EjbLink.
   */
  public SecurityRole createSecurityRole()
  {
    return new SecurityRole();
  }

  /**
   * Stub for the xsi:schemaLocation tag.
   */
  public void setSchemaLocation(String s)
  {
  }

  /**
   * Stub for the version tag.
   */
  public void setVersion(String s)
  {
  }

  /**
   * Initialize the client.
   */
  @PostConstruct
  public void init()
  {
    log("INIT: " + _links);
    if (! _lifecycle.toInit())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      Path rootDir = getRootDirectory();
      Vfs.setPwd(rootDir);

      Path workDir = getRootDirectory().lookup("META-INF/work");
      _loader.addJar(workDir);

      WorkDir.setLocalWorkDir(workDir);

      for (EjbLink link : _links) {
	link.deploy();
      }

      // configApplication();
    } catch (Throwable e) {
      e.printStackTrace();
      log.log(Level.WARNING, e.toString(), e);

      _configException = e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void configResinBinding()
    throws Exception
  {
    Path rootDir = getRootDirectory();

    Path xml = rootDir.lookup("META-INF/resin-client.xml");

    if (! xml.canRead())
      return;

    // AppClientBinding binding = new AppClientBinding(this);

    // builder.setCompactSchema("com/caucho/server/e_app/app-client.rnc");

    new Config().configure(this, xml);
  }

  /**
   * Return true for modified.
   */
  public boolean isModified()
  {
    return false;
  }

  /**
   * Return true for modified.
   */
  public boolean isModifiedNow()
  {
    return false;
  }

  /**
   * Log the reason for modification.
   */
  public boolean logModified(Logger log)
  {
    return false;
  }

  /**
   * Return true is the deployment had an error.
   */
  public boolean isDeployError()
  {
    return _configException != null;
  }

  /**
   * Return true is the deployment is idle
   */
  public boolean isDeployIdle()
  {
    return false;
  }

  /**
   * Start the client.
   */
  public void start()
  {
    init();

    if (! _lifecycle.toActive())
      return;
  }

  /**
   * Execute the main class.
   */
  public void main(String []args)
    throws Throwable
  {
    if (_mainClass == null)
      throw new IllegalStateException(L.l("main() method require a main class"));

    main(_mainClass, args);
  }

  /**
   * Execute the main class.
   */
  public void main(String mainClassName, String []args)
    throws Throwable
  {
    start();

    log("MAIN: " + mainClassName);
    log("C: " + System.getProperty("java.class.path"));

    if (_configException != null)
      throw _configException;

    if (! _lifecycle.isActive())
      throw new IllegalStateException(L.l("{0} is not active.",
					  this));

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      Class mainClass = Class.forName(mainClassName, false, _loader);

      log("MAIN:");
      Method main = mainClass.getMethod("main",
					new Class[] { String[].class });

      try {
	Class cl = Class.forName("com.sun.ts.lib.implementation.sun.common.SunRIURL", false, _loader);
	log("CL: " + cl);
      } catch (Throwable e) {
	e.printStackTrace();
      }

      main.invoke(null, new Object[] { args});
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Destroys the client.
   */
  public void destroy()
  {
    if (! _lifecycle.toDestroy())
      return;
  }

  public String toString()
  {
    return "EntAppClient[" + getName() + "]";
  }

  public class EjbLink {
    private String _ejbName;
    private String _jndiName;
    private Class _api;

    public void setEjbName(String ejbName)
      throws ConfigException
    {
      _ejbName = ejbName;
    }

    public void setJndiName(String jndiName)
    {
      _jndiName = jndiName;
    }

    public void deploy()
      throws Exception
    {
      log("LINK: " + _jndiName + " " + _ejbName);
      String orbHost = System.getProperty("org.omg.CORBA.ORBInitialHost");
      String orbPort = System.getProperty("org.omg.CORBA.ORBInitialPort");

      Hashtable env = new Hashtable();
      env.put("java.naming.factory.initial",
	      "com.sun.jndi.cosnaming.CNCtxFactory");
      env.put("java.naming.provider.url", "iiop://" + orbHost + ":" + orbPort);
      javax.naming.Context ic = new InitialContext(env);

      Object ior = ic.lookup(_jndiName);
      
      _api = getEjbHome(_ejbName);

      if (_api == null)
	throw new ConfigException(L.l("'{0}' is an unknown ejb name.",
				      _ejbName));
      
      Object value = PortableRemoteObject.narrow(ior, _api);

      log("VALUE: " + value + " " + value.getClass() + " " + _api);
      Jndi.rebindDeepShort(_ejbName, value);
    }
  }

  public class SecurityRole {
    public void setId(String id)
    {
    }

    public void addDescription(String description)
    {
    }

    public void setRoleName(String roleName)
    {
    }

    public void setRoleLink(String roleLink)
    {
    }
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
    }
    
    /**
     * Adds a new ejb module.
     */
    public void addEjb(Path path)
      throws Exception
    {
      getClassLoader().addJar(path);

      getEJBClient().addEJBJar(path);

      getEJBClient().initEJBs();
    }
    
    /**
     * Adds a new java module.
     */
    public void addJava(Path path)
      throws Exception
    {
      if (! path.canRead())
	throw new ConfigException(L.l("<java> module {0} must be a valid path.",
				      path));

      
      getClassLoader().addJar(path);
	
      _clientJar = JarPath.create(path);

      Manifest manifest = _clientJar.getManifest();
      String mainClass = manifest.getMainAttributes().getValue("Main-Class");

      setMainClass(mainClass);

      Path appClient = _clientJar.lookup("META-INF/application-client.xml");

      if (appClient.canRead())
	new Config().configureBean(EntAppClient.this, appClient);
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
