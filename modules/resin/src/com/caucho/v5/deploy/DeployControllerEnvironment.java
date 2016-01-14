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

package com.caucho.v5.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.types.PathBuilder;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * A deploy controller for an environment.
 */
abstract public class
  DeployControllerEnvironment<I extends DeployInstanceEnvironment,
                              C extends ConfigDeploy>
  extends DeployControllerExpand<I>
//  implements EnvironmentListener
{
  private static final L10N L = new L10N(DeployControllerEnvironment.class);
  private static final Logger log
    = Logger.getLogger(DeployControllerEnvironment.class.getName());

  // The JMX identity
  private LinkedHashMap<String,String> _jmxContext;

  private Object _mbean;

  private ObjectName _objectName;

  // The default configurations
  private ArrayList<C> _configDefaults =  new ArrayList<C>();

  // The primary configuration
  private C _config;
  // private ConfigDeploy _prologue;

  // The variable mapping
  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  // Config exception passed in from parent, e.g. .ear
  private Throwable _configException;

  public DeployControllerEnvironment(String id,
                                     PathImpl rootDirectory)
  {
    this(id, rootDirectory, Thread.currentThread().getContextClassLoader());
  }

  public DeployControllerEnvironment(String id,
                                     PathImpl rootDirectory,
                                     ClassLoader loader)
  {
    this(id, rootDirectory, loader, null, null);
  }

  public DeployControllerEnvironment(String id,
                                     PathImpl rootDirectory,
                                     ClassLoader loader,
                                     C config,
                                     DeployContainerService<I,?> container)
  {
    super(id, loader, rootDirectory, container);

    //_jmxContext = JmxUtil.copyContextProperties(getParentClassLoader());
    
    setConfig(config);
    
    if (config != null) {
      setControllerType(config.getControllerType());
    }
  }

  /**
   * Sets the primary configuration.
   */
  public void setConfig(C config)
  {
    if (config == null)
      return;

    if (_config != null && ! _configDefaults.contains(_config)) {
      addConfigDefault(_config);
    }

    addConfigController(config);

    _config = config;
  }

  /**
   * Gets the primary configuration
   */
  public C getConfig()
  {
    return _config;
  }

  /**
   * Gets the prologue configuration
   */
  public ContainerProgram getPrologue()
  {
    if (getConfig() != null) {
      return getConfig().getPrologue();
    }
    else {
      return null;
    }
  }

  /**
   * Sets the prologue configuration
   */
  public void setPrologue(ContainerProgram program)
  {
    // getConfig().setPrologue(program);
  }

  /**
   * Returns the error message
   */
  public String getErrorMessage()
  {
    Throwable exn = getConfigException();

    if (exn instanceof ConfigException) {
      exn.printStackTrace();
      return exn.getMessage();
    }
    else if (exn != null) {
      exn.printStackTrace();
      return exn.toString();
    }
    else
      return null;
  }

  /**
   * Returns the configure exception.
   */
  @Override
  public Throwable getConfigException()
  {
    Throwable configException = super.getConfigException();

    if (configException == null)
      configException = _configException;

    /*
    if (configException == null) {
      DeployInstance deploy = getDeployInstance();

      if (deploy != null)
        configException = deploy.getConfigException();
    }
    */

    return configException;
  }

  /**
   * Adds a default config.
   */
  public void addConfigDefault(C config)
  {
    if (! _configDefaults.contains(config)) {
      _configDefaults.add(config);

      addConfigController(config);
    }
  }

  private void addConfigController(C config)
  {
    if (config.getStartupMode() != null) {
      setStartupMode(config.getStartupMode());
    }

    if (config.getRedeployCheckInterval() != null) {
      setRedeployCheckInterval(config.getRedeployCheckInterval());
    }

    if (config.getRedeployMode() != null) {
      setRedeployMode(config.getRedeployMode());
    }

    if (config.getExpandCleanupFileset() != null) {
      addParentExpandCleanupFileSet(config.getExpandCleanupFileset());
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
   * Returns the mbean.
   */
  public Object getMBean()
  {
    return _mbean;
  }

  /**
   * Returns the object name.
   */
  public ObjectName getObjectName()
  {
    return _objectName;
  }

  /**
   * Sets a parent config exception (e.g. from a .ear)
   */
  public void setConfigException(Throwable e)
  {
    _configException = e;
    
    /*
    if (getDeployInstance() != null)
      getDeployInstance().setConfigException(e);
      */
  }

  /**
   * Initialize the controller.
   */
  @Override
  protected void initEnd()
  {
    super.initEnd();
    /*
    if (getDeployAdmin() != null) {
      getDeployAdmin().register();
    }
    */
  }

  /**
   * Returns true if the entry matches.
   */
  @Override
  public boolean isNameMatch(String url)
  {
    return url.equals(getId());
  }
  
  protected String getMBeanTypeName()
  {
    // return getDeployInstance().getClass().getSimpleName();
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected String getMBeanId()
  {
    String name = getId();
    if (name == null || name.equals(""))
      name = "default";

    return name;
  }

  /**
   * Merges with the old controller.
   */
  /*
  @Override
  protected void mergeController(DeployController oldControllerV)
  {
    super.mergeController(oldControllerV);

    EnvironmentDeployController<I,C> oldController;
    oldController = (EnvironmentDeployController) oldControllerV;
    // setId(oldController.getId());

    ArrayList<C> configDefaults = new ArrayList<C>();

    if (getPrologue() == null)
      setPrologue(oldController.getPrologue());
    else if (oldController.getPrologue() != null) {
      configDefaults.add(0, (C) getPrologue()); // XXX: must be first

      setPrologue(oldController.getPrologue());
    }

    configDefaults.addAll(oldController._configDefaults);
    
    if (getConfig() == null)
      setConfig(oldController.getConfig());
    else if (oldController.getConfig() != null) {
      configDefaults.add(getConfig());

      setConfig(oldController.getConfig());
    }

    for (C config : _configDefaults) {
      if (! configDefaults.contains(config))
        configDefaults.add(config);
    }
    
    _configDefaults = configDefaults;

    mergeStartupMode(oldController.getStartupMode());

    mergeRedeployCheckInterval(oldController.getRedeployCheckInterval());

    mergeRedeployMode(oldController.getRedeployMode());
  }
  */
  @Override
  public void merge(DeployController<I> oldControllerV)
  {
    DeployControllerEnvironment<I,C> oldController;
    oldController = (DeployControllerEnvironment<I,C>) oldControllerV;
    
    // server/10l4
    if (! getRootDirectory().equals(oldController.getRootDirectory())) {
      return;
    }
    
    super.merge(oldControllerV);
    // setId(oldController.getId());

    ArrayList<C> configDefaults = new ArrayList<C>();

    if (getPrologue() == null)
      setPrologue(oldController.getPrologue());
    else if (oldController.getPrologue() != null) {
      // XXX:
      // configDefaults.add(0, (C) getPrologue()); // XXX: must be first
      // setPrologue(getPrologue

      setPrologue(oldController.getPrologue());
    }

    configDefaults.addAll(oldController._configDefaults);

    if (getConfig() == null)
      setConfig(oldController.getConfig());
    else if (oldController.getConfig() != null) {
      configDefaults.add(getConfig());

      setConfig(oldController.getConfig());
    }

    for (C config : _configDefaults) {
      if (! configDefaults.contains(config))
        configDefaults.add(config);
    }
    
    _configDefaults = configDefaults;

    // mergeStartupMode(oldController.getStartupMode());

    // mergeRedeployCheckInterval(oldController.getRedeployCheckInterval());

    // mergeRedeployMode(oldController.getRedeployMode());
    setArchivePath(oldController.getArchivePath());
    setStartupMode(oldController.getStartupMode());
    setRedeployMode(oldController.getRedeployMode());
    setStartupPriority(oldController.getStartupPriority());
  }
  
  /*
  protected DeployControllerAdmin<I,?> getDeployAdmin(DeployHandle<I> handle)
  {
    return null;
  }
  */

  /**
   * Returns the application object.
   */
  @Override
  public boolean destroy()
  {
    if (! super.destroy())
      return false;

    // Environment.removeEnvironmentListener(this, getParentClassLoader());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());
      
      /*
      if (getDeployAdmin() != null)
        getDeployAdmin().unregister();
        */
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return true;
  }

  /**
   * Configures the instance.
   */
  @Override
  protected void configureInstance(final DeployInstanceBuilder<I> builder)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    final ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      final ClassLoader classLoader = builder.getClassLoader();

      thread.setContextClassLoader(classLoader);

      log.fine("initializing " + builder);
      
      super.configureInstance(builder);

      // set from external error, like .ear
      builder.setConfigException(_configException);

      extendJMXContext(_jmxContext);
      //JmxUtil.setContextProperties(_jmxContext, classLoader);

      configureInstanceVariables(builder);

      //ClassLoader loader = builder.getClassLoader();
      //thread.setContextClassLoader(loader);
      VfsOld.setPwd(getRootDirectory());

      initDependencies();

      builder.preConfigInit();
      
      ConfigInstanceBuilder config;
      ConfigInstanceBuilder configPrologue;
      
      /*
      if (builder instanceof XmlSchemaBean) {
        XmlSchemaBean schemaBean = (XmlSchemaBean) builder;
        
        configPrologue = new ConfigInstanceBuilderXml(schemaBean.getSchema());
        config = new ConfigInstanceBuilderXml(schemaBean.getSchema());
      }
      else {
        configPrologue = new ConfigInstanceBuilder();
        config = new ConfigInstanceBuilder();
      }
      */
      
      configPrologue = new ConfigInstanceBuilder();
      config = new ConfigInstanceBuilder();
      
      //CandiManager cdiManager = CandiManager.getCurrent();
      
      try {
        //cdiManager.setEnableAutoUpdate(false);

        if (getPrologue() != null) {
          getPrologue().configure(configPrologue);
        }

        fillInitListPrologue(configPrologue);
        fillInitList(config);
      } finally {
        //cdiManager.setEnableAutoUpdate(true);
      }
      
      //cdiManager.fireBeforeBeanDiscovery();

      if (classLoader instanceof DynamicClassLoader) {
        DynamicClassLoader dynLoader = (DynamicClassLoader) classLoader;
        
        try {
          dynLoader.make();
        } catch (Exception e) {
          log.warning(e.toString());
          
          // baratine/a125
          builder.setConfigException(e);
        }

        dynLoader.updateScan();
      }
      
      configPrologue.getProgram().configure(builder);
      
      builder.postClassLoaderInit();
      
      // cdiManager.update();
      
      config.getProgram().configure(builder);
      // builder.init();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  protected void extendJMXContext(Map<String,String> context)
  {
    // _jmxContext.put(getMBeanTypeName(), getMBeanId());
  }

  protected void fillInitListPrologue(ConfigInstanceBuilder builder)
  {
    boolean isSkipDefault = _config != null && _config.isSkipDefaultConfig();

    // configure prologue first
    if (! isSkipDefault) {
      for (ConfigDeploy config : _configDefaults) {
        ConfigProgram prologue = config.getPrologue();

        if (prologue != null) {
          prologue.configure(builder);
        }
      }
    }

    if (_config != null && ! _configDefaults.contains(_config)) {
      if (_config.getPrologue() != null) {
        _config.getPrologue().configure(builder);
      }
    }
  }

  protected void fillInitList(ConfigInstanceBuilder builder)
  {
    boolean isSkipDefault = _config != null && _config.isSkipDefaultConfig();

    // normal configuration
    if (! isSkipDefault) {
      for (ConfigDeploy config : _configDefaults) {
        config.getBuilderProgram().configure(builder);
      }
    }

    if (_config != null && ! _configDefaults.contains(_config)) {
      _config.getBuilderProgram().configure(builder);
    }
  }

  protected void configureInstanceVariables(DeployInstanceBuilder<I> builder)
  {
    PathImpl rootDirectory = getRootDirectory();

    if (rootDirectory == null)
      throw new NullPointerException("Null root directory");

    if (! rootDirectory.isFile()) {
    }
    else if (rootDirectory.getPath().endsWith(".jar")
             || rootDirectory.getPath().endsWith(".war")) {
      throw new ConfigException(L.l("root-directory '{0}' must specify a directory.  It may not be a .jar or .war.",
                                    rootDirectory.getPath()));
    }
    else
      throw new ConfigException(L.l("root-directory `{0}' may not be a file.  root-directory must specify a directory.",
                                    rootDirectory.getPath()));
    VfsOld.setPwd(rootDirectory);

    if (log.isLoggable(Level.FINEST)) {
      log.fine("root-directory=" + rootDirectory + " " + builder);
    }

    // instance.setRootDirectory(rootDirectory);
  }
  
  public final Map<String,String> getRepositoryMetaData()
  {
    HashMap<String,String> map = new HashMap<String,String>();

    /*
    Map<String,RepositoryTagEntry> tagMap = getRepository().getTagMap();
    
    RepositoryTagEntry entry = tagMap.get(getId());
    
    if (entry == null && getAutoDeployTag() != null) {
      entry = tagMap.get(getAutoDeployTag());
    }
    
    if (entry != null) {
      map.putAll(entry.getAttributeMap());
    }
    */
    
    return map;
  }
  
  public String []getClassPath()
  {
    I instance = null; // getDeployInstance();
    
    if (instance == null)
      return null;
    
    ClassLoader loader = instance.getClassLoader();
    
    if (! (loader instanceof DynamicClassLoader)) {
      log.finer(this + " " + loader + " is not a DynamicClassLoader");
      
      return null;
    }

    DynamicClassLoader dynLoader = (DynamicClassLoader) loader;
    
    ArrayList<String> classPathList = new ArrayList<String>();
    
    dynLoader.buildClassPath(classPathList);
    
    String []classPath = new String[classPathList.size()];
    
    classPathList.toArray(classPath);
    
    return classPath;
  }

  @Override
  public PathImpl getArchivePath()
  {
    PathImpl path = super.getArchivePath();

    if (path != null)
      return path;

    if (_config != null) {
      String pathString = _config.getArchivePath();

      if (pathString != null) {
        path = PathBuilder.lookupPath(pathString);
      }

      setArchivePath(path);
    }

    return path;
  }
  
  /**
   * Returns a printable view.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "," + getLifecycleState() + "]";
  }
}
