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

package com.caucho.v5.http.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.types.PathBuilder;
import com.caucho.v5.deploy.DeployController;
import com.caucho.v5.deploy.DeployControllerAdmin;
import com.caucho.v5.deploy.DeployControllerEnvironment;
import com.caucho.v5.deploy.DeployControllerType;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.deploy.DeployInstanceBuilder;
import com.caucho.v5.http.webapp.WebAppConfig;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.management.server.HostMXBean;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Depend;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * A configuration entry for a host
 */
public class HostController
  extends DeployControllerEnvironment<Host,HostConfig>
{
  private static final L10N L = new L10N(HostController.class);
  
  private static final Logger log
    = Logger.getLogger(HostController.class.getName());
  
  private HostContainer _container;

  private final String _idKey;
  
  // The host name is the canonical name
  private final String _hostName;

  // The regexp name is the matching name of the regexp
  private String _regexpName;

  private Pattern _regexp;
  private String _rootDirectoryPattern;

  // Any host aliases.
  private ArrayList<String> _entryHostAliases
    = new ArrayList<String>();
  private ArrayList<Pattern> _entryHostAliasRegexps
    = new ArrayList<Pattern>();

  // includes aliases from the Host, e.g. server/1f35
  private ArrayList<String> _hostAliases = new ArrayList<String>();
  private ArrayList<Pattern> _hostAliasRegexps = new ArrayList<Pattern>();

  // The host variables.
  private final HostVar _hostVar = new HostVar(this);
  private HostAdmin _admin;

  private ArrayList<Dependency> _dependList = new ArrayList<Dependency>();

  HostController(String id,
                 PathImpl rootDirectory,
                 String hostName,
                 HostConfig config,
                 HostContainer container,
                 Map<String,Object> varMap)
  {
    super(id, rootDirectory, container.getClassLoader(),
          config, container.getHostDeployContainer());
    
    _hostName = hostName;

    if (varMap != null) {
      getVariableMap().putAll(varMap);
    }
    
    getVariableMap().put("host", _hostVar);

    setContainer(container);
    
    // String id = handle.getId();
    
    int p = id.lastIndexOf('/');
    _idKey = id.substring(p + 1);
    
    if (config != null) {
      _regexp = config.getRegexp();
      addConfigValues(config);
    }
  }

  /*
  public HostController(String id,
                        Path rootDirectory,
                        String hostName,
                        HostContainer container)
  {
    this(id, rootDirectory, hostName, null, container, null);
  }
  */
  
  private boolean isErrorHost()
  {
    return getId().startsWith("error/");
  }

  public void setContainer(HostContainer container)
  {
    _container = container;
    
    if (_container != null && ! isErrorHost()) {
      for (HostConfig defaultConfig : _container.getHostDefaultList()) {
        addConfigDefault(defaultConfig);
      }
    }
  }
  
  public HostContainer getContainer()
  {
    return _container;
  }
  
  @Override
  public void addConfigDefault(HostConfig config)
  {
    addConfigValues(config);
    
    super.addConfigDefault(config);
  }
  
  private void addConfigValues(HostConfig config)
  {
    for (String hostAlias : config.getHostAliases()) {
      _entryHostAliases.add(hostAlias);
      _hostAliases.add(hostAlias);
    }
    
    _entryHostAliasRegexps.addAll(config.getHostAliasRegexps());
    _hostAliasRegexps.addAll(config.getHostAliasRegexps());
  }

  /**
   * Returns the Resin host name.
   */
  public String getName()
  {
    /*
    String name = getHostName();
    
    if (name.isEmpty())
      return Host.DEFAULT_NAME;
    else
      return name;
      */
    return _idKey;
  }

  /**
   * Returns the host's canonical name
   */
  public String getHostName()
  {
    return _hostName;
  }

  /**
   * Returns the host's canonical name
   */
  public void setRegexpName(String name)
  {
    _regexpName = name.toLowerCase(Locale.ENGLISH);
  }
  
  /**
   * Adds a host alias.
   */
  public void addHostAlias(String name)
  {
    if (name != null) {
      name = name.trim();
    }
    
    if (name == null || name.equals("*")) {
      // name = ""; // XXX: default?
      return;
    }
    
    name = name.toLowerCase(Locale.ENGLISH);

    if (! _entryHostAliases.contains(name)) {
      _entryHostAliases.add(name);
    }

    addExtHostAlias(name);
  }

  /**
   * Adds an extension host alias, e.g. from a resin:import
   */
  public void addExtHostAlias(String name)
  {
    if (! _hostAliases.contains(name)) {
      _hostAliases.add(name);
    }
  }

  /**
   * Returns the host aliases.
   */
  public ArrayList<String> getHostAliases()
  {
    return _hostAliases;
  }

  /**
   * Adds an extension host alias, e.g. from a resin:import
   */
  public void addExtHostAliasRegexp(Pattern name)
  {
    if (! _hostAliasRegexps.contains(name))
      _hostAliasRegexps.add(name);
  }

  /**
   * Sets the regexp pattern
   */
  public void setRegexp(Pattern regexp)
  {
    _regexp = regexp;
  }
  
  @Override
  public DeployControllerType getControllerType()
  {
    if (_regexp != null)
      return DeployControllerType.DYNAMIC;
    else
      return super.getControllerType();
  }

  /**
   * Sets the root directory pattern
   */
  public void setRootDirectoryPattern(String rootDirectoryPattern)
  {
    _rootDirectoryPattern = rootDirectoryPattern;
  }

  /**
   * Adds a dependent file.
   */
  public void addDepend(PathImpl depend)
  {
    if (! _dependList.contains(depend))
      _dependList.add(new Depend(depend));
  }

  /**
   * Returns the host admin.
   */
  public HostMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Returns the deploy admin.
   */
  /*
  @Override
  protected DeployControllerAdmin getDeployAdmin(DeployHandle<Host> handle)
  {
    if (isErrorHost()) {
      return null;
    }
    
    return new HostAdmin(handle);
  }
  */

  public PathImpl getWarExpandDirectory()
  {
    return null;
  }

  /**
   * Initialize the entry.
   */
  @Override
  protected void initBegin()
  {
    try {
      /*
      try {
        if (getConfig() == null || getHostName() != null) {
        }
        else if (getConfig().getHostName() != null)
          setHostName(EL.evalString(getConfig().getHostName(),
                                    EL.getEnvironment()));
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      if (_regexpName != null && _hostName == null)
        _hostName = _regexpName;

      if (_hostName == null)
        _hostName = "";
      */
      

      ArrayList<String> aliases = null;

      if (getConfig() != null) {
        aliases = getConfig().getHostAliases();

        _entryHostAliasRegexps.addAll(getConfig().getHostAliasRegexps());
        _hostAliasRegexps.addAll(getConfig().getHostAliasRegexps());
      }
      
      Function<String, Object> env = ConfigContext.getEnvironment();
      
      for (int i = 0; aliases != null && i < aliases.size(); i++) {
        String alias = aliases.get(i);

        alias = ConfigContext.evalString(alias, env);

        addHostAlias(alias);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    super.initBegin();
  }

  /**
   * Returns the "name" property.
   */
  @Override
  protected String getMBeanId()
  {
    String name = getName();
    
    if (name == null)
      name = "";
    else if (name.indexOf(':') >= 0)
      name = name.replace(':', '-');

    if (name.equals("")) {
      return Host.DEFAULT_NAME;
    }
    else {
      return name;
    }
  }

  /**
   * Returns true for a matching name.
   */
  @Override
  public boolean isNameMatch(String name)
  {
    if (_hostName.equalsIgnoreCase(name)) {
      return true;
    }

    for (int i = _hostAliases.size() - 1; i >= 0; i--) {
      String alias = _hostAliases.get(i);
      
      if (name.equalsIgnoreCase(alias)) {
        return true;
      }
    }

    for (int i = _hostAliasRegexps.size() - 1; i >= 0; i--) {
      Pattern alias = _hostAliasRegexps.get(i);

      // server/1f60
      if (alias.matcher(name).matches()) {
        return true;
      }
    }

    if (_regexp != null) {
      // server/0523
      
      Matcher matcher = _regexp.matcher(name);

      if (matcher.matches()) {
        PathImpl rootDirectory = calculateRoot(matcher);

        if (getRootDirectory().equals(rootDirectory)) {
          return true;
        }
      }
    }
    
    return false;
  }

  private PathImpl calculateRoot(Matcher matcher)
  {
    // XXX: duplicates HostRegexp

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());

      if (_rootDirectoryPattern == null) {
        // server/129p
        return VfsOld.lookup();
      }
      
      int length = matcher.end() - matcher.start();

      ArrayList<String> vars = new ArrayList<String>();

      HashMap<String,Object> varMap = new HashMap<String,Object>();
        
      for (int j = 0; j <= matcher.groupCount(); j++) {
        vars.add(matcher.group(j));
        varMap.put("host" + j, matcher.group(j));
      }

      varMap.put("regexp", vars);
      varMap.put("host", new HostRegexpVar(matcher.group(0), vars));

      PathImpl path = PathBuilder.lookupPath(_rootDirectoryPattern, varMap);
      
      return path;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      // XXX: not quite right
      return VfsOld.lookup(_rootDirectoryPattern);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Merges two entries.
   */
  /*
  protected HostController merge(HostController newController)
  {
    if (getConfig() != null && getConfig().getRegexp() != null)
      return newController;
    else if (newController.getConfig() != null
             && newController.getConfig().getRegexp() != null)
      return this;
    else {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(getParentClassLoader());

        String id = newController.getId();
        
        HostController mergedController
          = new HostController(id,
                               getRootDirectory(),
                               newController.getHostName(),
                               _container);

        mergedController.mergeController(this);
        mergedController.mergeController(newController);

        if (! isNameMatch(newController.getHostName())
            && ! newController.isNameMatch(getHostName())) {
          ConfigException e;

          e = new ConfigException(L.l("Illegal merge of {0} and {1}.  Both hosts have the same root-directory '{2}'.",
                                      getId(),
                                      newController.getId(),
                                      getRootDirectory()));

          log.warning(e.getMessage());
          log.log(Level.FINEST, e.toString(), e);

          mergedController.setConfigException(e);
        }

        return mergedController;
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);

        return null;
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }
  }
  */

  /**
   * Merges with the old controller.
   */
  @Override
  public void merge(DeployController<Host> newControllerV)
  {
    super.merge(newControllerV);

    HostController newController = (HostController) newControllerV;
    
    _entryHostAliases.addAll(newController._entryHostAliases);
    if (! newController.getHostName().equals("")) {
      _entryHostAliases.add(newController.getHostName());
    }
    _entryHostAliasRegexps.addAll(newController._entryHostAliasRegexps);
    
    _hostAliases.addAll(newController._hostAliases);
    _hostAliasRegexps.addAll(newController._hostAliasRegexps);
    
    /*
    if (_regexp == null) {
      _regexp = newController._regexp;
      _rootDirectoryPattern = newController._rootDirectoryPattern;
    }
    */
  }

  /**
   * Creates a new instance of the host object.
   */
  @Override
  protected HostBuilder createInstanceBuilder()
  {
    return new HostBuilder(_container, this, _hostName);
  }

  /**
   * Creates the host.
   */
  @Override
  protected void configureInstance(DeployInstanceBuilder<Host> builder)
    throws Exception
  {
    /*
    _hostAliases.clear();
    _hostAliases.addAll(_entryHostAliases);
    */
    
    ConfigContext.setProperty("name", _hostName);
    ConfigContext.setProperty("host", _hostVar);

    for (Map.Entry<String,Object> entry : getVariableMap().entrySet()) {
      Object value = entry.getValue();
      
      if (value != null) {
        ConfigContext.setProperty(entry.getKey(), value);
      }
    }

    _hostAliases.clear();
    
    for (String alias : _entryHostAliases) {
      _hostAliases.add(ConfigContext.evalString(alias));
    }

    if (_container != null) {
      HostBuilder host = (HostBuilder) builder;
      
      /*
      for (EarConfig config : _container.getEarDefaultList()) {
        host.getWebAppContainer().addEarDefault(config);
      }
      */

      for (WebAppConfig config : _container.getWebAppDefaultList()) {
        host.getWebAppContainer().addWebAppDefault(config);
      }
    }

    super.configureInstance(builder);
  }

  @Override
  protected void extendJMXContext(Map<String,String> context)
  {
    context.put("Host", getMBeanId());
  }

  /**
   * Returns the appropriate log for debugging.
   */
  @Override
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Returns equality.
   */
  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof HostController))
      return false;

    HostController entry = (HostController) o;

    return _hostName.equals(entry._hostName);
  }

  /**
   * Returns a printable view.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }
}
