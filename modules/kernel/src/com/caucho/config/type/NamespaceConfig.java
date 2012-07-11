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

package com.caucho.config.type;

import java.util.HashMap;

import com.caucho.config.core.ResinChoose;
import com.caucho.config.core.ResinIf;
import com.caucho.config.core.ResinImport;
import com.caucho.config.core.ResinLog;
import com.caucho.config.core.ResinProperties;
import com.caucho.config.core.ResinSet;
import com.caucho.config.core.ResinSystemConfig;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

// configuration types
class NamespaceConfig {
  static final NamespaceConfig NS_RESIN;
  static final NamespaceConfig NS_DEFAULT;
  static final NamespaceConfig NS_RESIN_CORE;
  static final NamespaceConfig URN_RESIN;
  
  private TypeFactory _factory;
  
  private String _ns = "";
  private boolean _isDefault;
  private Path _path;

  private HashMap<String,NamespaceBeanConfig> _beanMap
    = new HashMap<String,NamespaceBeanConfig>();
  
  private NamespaceConfig(String ns, boolean isDefault)
  {
    _factory = TypeFactory.getFactory(getClass().getClassLoader());
    _isDefault = isDefault;
    
    setName(ns);
  }

  private void setName(String ns)
  {
    if ("default".equals(ns))
      ns = "";

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
      _path = Vfs.lookup("classpath:" + path);
    else
      _path = Vfs.lookup(path);
  }

  public Path getPath()
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
  
  private void addFlow(String name, Class<?> cl)
  {
    addFlow(name, cl.getName());
  }
  
  private void addFlow(String name, String className)
  {
    NamespaceFlowConfig bean = createFlow();
    bean.setName(name);
    bean.setClass(className);
    addFlow(bean);
  }
  
  private void addBean(String name, Class<?> cl)
  {
    addBean(name, cl.getName());
  }
  
  private void addBean(String name, String className)
  {
    NamespaceBeanConfig bean = createBean();
    bean.setName(name);
    bean.setClass(className);
    addBean(bean);
  }
  
  private void initCore()
  {
    addFlow("choose", ResinChoose.class);
    addFlow("if", ResinIf.class);
    addFlow("properties", ResinProperties.class);
    
    addBean("import", ResinImport.class);
    addBean("log", ResinLog.class);
    addBean("message", ResinLog.class);
    addBean("set", ResinSet.class);
    addBean("system", ResinSystemConfig.class);
    
    addBean("value", "com.caucho.config.type.ValueType");
  }
  
