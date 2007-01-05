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

package com.caucho.ejb.cfg;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.config.types.JndiBuilder;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.message.MessageServer;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.management.j2ee.J2EEManagedObject;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.ejb.MessageDrivenBean;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.naming.NamingException;
import java.lang.reflect.Modifier;

/**
 * Configuration for an ejb entity bean.
 */
public class EjbMessageBean extends EjbBean {
  private static L10N L = new L10N(EjbMessageBean.class);

  private ConnectionFactory _connectionFactory;
  private Destination _destination;
  private boolean _isContainerTransaction = true;
  private int _acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
  private String _selector;
  private String _subscriptionName;
  private String _destinationType;
  private String _destinationLink;
  private int _consumerMax = 5;

  /**
   * Creates a new message bean configuration.
   */
  public EjbMessageBean(EjbConfig config, String ejbModuleName)
  {
    super(config, ejbModuleName);

    _consumerMax = config.getEJBManager().getMessageConsumerMax();
  }

  /**
   * Returns the kind of bean.
   */
  public String getEJBKind()
  {
    return "message";
  }

  /**
   * Sets the ejb implementation class.
   */
  @Override
  public void setEJBClass(String ejbType)
    throws ConfigException
  {
    super.setEJBClass(ejbType);
    
    Class ejbClass = getEJBClass();

    if (! MessageListener.class.isAssignableFrom(ejbClass))
      throw error(L.l("'{0}' must implement javax.jms.MessageListener.  Every message-driven bean must implement MessageListener.", ejbClass.getName()));

    if (! MessageDrivenBean.class.isAssignableFrom(ejbClass) &&
	! isAllowPOJO())
      throw error(L.l("'{0}' must implement javax.ejb.MessageDrivenBean.  Every message-driven bean must implement MessageDrivenBean.", ejbClass.getName()));


    if (Modifier.isAbstract(ejbClass.getModifiers()))
      throw error(L.l("'{0}' must not be abstract.  Every message-driven bean must be a fully-implemented class.",
		      ejbClass.getName()));

    JMethod create = getMethod(getEJBClassWrapper(), "ejbCreate", new JClass[] {});

    if (create == null) {
      if (! isAllowPOJO()) {
	throw error(L.l("{0}: ejbCreate() method is missing.  Every message-driven bean must have an ejbCreate() method.",
		      ejbClass.getName()));
      }
    }
    else if (! create.isPublic()) {
      throw error(L.l("{0}: ejbCreate() must be public.  Every message-driven bean must have a public ejbCreate method.",
		      ejbClass.getName()));
    }
  }

  /**
   * Creates the old EJB 2.0 message-driven-destination
   */
  public MessageDrivenDestination createMessageDrivenDestination()
  {
    return new MessageDrivenDestination();
  }

  /**
   * Sets the JMS destination.
   */
  public void setDestination(JndiBuilder destination)
    throws ConfigException, NamingException
  {
    if (! (destination.getObject() instanceof Destination))
      throw new ConfigException(L.l("`{0}' needs to implement javax.jms.Destination.",
				    destination.getObject()));
    
    _destination = (Destination) destination.getObject();
  }

  /**
   * Returns the destination.
   */
  public Destination getDestination()
  {
    return _destination;
  }

  /**
   * Sets the JMS destination type.
   */
  public void setMessageDestinationType(String type)
    throws ConfigException, NamingException
  {
    _destinationType = type;
  }

  /**
   * Sets the JMS destination link
   */
  public void setMessageDestinationLink(String link)
    throws ConfigException, NamingException
  {
    _destinationLink = link;
  }

  /**
   * Sets the connection factory.
   */
  public void setConnectionFactory(JndiBuilder factory)
    throws ConfigException, NamingException
  {
    if (! (factory.getObject() instanceof ConnectionFactory))
      throw new ConfigException(L.l("`{0}' needs to implement javax.jms.ConnectionFactory.",
				    factory.getObject()));
    
    _connectionFactory = (ConnectionFactory) factory.getObject();
  }

  /**
   * Returns the destination.
   */
  public ConnectionFactory getConnectionFactory()
  {
    return _connectionFactory;
  }

  /**
   * Returns true if the container handles transactions.
   */
  public boolean getContainerTransaction()
  {
    return _isContainerTransaction;
  }

  /**
   * Set true if the container handles transactions.
   */
  public void setContainerTransaction(boolean isContainerTransaction)
  {
    _isContainerTransaction = isContainerTransaction;
  }

  /**
   * Returns the acknowledge mode.
   */
  public int getAcknowledgeMode()
  {
    return _acknowledgeMode;
  }

  /**
   * Set the acknowledge mode.
   */
  public void setAcknowledgeMode(int acknowledgeMode)
  {
    _acknowledgeMode = acknowledgeMode;
  }

  /**
   * Returns the message selector
   */
  public String getSelector()
  {
    return _selector;
  }

  /**
   * Set the message selector.
   */
  public void setSelector(String selector)
  {
    _selector = selector;
  }

  /**
   * Returns the durable subscription name
   */
  public String getSubscriptionName()
  {
    return _subscriptionName;
  }

  /**
   * Set the message selector.
   */
  public void setSubscriptionName(String subscriptionName)
  {
    _subscriptionName = subscriptionName;
  }

  /**
   * Set true if the container handles transactions.
   */
  public void setTransactionType(String type)
    throws ConfigException
  {
    if (type.equals("Container")) {
    }
    else if (type.equals("Bean")) {
    }
    else
      throw new ConfigException(L.l("`{0}' is an unknown transaction-type.  transaction-type must be `Bean' or `Container'.", type));
  }

  public void setSecurityIdentity(SecurityIdentity identity)
  {
  }

  /**
   * Adds the activation config.
   */
  public ActivationConfig createActivationConfig()
  {
    return new ActivationConfig();
  }

  /**
   * Sets the number of message consumers.
   */
  public void setMessageConsumerMax(int consumerMax)
    throws ConfigException
  {
    _consumerMax = consumerMax;
  }

  /**
   * Initialize
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    J2EEManagedObject.register(new com.caucho.management.j2ee.MessageDrivenBean(this));
  }

  /**
   * Deploys the bean.
   */
  public AbstractServer deployServer(EjbServerManager ejbManager,
				     JavaClassGenerator javaGen)
    throws ClassNotFoundException
  {
    MessageServer server = new MessageServer(ejbManager);

    server.setModuleName(getEJBModuleName());
    server.setEJBName(getEJBName());

    //Class contextImplClass = javaGen.loadClass(getSkeletonName());
    //server.setContextImplClass(contextImplClass);
    
    server.setContextImplClass(getEJBClass());
    server.setDestination(_destination);
    server.setConsumerMax(_consumerMax);
    
    server.setInitProgram(getInitProgram());

    return server;
  }

  public class ActivationConfig {
    public void addActivationConfigProperty(ActivationConfigProperty prop)
    {
    }
  }

  public static class ActivationConfigProperty {
    String _name;
    String _value;

    public void setActivationConfigPropertyName(String name)
    {
      _name = name;
    }

    public void setActivationConfigPropertyValue(String value)
    {
      _value = value;
    }
  }

  public class MessageDrivenDestination {
    public void setDestinationType(String value)
      throws ConfigException, NamingException
    {
      setMessageDestinationType(value);
    }
    
    public void setSubscriptionDurability(String durability)
    {
    }
    
    public void setJndiName(JndiBuilder destination)
      throws ConfigException, NamingException
    {
      setDestination(destination);
    }
  }
}
