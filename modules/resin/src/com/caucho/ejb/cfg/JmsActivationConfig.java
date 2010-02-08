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

package com.caucho.ejb.cfg;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.Names;
import com.caucho.config.TagName;
import com.caucho.config.gen.ApiClass;
import com.caucho.config.gen.ApiMethod;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.gen.XaAnnotation;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.JndiBuilder;
import com.caucho.ejb.gen.MessageGenerator;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.message.*;
import com.caucho.ejb.server.AbstractServer;
import com.caucho.ejb.server.EjbProducer;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.jca.*;
import com.caucho.jms.JmsMessageListener;
import com.caucho.jca.cfg.*;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.AroundInvoke;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.resource.spi.*;
import javax.naming.NamingException;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import java.lang.reflect.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Set;

/**
 * Configuration for a JMS/JNDI based MessageDrivenBean.  Each property
 * of this class corresponds to an ActivationConfigProperty.
 */
@Configurable
public class JmsActivationConfig {
  private static final L10N L = new L10N(JmsActivationConfig.class);
  private String _connectionFactoryName;
  private String _destinationName;
  
  private Class<?> _destinationType = Queue.class;
  
  private int _maxPoolSize = 1;
  
  /**
   * The JNDI name of a configured ConnectionFactory. 
   */
  @Configurable
  @TagName({"ConnectionFactoryJndiName", "connectionFactoryJndiName"})
  public void setConnectionFactoryJndiName(String jndiName)
  {
    _connectionFactoryName = jndiName;
  }
  
  /**
   * The CDI @Named value for a configured ConnectionFactory. 
   */
  @Configurable
  @TagName({"ConnectionFactoryName", "connectionFactoryName"})
  public void setConnectionFactoryName(String name)
  {
    _connectionFactoryName = name;
  }  
 
  /**
   * The name of a configured ConnectionFactory. 
   */
  public String getConnectionFactoryName()
  {
    return _connectionFactoryName;
  }
    
  /**
   * The name of a configured queue or topic
   */
  public String getDestinationName()
  {
    return _destinationName;
  }
   
  /*
   * The jndi name of a configured queue or topic
   */
  @Configurable
  @TagName({"DestinationJndiName", "destinationJndiName"})
  public void setDestinationJndiName(String jndiName)
  {
    _destinationName = jndiName;
  }
  
  /*
   * The CDI @Named value of a configured queue or topic
   */
  @Configurable
  @TagName({"DestinationName", "destinationName"})
  public void setDestinationName(String jndiName)
  {
    setDestinationJndiName(jndiName);
  }
  
  /**
   * javax.jms.Queue or javax.jms.Topic
   */
  @Configurable
  @TagName({"DestinationType", "destinationType"})
  public void setDestinationType(Class<?> type)
  {
    if (! Queue.class.equals(type) && ! Topic.class.equals(type))
      throw new ConfigException(L.l("JMS DestinationType must be javax.jms.Queue or javax.jms.Topic"));
    
    _destinationType = type;
  }
  
  public Class <? extends Destination> getDestinationType()
  {
    return (Class<? extends Destination>) _destinationType;
  }
  
  /**
   * Configures the maximum pool of listeners.
   */
  @Configurable
  @TagName({"MaxPoolSize", "maxPoolSize", 
            "consumer-max", "message-consumer-max"})
  public void setMaxPoolSize(int poolSize)
  {
    _maxPoolSize = poolSize;
  }
  
  public int getMaxPoolSize()
  {
    return _maxPoolSize;
  }
}