  private void initResin()
  {
    addBean("authenticator", "com.caucho.security.Authenticator");
    addBean("bam-service", "com.caucho.hemp.broker.BamServiceConfig");
    addBean("bean", "com.caucho.config.cfg.BeanConfig");
    addBean("case-insensitive", "com.caucho.vfs.CaseInsensitive");
    addBean("character-encoding", "com.caucho.i18n.CharacterEncoding");
    addBean("choose", "com.caucho.config.core.ResinChoose");
    addBean("class-loader", "com.caucho.loader.ClassLoaderConfig");
    addBean("classpath", "com.caucho.loader.ClasspathConfig");
    addBean("class-update-interval", "com.caucho.loader.DependencyCheckInterval");
    addBean("component", "com.caucho.config.cfg.WbComponentConfig");
    addBean("connection-factory", "com.caucho.jca.cfg.ConnectionFactoryConfig");
    addBean("connector", "com.caucho.jca.ra.ConnectorConfig");
    addBean("database", "com.caucho.sql.DBPool");
    addBean("database-default", "com.caucho.sql.DatabaseConfig");
    addBean("data-source", "com.caucho.config.types.DataSourceRef");
    addBean("dependency", "com.caucho.make.DependencyConfig");
    addBean("dependency-check-interval", "com.caucho.loader.DependencyCheckInterval");
    addBean("ejb-local-ref", "com.caucho.config.types.EjbLocalRef");
    addBean("ejb-message-bean", "com.caucho.ejb.cfg.MessageBeanConfig");
    addBean("ejb-ref", "com.caucho.config.types.EjbRef");
    addBean("ejb-server", "com.caucho.ejb.EJBServer");
    addBean("ejb-stateless-bean", "com.caucho.ejb.cfg.StatelessBeanConfig");
    addBean("ejb-stateful-bean", "com.caucho.ejb.cfg.StatefulBeanConfig");
    addBean("env-entry", "com.caucho.config.types.EnvEntry");
    addBean("if", "com.caucho.config.core.ResinIf");
    addBean("import", "com.caucho.config.core.ResinImport");
    addBean("include", "com.caucho.config.core.ResinInclude");
    addBean("interceptor", "com.caucho.config.cfg.InterceptorConfig");
    addBean("java", "com.caucho.java.JavacConfig");
    addBean("javac", "com.caucho.java.JavacConfig");
    addBean("jndi-link", "com.caucho.naming.LinkProxy");
    addBean("jms-connection-factory", "com.caucho.jms.cfg.JmsConnectionFactoryConfig");
    addBean("jms-queue", "com.caucho.jms.cfg.JmsQueueConfig");
    addBean("jms-topic", "com.caucho.jms.cfg.JmsTopicConfig");
    addBean("jpa-persistence", "com.caucho.amber.cfg.PersistenceManager");
    addBean("jpa-persistence-unit", "com.caucho.env.jpa.ConfigJpaPersistenceUnit");
    addBean("jpa-persistence-unit-default", "com.caucho.env.jpa.ConfigJpaPersistenceUnitDefault");
    
    addBean("list", "com.caucho.config.type.ListType");
    addBean("log", "com.caucho.log.LogConfig");
    addBean("log-handler", "com.caucho.log.LogHandlerConfig");
    addBean("logger", "com.caucho.log.LoggerConfig");
    
    addBean("mail", "com.caucho.jca.cfg.JavaMailConfig");
    addBean("map", "com.caucho.config.type.MapType");
    addBean("mbean", "com.caucho.jmx.MBeanConfig");
    addBean("message", "com.caucho.config.core.ResinLog");
    addBean("message-destination-ref", "com.caucho.config.types.MessageDestinationRef");
    
    addBean("null", "com.caucho.config.type.NullType");
    
    addBean("persistent-store", "com.caucho.server.distcache.PersistentStoreConfig");
    addBean("persistence-manager", "com.caucho.amber.cfg.PersistenceManager");
    addBean("persistence-unit-ref", "com.caucho.config.types.PersistenceUnitRef");
    
    addBean("rar-deploy", "com.caucho.jca.ra.ResourceDeploy");
    addBean("reference", "com.caucho.config.types.ReferenceConfig");
    addBean("remote-client", "com.caucho.remote.client.RemoteClient");
    addBean("resource", "com.caucho.jca.cfg.Resource");
    addBean("resource-adapter", "com.caucho.jca.cfg.ResourceAdapterBeanConfig");
    addBean("resource-default", "com.caucho.jca.ra.ResourceDefault");
    addBean("resource-deploy", "com.caucho.jca.ra.ResourceDeploy");
    addBean("resource-env-ref", "com.caucho.config.types.ResourceEnvRef");
    addBean("resource-manager", "com.caucho.jca.ra.ResourceManagerConfig");
    addBean("resource-ref", "com.caucho.jca.ra.ResourceRef");
    addBean("role-map", "com.caucho.security.RoleMap");
    
    addBean("scheduled-task", "com.caucho.resources.ScheduledTaskConfig");
    addBean("security-role-ref", "com.caucho.config.types.SecurityRoleRef");
    addBean("servlet-classloader-hack", "com.caucho.loader.ServletClassloaderHack");
    addBean("set", "com.caucho.config.core.ResinSet");
    addBean("stderr-log", "com.caucho.log.StderrLog");
    addBean("stdout-log", "com.caucho.log.StdoutLog");
    addBean("system-property", "com.caucho.loader.SystemProperty");
    
    addBean("temp-dir", "com.caucho.java.TempDir");
    addBean("temporary-directory", "com.caucho.java.TempDir");
    
    addBean("value", "com.caucho.config.type.ValueType");
    
    addBean("web-service-client", "com.caucho.remote.client.RemoteClient");
    addBean("work-dir", "com.caucho.java.WorkDir");
    addBean("work-directory", "com.caucho.java.WorkDir");
  }
  
  static {
    NS_RESIN = new NamespaceConfig("http://caucho.com/ns/resin", true);
    NS_RESIN.initResin();
    
    NS_RESIN_CORE = new NamespaceConfig("http://caucho.com/ns/resin/core", false);
    NS_RESIN_CORE.initCore();
    
    NS_DEFAULT= new NamespaceConfig("", true);
    NS_DEFAULT.initCore();
    NS_DEFAULT.initResin();
    
    URN_RESIN = new NamespaceConfig("urn:java:com.caucho.resin", false);
    URN_RESIN.initCore();
  }
}
