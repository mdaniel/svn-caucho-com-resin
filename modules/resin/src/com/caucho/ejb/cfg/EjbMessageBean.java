/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.j2ee.InjectIntrospector;
import com.caucho.config.types.JndiBuilder;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.message.MessageServer;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.jca.*;
import com.caucho.management.j2ee.J2EEManagedObject;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenBean;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.AroundInvoke;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.naming.NamingException;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;

/**
 * Configuration for an ejb entity bean.
 */
public class EjbMessageBean extends EjbBean {
  private static final Logger log
    = Logger.getLogger(EjbMessageBean.class.getName());
  private static final L10N L = new L10N(EjbMessageBean.class);

  private ConnectionFactory _connectionFactory;
  private Destination _destination;
  private String _messageSelector;
  private boolean _isContainerTransaction = true;
  private int _acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
  private String _selector;
  private String _subscriptionName;
  private int _consumerMax = -1;
  private String _messageDestinationLink;
  private Class _messagingType;

  /**
   * Creates a new message bean configuration.
   */
  public EjbMessageBean(EjbConfig config, String ejbModuleName)
  {
    super(config, ejbModuleName);
  }

  /**
   * Returns the kind of bean.
   */
  @Override
  public String getEJBKind()
  {
    return "message";
  }

  /**
   * Sets the ejb implementation class.
   */
  @Override
  public void setEJBClass(Class ejbClass)
    throws ConfigException
  {
    super.setEJBClass(ejbClass);

    // ejb/0987
    /*
      if (! MessageDrivenBean.class.isAssignableFrom(ejbClass)
      && ! isAllowPOJO())
      throw error(L.l("'{0}' must implement javax.ejb.MessageDrivenBean.  Every message-driven bean must implement MessageDrivenBean.", ejbClass.getName()));
    */

    if (Modifier.isAbstract(ejbClass.getModifiers()))
      throw error(L.l("'{0}' must not be abstract.  Every message-driven bean must be a fully-implemented class.",
                      ejbClass.getName()));

    // ejb 3.0 simplified section 10.1.3
    // The name annotation element defaults to the unqualified name of the bean
    // class.

    if (getEJBName() == null) {
      setEJBName(ejbClass.getSimpleName());
    }

    /* XXX: ejb/0fbl, EJB 3.0 should not need ejbCreate()
       ApiMethod create = getEJBClassWrapper().getMethod("ejbCreate",
       new Class[0]);

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
    */
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
    Object obj = destination.getObject();

    if (! (obj instanceof Destination))
      throw new ConfigException(L.l("'{0}' needs to implement javax.jms.Destination.",
                                    obj));

    _destination = (Destination) obj;
  }

  /**
   * Sets the JMS destination.
   */
  public void setDestinationValue(Destination destination)
  {
    _destination = destination;
  }

  public void setMessagingType(Class messagingType)
  {
    if (messagingType != Object.class)
      _messagingType = messagingType;
  }

  /**
   * Returns the destination.
   */
  public Destination getDestination()
  {
    return _destination;
  }

  /**
   * @deprecated for compat with TCK
   */
  public void setMappedName(JndiBuilder destination)
    throws ConfigException, NamingException
  {
    setDestination(destination);
  }

  /**
   * Sets the JMS destination type.
   */
  public void setMessageDestinationType(String type)
    throws ConfigException, NamingException
  {
  }

  /**
   * Sets the JMS destination link
   */
  public void setMessageDestinationLink(String messageDestinationLink)
    throws ConfigException, NamingException
  {
    _messageDestinationLink = messageDestinationLink;
  }

  /**
   * Sets the connection factory.
   */
  public void setConnectionFactory(JndiBuilder factory)
    throws ConfigException, NamingException
  {
    if (! (factory.getObject() instanceof ConnectionFactory))
      throw new ConfigException(L.l("'{0}' needs to implement javax.jms.ConnectionFactory.",
                                    factory.getObject()));

    _connectionFactory = (ConnectionFactory) factory.getObject();
  }

  /**
   * Sets the connection factory.
   */
  public void setConnectionFactoryValue(ConnectionFactory factory)
  {
    _connectionFactory = factory;
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
      _isContainerTransaction = true;
    }
    else if (type.equals("Bean")) {
      _isContainerTransaction = false;
    }
    else
      throw new ConfigException(L.l("'{0}' is an unknown transaction-type.  transaction-type must be 'Bean' or 'Container'.", type));
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


  public void setResourceAdapter(String name)
  {
    ResourceArchive ra = ResourceArchiveManager.findResourceArchive(name);

    if (ra == null)
      throw new ConfigException(L.l("'{0}' is an unknown resource-adapter"));
  }

