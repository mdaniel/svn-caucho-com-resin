/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.config.type;

import com.caucho.v5.config.cfg.BeanConfig;
import com.caucho.v5.config.core.MessageConfig;
import com.caucho.v5.config.core.ResinChoose;
import com.caucho.v5.config.core.IfConfig;
import com.caucho.v5.config.core.ResinProperties;
import com.caucho.v5.config.core.ResinSet;
import com.caucho.v5.config.core.ResinSystemConfig;
import com.caucho.v5.config.types.DataSourceRef;
import com.caucho.v5.config.types.EnvEntry;
import com.caucho.v5.config.xml.ImportConfigXml;
import com.caucho.v5.env.jpa.ConfigJpaPersistenceUnit;
import com.caucho.v5.env.jpa.ConfigJpaPersistenceUnitDefault;
import com.caucho.v5.java.JavacConfig;
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
import com.caucho.v5.naming.LinkProxy;
import com.caucho.v5.resources.ScheduledTaskConfig;
import com.caucho.v5.sql.DBPool;
import com.caucho.v5.sql.DatabaseConfig;

// configuration types
class NamespaceConfigResin extends NamespaceConfig
{
  static final NamespaceConfig NS_RESIN;
  static final NamespaceConfig NS_RESIN_CORE;
  static final NamespaceConfig URN_RESIN;
  static final NamespaceConfig URN_RESIN_V5;
  static final NamespaceConfig NS_JAVAEE;
  static final NamespaceConfig NS_J2EE;
  
  NamespaceConfigResin(String ns, boolean isDefault)
  {
    super(ns, isDefault);
  }

  @Override
  protected void initCore()
  {
    super.initCore();
    
    addFlow("choose", ResinChoose.class);
    //addFlow("if", IfConfig.class);
    addFlow("properties", ResinProperties.class);
    
    addBean("import", ImportConfigXml.class);
    addBean("log", MessageConfig.class);
    addBean("message", MessageConfig.class);
    addBean("set", ResinSet.class);
    addBean("system", ResinSystemConfig.class);
    
    addBean("value", ValueType.class);
  }
  
