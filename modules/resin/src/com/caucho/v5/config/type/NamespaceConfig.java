/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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

package com.caucho.v5.config.type;

import java.util.HashMap;

import com.caucho.v5.config.cf.ConfigFileBaratine;
import com.caucho.v5.config.core.IfConfig;
import com.caucho.v5.config.core.ImportConfig;
import com.caucho.v5.config.core.MessageConfig;
import com.caucho.v5.i18n.CharacterEncoding;
import com.caucho.v5.javac.JavacConfig;
import com.caucho.v5.javac.TempDir;
import com.caucho.v5.javac.WorkDir;
import com.caucho.v5.loader.ClassLoaderConfig;
import com.caucho.v5.loader.ClasspathConfig;
import com.caucho.v5.loader.DependencyCheckInterval;
import com.caucho.v5.loader.SystemProperty;
import com.caucho.v5.log.impl.LogConfig;
import com.caucho.v5.log.impl.LogHandlerConfig;
import com.caucho.v5.log.impl.LoggerConfig;
import com.caucho.v5.log.impl.StderrLog;
import com.caucho.v5.log.impl.StdoutLog;
import com.caucho.v5.make.DependencyConfig;
import com.caucho.v5.vfs.CaseInsensitive;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

// configuration types
class NamespaceConfig {
  static final NamespaceConfig NS_DEFAULT;

  static final NamespaceConfig URN_BARATINE;
  
  private TypeFactoryConfig _factory;
  
  private String _ns = "";
  private boolean _isDefault;
  private PathImpl _path;

  private HashMap<String,NamespaceBeanConfig> _beanMap
    = new HashMap<>();
  
  protected NamespaceConfig(String ns, boolean isDefault)
  {
    _factory = TypeFactoryConfig.getFactory(getClass().getClassLoader());
    _isDefault = isDefault;
    
    setName(ns);
  }

  private void setName(String ns)
  {
    if ("default".equals(ns)) {
      ns = "";
    }

    _ns = ns;
  }

  public String getName()
  {
    return _ns;
  }

  public void setDefault(boolean isDefault)
  {
    _isDefault = isDefault;
  }

  public boolean isDefault()
  {
    return _isDefault;
  }

  public void setPath(String path)
  {
    if (path.indexOf(':') < 0)
      _path = VfsOld.lookup("classpath:" + path);
    else
      _path = VfsOld.lookup(path);
  }

  public PathImpl getPath()
  {
    return _path;
  }

  /*
  public void loadBeans()
  {
    if (_isBeansLoaded.getAndSet(true))
      return;

    try {
      new Config().configure(this, _path);
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  */

  public ConfigType<?> getBean(String name)
  {
    NamespaceBeanConfig beanConfig = _beanMap.get(name);

    if (beanConfig != null)
      return beanConfig.getConfigType();
    else
      return null;
  }

  public NamespaceBeanConfig createBean()
  {
    return new NamespaceBeanConfig(_factory, _ns, _isDefault);
  }

  public void addBean(NamespaceBeanConfig bean)
  {
    _beanMap.put(bean.getName(), bean);
  }

  public NamespaceFlowConfig createFlow()
  {
    return new NamespaceFlowConfig(_factory, _ns, _isDefault);
  }

  public void addFlow(NamespaceFlowConfig flow)
  {
    _beanMap.put(flow.getName(), flow);
  }
  
  protected void addFlow(String name, Class<?> cl)
  {
    addFlow(name, cl.getName());
  }
  
  protected void addFlow(String name, String className)
  {
    NamespaceFlowConfig bean = createFlow();
    bean.setName(name);
    bean.setClass(className);
    addFlow(bean);
  }
  
  protected void addBean(String name, Class<?> cl)
  {
    addBean(name, cl.getName());
  }
  
  protected void addBean(String name, String className)
  {
    NamespaceBeanConfig bean = createBean();
    bean.setName(name);
    bean.setClass(className);
    addBean(bean);
  }
  
  protected void initCore()
  {
    addFlow("config", ConfigFileBaratine.class);
    
    //addBean("import", ImportConfig.class);
    // addFlow("import", ImportConfigXml.class);
    addFlow("if", IfConfig.class);
    
    try {
      Class<?> importXml = Class.forName("com.caucho.v5.config.xml.ImportConfigXml");
      
      addFlow("import", importXml);
    } catch (Exception e) {
      addFlow("import", ImportConfig.class);
    }
    
    addBean("message", MessageConfig.class);
    
    addBean("value", ValueType.class);
  }
  
  protected void initExtensions()
  {
    addBean("case-insensitive", CaseInsensitive.class);
    addBean("character-encoding", CharacterEncoding.class);
    addBean("class-loader", ClassLoaderConfig.class);
    addBean("classpath", ClasspathConfig.class);

    addBean("dependency", DependencyConfig.class);
    addBean("dependency-check-interval", DependencyCheckInterval.class);
    
    //addBean("import", ImportConfig.class);
    addBean("if", IfConfig.class);
    
    addBean("javac", JavacConfig.class);
    
    addBean("list", ListType.class);
    addBean("log", LogConfig.class);
    addBean("log-handler", LogHandlerConfig.class);
    addBean("logger", LoggerConfig.class);
    
    addBean("map", MapType.class);
    // addBean("mbean", MBeanConfig.class);
    
    addBean("null", NullType.class);
    
    addBean("stderr-log", StderrLog.class);
    addBean("stdout-log", StdoutLog.class);
    addBean("system-property", SystemProperty.class);
    
    addBean("temp-dir", TempDir.class);
    addBean("temporary-directory", TempDir.class);
    
    addBean("value", ValueType.class);
    
    addBean("work-dir", WorkDir.class);
    addBean("work-directory", WorkDir.class);
  }
  
  static {
    NS_DEFAULT = new NamespaceConfig("", true);
    NS_DEFAULT.initCore();
    NS_DEFAULT.initExtensions();
    
    URN_BARATINE = new NamespaceConfig("urn:java:com.caucho.baratine", false);
    URN_BARATINE.initCore();
    URN_BARATINE.initExtensions();
   }
}