  private void addActivationConfigProperty(String name, String value)
  {
    if ("destination".equals(name)) {
      JndiBuilder jndiBuilder = new JndiBuilder();
      jndiBuilder.addText(value);
      try {
        setDestination(jndiBuilder);
      }
      catch (NamingException e) {
        throw ConfigException.create(e);
      }
    }
    else if ("messageSelector".equals(name)) {
      _messageSelector = value;
    }
    else
      log.log(Level.FINE, L.l("activation-config-property '{0}' is unknown, ignored",
                              name));
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
  @Override
  public void init()
    throws ConfigException
  {
    super.init();

    if (! MessageListener.class.isAssignableFrom(getEJBClass())
        && _messagingType == null)

      throw error(L.l("'{0}' must implement javax.jms.MessageListener or specify {1}.",
                      getEJBClass().getName(),
                      isAllowPOJO() ? "messaging-type" : "messageListenerInterface"));

    // J2EEManagedObject.register(new com.caucho.management.j2ee.MessageDrivenBean(this));
  }

  /**
   * Obtain and apply initialization from annotations.
   */
  @Override
  public void initIntrospect()
    throws ConfigException
  {
    // ejb/0fbm
    super.initIntrospect();

    ApiClass type = getEJBClassWrapper();

    // ejb/0j40
    if (! type.isAnnotationPresent(MessageDriven.class)
        && ! type.isAnnotationPresent(MessageDriven.class)
        && ! isAllowPOJO())
      return;

    // XXX: annotations in super classes?

    javax.ejb.MessageDriven messageDriven
      = type.getAnnotation(javax.ejb.MessageDriven.class);

    if (messageDriven != null) {
      ActivationConfigProperty[] properties
        = messageDriven.activationConfig();

      if (properties != null) {
        for (ActivationConfigProperty property : properties)
          addActivationConfigProperty(property.propertyName(),
                                      property.propertyValue());
      }

      Class messageListenerInterface
        = messageDriven.messageListenerInterface();

      if (messageListenerInterface != null)
        setMessagingType(messageListenerInterface);

      TransactionManagement transaction = type.getAnnotation(TransactionManagement.class);
      if (transaction == null)
        setTransactionType("Container");
      else if (TransactionManagementType.BEAN.equals(transaction.value()))
        setTransactionType("Bean");
      else
        setTransactionType("Container");

      configureMethods(type);
    }
  }

  private void configureMethods(ApiClass type)
    throws ConfigException
  {
    for (ApiMethod method : type.getMethods()) {
      AroundInvoke aroundInvoke = method.getAnnotation(AroundInvoke.class);

      // ejb/0fbl
      if (aroundInvoke != null) {
        setAroundInvokeMethodName(method.getName());

        // XXX: needs to check invalid duplicated @AroundInvoke methods.
        break;
      }
    }
  }

  /**
   * Deploys the bean.
   */
  @Override
  public AbstractServer deployServer(EjbContainer ejbManager,
                                     JavaClassGenerator javaGen)
    throws ClassNotFoundException
  {
    MessageServer server = new MessageServer(ejbManager);

    server.setModuleName(getEJBModuleName());
    server.setEJBName(getEJBName());
    server.setMappedName(getMappedName());
    server.setId(getEJBModuleName() + "#" + getMappedName());

    //Class contextImplClass = javaGen.loadClass(getSkeletonName());
    //server.setContextImplClass(contextImplClass);

    server.setContainerTransaction(getContainerTransaction());

    server.setEjbClass(getEJBClass());

    server.setContextImplClass(getEJBClass());
    server.setMessageListenerType(_messagingType);
    server.setAroundInvokeMethodName(getAroundInvokeMethodName());
    server.setInterceptors(getInterceptors());

    if (_connectionFactory != null)
      server.setConnectionFactory(_connectionFactory);
    else
      server.setConnectionFactory(getEjbContainer().getJmsConnectionFactory());

    if (_destination != null)
      server.setDestination(_destination);

    server.setMessageSelector(_messageSelector);
    server.setMessageDestinationLink(_messageDestinationLink);

    if (_consumerMax > 0)
      server.setConsumerMax(_consumerMax);
    else
      server.setConsumerMax(getEjbContainer().getMessageConsumerMax());

    Class beanClass = javaGen.loadClass(getEJBClass().getName());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(server.getClassLoader());

      ContainerProgram initContainer = getInitProgram();

      /*
      ArrayList<BuilderProgram> initList;
      initList = InjectIntrospector.introspect(beanClass);

      if (initList != null && initList.size() > 0) {
        if (initContainer == null)
          initContainer = new ContainerProgram();

        for (BuilderProgram init : initList) {
          initContainer.addProgram(init);
        }
      }
      */

      server.setInitProgram(initContainer);

      try {
        if (getServerProgram() != null)
          getServerProgram().configure(server);
      } catch (ConfigException e) {
        throw e;
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return server;
  }

  public class ActivationConfig {
    public void addActivationConfigProperty(ActivationConfigPropertyConfig prop)
      throws NamingException
    {
      String name = prop.getActivationConfigPropertyName();
      String value = prop.getActivationConfigPropertyValue();

      EjbMessageBean.this.addActivationConfigProperty(name, value);
    }
  }

  public static class ActivationConfigPropertyConfig {
    String _name;
    String _value;

    public void setActivationConfigPropertyName(String name)
    {
      _name = name;
    }

    public String getActivationConfigPropertyName()
    {
      return _name;
    }

    public void setActivationConfigPropertyValue(String value)
    {
      _value = value;
    }

    public String getActivationConfigPropertyValue()
    {
      return _value;
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