  @Override
  protected void initExtensions()
  {
    super.initExtensions();
    
    addBean("authenticator", "com.caucho.security.Authenticator");
    
    addBean("bean", BeanConfig.class);
    
    addBean("case-insensitive", "com.caucho.v5.vfs.CaseInsensitive");
    addBean("character-encoding", "com.caucho.v5.i18n.CharacterEncoding");
    addBean("choose", ResinChoose.class);
    addBean("class-loader", ClassLoaderConfig.class);
    addBean("classpath", ClasspathConfig.class);
    addBean("class-update-interval", "com.caucho.v5.loader.DependencyCheckInterval");
    addBean("component", "com.caucho.v5.config.cfg.WbComponentConfig");
    addBean("connection-factory", "com.caucho.v5.jca.cfg.ConnectionFactoryConfig");
    addBean("connector", "com.caucho.v5.jca.ra.ConnectorConfig");
    
    addBean("database", DBPool.class);
    addBean("database-default", DatabaseConfig.class);
    addBean("data-source", DataSourceRef.class);
    addBean("dependency", DependencyConfig.class);
    addBean("dependency-check-interval", DependencyCheckInterval.class);
    
    /*
    addBean("ejb-local-ref", "com.caucho.v5.config.types.EjbLocalRef");
    addBean("ejb-message-bean", "com.caucho.ejb.cfg.MessageBeanConfig");
    addBean("ejb-ref", "com.caucho.v5.config.types.EjbRef");
    addBean("ejb-server", "com.caucho.ejb.EJBServer");
    addBean("ejb-stateless-bean", "com.caucho.ejb.cfg.StatelessBeanConfig");
    addBean("ejb-stateful-bean", "com.caucho.ejb.cfg.StatefulBeanConfig");
    */
    addBean("env-entry", EnvEntry.class);
    
    //addBean("if", IfConfig.class);
    addBean("import", ImportConfigXml.class);
    // addBean("include", ResinInclude.class);
    // addBean("interceptor", "com.caucho.v5.config.cfg.InterceptorConfig");
    
    addBean("java", JavacConfig.class);
    addBean("javac", JavacConfig.class);
    addBean("jndi-link", LinkProxy.class);
    //addBean("jms-connection-factory", "com.caucho.jms.cfg.JmsConnectionFactoryConfig");
    //addBean("jms-queue", "com.caucho.jms.cfg.JmsQueueConfig");
    //addBean("jms-topic", "com.caucho.jms.cfg.JmsTopicConfig");
    // addBean("jpa-persistence", "com.caucho.amber.cfg.PersistenceManager");
    addBean("jpa-persistence-unit", ConfigJpaPersistenceUnit.class);
    addBean("jpa-persistence-unit-default", ConfigJpaPersistenceUnitDefault.class);
    
    addBean("list", ListType.class);
    addBean("log", LogConfig.class);
    addBean("log-handler", LogHandlerConfig.class);
    addBean("logger", LoggerConfig.class);
    
    addBean("mail", "com.caucho.v5.jca.cfg.JavaMailConfig");
    addBean("map", "com.caucho.v5.config.type.MapType");
    addBean("mbean", "com.caucho.v5.jmx.MBeanConfig");
    // addBean("message", ResinLog.class);
    addBean("message-destination-ref", "com.caucho.v5.config.types.MessageDestinationRef");
    
    addBean("null", NullType.class);
    
    // addBean("persistent-store", PersistentStoreConfig.class);
    addBean("persistence-unit-ref", "com.caucho.v5.config.types.PersistenceUnitRef");
    
    //addBean("rar-deploy", "com.caucho.jca.ra.ResourceDeploy");
    addBean("reference", "com.caucho.v5.config.types.ReferenceConfig");
    addBean("remote-client", "com.caucho.v5.remote.client.RemoteClient");
    //addBean("resource", "com.caucho.jca.cfg.Resource");
    //addBean("resource-adapter", "com.caucho.jca.cfg.ResourceAdapterBeanConfig");
    //addBean("resource-default", "com.caucho.jca.ra.ResourceDefault");
    //addBean("resource-deploy", "com.caucho.jca.ra.ResourceDeploy");
    //addBean("resource-env-ref", "com.caucho.v5.config.types.ResourceEnvRef");
    //addBean("resource-manager", "com.caucho.jca.ra.ResourceManagerConfig");
    //addBean("resource-ref", "com.caucho.v5.config.types.ResourceRef");
    addBean("role-map", "com.caucho.v5.security.RoleMap");
    
    addBean("scheduled-task", ScheduledTaskConfig.class);
    addBean("security-role-ref", "com.caucho.v5.config.types.SecurityRoleRef");
    addBean("servlet-classloader-hack", "com.caucho.v5.loader.ServletClassloaderHack");
    addBean("set", "com.caucho.v5.config.core.ResinSet");
    addBean("stderr-log", StderrLog.class);
    addBean("stdout-log", StdoutLog.class);
    addBean("system-property", SystemProperty.class);
    
    addBean("temp-dir", "com.caucho.v5.java.TempDir");
    addBean("temporary-directory", "com.caucho.v5.java.TempDir");
    
    addBean("value", "com.caucho.v5.config.type.ValueType");
    
    addBean("web-service-client", "com.caucho.v5.remote.client.RemoteClient");
    addBean("work-dir", "com.caucho.v5.java.WorkDir");
    addBean("work-directory", "com.caucho.v5.java.WorkDir");
  }
  
  static {
    NS_RESIN = new NamespaceConfigResin("http://caucho.com/ns/resin", true);
    NS_RESIN.initCore();
    NS_RESIN.initExtensions();
    
    NS_JAVAEE = new NamespaceConfigResin("http://java.sun.com/xml/ns/javaee", true);
    NS_JAVAEE.initCore();
    NS_JAVAEE.initExtensions();
    
    NS_J2EE = new NamespaceConfigResin("http://java.sun.com/xml/ns/j2ee", true);
    NS_J2EE.initCore();
    NS_J2EE.initExtensions();
    
    NS_RESIN_CORE = new NamespaceConfigResin("http://caucho.com/ns/resin/core", false);
    NS_RESIN_CORE.initCore();
    
    URN_RESIN = new NamespaceConfigResin("urn:java:com.caucho.resin", false);
    URN_RESIN.initCore();
    URN_RESIN.initExtensions();
    
    URN_RESIN_V5 = new NamespaceConfigResin("urn:java:com.caucho.v5.resin", false);
    URN_RESIN_V5.initCore();
    URN_RESIN_V5.initExtensions();
  }
}
