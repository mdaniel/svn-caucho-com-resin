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

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Names;
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
import com.caucho.jca.ra.ResourceArchive;
import com.caucho.jca.ra.ResourceArchiveManager;
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
import javax.jms.Session;
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
 * Configuration for an ejb entity bean.
 */
public class EjbMessageBean extends EjbBean {
  private static final Logger log
    = Logger.getLogger(EjbMessageBean.class.getName());
  private static final L10N L = new L10N(EjbMessageBean.class);

  private ConnectionFactory _connectionFactory;

  private ActivationSpec _activationSpec;
  
  private JmsActivationConfig _jmsActivationConfig
    = new JmsActivationConfig();
  
  // private Destination _destination;
  private String _messageSelector;
  private int _acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
  private String _selector;
  private String _subscriptionName;
  // private int _consumerMax = -1;
  private String _messageDestinationLink;
  private Class<?> _messagingType;
  
  private MessageGenerator _messageBean;

  /**
   * Creates a new message bean configuration.
   */
  public EjbMessageBean(EjbConfig config, String ejbModuleName)
  {
    super(config, ejbModuleName);
  }

  /**
   * Creates a new session bean configuration.
   */
  public EjbMessageBean(EjbConfig ejbConfig,
                        AnnotatedType<?> annType,
                        MessageDriven messageDriven)
  {
    super(ejbConfig, annType, messageDriven.name());
  }


  /**
   * Creates a new session bean configuration.
   */
  public EjbMessageBean(EjbConfig ejbConfig,
                        AnnotatedType<?> annType,
                        String ejbName)
  {
    super(ejbConfig, annType, ejbName);
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
  }

  /**
   * Creates the old EJB 2.0 message-driven-destination
   */
  public MessageDrivenDestination createMessageDrivenDestination()
  {
    return new MessageDrivenDestination();
  }

  /**
   * Sets the JCA activation spec.
   */
  public void setActivationSpec(ActivationSpec activationSpec)
  {
    _activationSpec = activationSpec;
  }

  /**
   * Sets the JMS destination.
   */
  public void setDestination(Destination destination)
    throws ConfigException
  {
      _jmsActivationConfig.setDestinationObject(destination);
  }

  /**
   * Sets the JMS destination.
   */
  public void setDestinationValue(Destination destination)
  {
    _jmsActivationConfig.setDestinationObject(destination);
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
    return _jmsActivationConfig.getDestinationObject();
  }

  /**
   * @deprecated for compat with TCK
   */
  public void setMappedName(String mappedName)
    throws ConfigException
  {
    // XXX:
    // setDestination(destination);
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
      setContainerTransaction(true);
    }
    else if (type.equals("Bean")) {
      setContainerTransaction(false);
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

  private void addActivationConfigProperty(String name, Object value)
  {
    if ("destination".equals(name)) {
      if (value instanceof Destination) {
        setDestination((Destination) value);
      }
      else {
        Config.setAttribute(_jmsActivationConfig, "destinationName", value);
      }
    }
    else if ("messageSelector".equals(name)) {
      _messageSelector = (String) value;
    }
    /*
    else if ("message-consumer-max".equals(name)
             || "consumer-max".equals(name)) {
      setMessageConsumerMax(Integer.parseInt(String.valueOf(value)));
    }
    */
    else {
      Config.setAttribute(_jmsActivationConfig, name, value);
    }
  }

  /**
   * Sets the number of message consumers.
   */
  public void setMessageConsumerMax(int consumerMax)
    throws ConfigException
  {
    _jmsActivationConfig.setMaxPoolSize(consumerMax);
  }

  /**
   * Initialize
   */
  @PostConstruct
  @Override
  public void init()
    throws ConfigException
  {
    if (_messagingType != null) {
    }
    else if (_activationSpec != null) {
      String specName = _activationSpec.getClass().getName();

      ResourceArchive ra
        = ResourceArchiveManager.findResourceArchive(specName);

      if (ra == null) {
        throw new ConfigException(L.l("'{0}' is an unknown activation-spec.  Make sure the JCA adapter is deployed in a .rar file",
                                      specName));
      }

      try {
        _activationSpec.validate();
      } catch (Exception e) {
        throw error(e);
      }

      MessageListenerConfig listener = ra.getMessageListener(specName);

      _messagingType = listener.getMessageListenerType();
    }
    else if (MessageListener.class.isAssignableFrom(getEJBClass())) {
      _messagingType = MessageListener.class;
    }
    else
      throw error(L.l("'{0}' must implement javax.jms.MessageListener or specify {1}.",
                      getEJBClass().getName(),
                      isAllowPOJO() ? "messaging-type" : "messageListenerInterface"));

    super.init();

    ApiMethod ejbCreate
      = getEJBClassWrapper().getMethod("ejbCreate", new Class[0]);

    if (ejbCreate != null) {
      if (! ejbCreate.isPublic() && ! ejbCreate.isProtected())
        throw error(L.l("{0}: ejbCreate method must be public or protected.",
                        getEJBClass().getName()));
    }

    // J2EEManagedObject.register(new com.caucho.management.j2ee.MessageDrivenBean(this));
  }

  protected void introspect()
  {
    super.introspect();

    MessageDriven messageDriven
      = getEJBClassWrapper().getAnnotation(MessageDriven.class);

    if (messageDriven != null) {
      ActivationConfigProperty []activationConfig
        = messageDriven.activationConfig();

      if (activationConfig != null) {
        for (ActivationConfigProperty prop : activationConfig) {
          addActivationConfigProperty(prop.propertyName(),
                                      prop.propertyValue());

        }
      }

      Class<?> type = messageDriven.messageListenerInterface();
      if (type != null && ! Object.class.equals(type))
        _messagingType = type;
    }

    JmsMessageListener listener
      = getEJBClassWrapper().getAnnotation(JmsMessageListener.class);

    if (listener != null) {
      addActivationConfigProperty("destination", listener.destination());
      addActivationConfigProperty("consumer-max",
                                  String.valueOf(listener.consumerMax()));
    }
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

    if (! type.isAnnotationPresent(TransactionAttribute.class)) {
      type.addAnnotation(XaAnnotation.create(TransactionAttributeType.REQUIRED));
    }

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

  /**
   * Creates the bean generator for the session bean.
   */
  @Override
  protected BeanGenerator createBeanGenerator()
  {
    _messageBean = new MessageGenerator(getEJBName(), getEJBClassWrapper());

    _messageBean.setApi(new ApiClass(_messagingType));

    return _messageBean;
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
    if (_activationSpec != null)
      return deployActivationSpecServer(ejbManager, javaGen);
    else
      return deployJmsServer(ejbManager, javaGen);
  }

  private AbstractServer deployJmsServer(EjbContainer ejbManager,
                                         JavaClassGenerator javaGen)
    throws ClassNotFoundException
  {
    /*
    ConnectionFactory factory;
    Destination destination = null;

    if (_connectionFactory != null)
      factory = _connectionFactory;
    else
      factory = getEjbContainer().getJmsConnectionFactory();

    if (factory == null) {
      InjectManager webBeans = InjectManager.create();

      factory = webBeans.getReference(ConnectionFactory.class);
    }

    if (_destination != null)
      destination = _destination;
    else if (_messageDestinationLink != null) {
      MessageDestination dest;
      dest = getConfig().getMessageDestination(_messageDestinationLink);

      if (dest != null)
        destination = dest.getResolvedDestination();
    }

    if (destination == null)
      throw new ConfigException(L.l("ejb-message-bean '{0}' does not have a configured JMS destination or activation-spec",
                                    getEJBName()));

    if (factory == null)
      throw new ConfigException(L.l("ejb-message-bean '{0}' does not have a configured JMS connection factory",
                                    getEJBName()));
                                    */

    JmsResourceAdapter ra
      = new JmsResourceAdapter(getEJBName(), _jmsActivationConfig);

    JmsActivationSpec spec
      = new JmsActivationSpec();

    ra.setAcknowledgeMode(_acknowledgeMode);
    ra.setMessageSelector(_messageSelector);
    ra.setSubscriptionName(_subscriptionName);

    if (_jmsActivationConfig.getMaxPoolSize() > 0)
      ra.setConsumerMax(_jmsActivationConfig.getMaxPoolSize());
    else
      ra.setConsumerMax(getEjbContainer().getMessageConsumerMax());

    return deployMessageServer(ejbManager, javaGen, ra, spec);
  }

  /**
   * Deploys the bean.
   */
  public AbstractServer deployActivationSpecServer(EjbContainer ejbManager,
                                                   JavaClassGenerator javaGen)
    throws ClassNotFoundException
  {
    if (_activationSpec == null)
      throw new ConfigException(L.l("ActivationSpec is required for ActivationSpecServer"));

      String specType = _activationSpec.getClass().getName();

      ResourceArchive raCfg = ResourceArchiveManager.findResourceArchive(specType);

      if (raCfg == null)
        throw error(L.l("'{0}' is an unknown activation-spec.  Make sure the .rar file for the driver is properly installed.",
                        specType));

      Class<?> raClass = raCfg.getResourceAdapterClass();

      if (raClass == null)
        throw error(L.l("resource-adapter class does not exist for activation-spec '{0}'.  Make sure the .rar file for the driver is properly installed.",
                        raClass.getName()));

      InjectManager webBeans = InjectManager.create();

      ResourceAdapter ra
        = (ResourceAdapter) webBeans.getReference(raClass);

      if (ra == null) {
        throw error(L.l("resource-adapter '{0}' must be configured in a <connector> tag.",
                        raClass.getName()));
      }

    return deployMessageServer(ejbManager, javaGen, ra, _activationSpec);
  }

  /**
   * Deploys the bean.
   */
  public AbstractServer deployMessageServer(EjbContainer ejbManager,
                                            JavaClassGenerator javaGen,
                                            ResourceAdapter ra,
                                            ActivationSpec spec)
    throws ClassNotFoundException
  {
    MessageServer server;

    try {
      if (spec == null)
        throw new ConfigException(L.l("ActivationSpec is required for MessageServer"));

      if (ra == null)
        throw new ConfigException(L.l("ResourceAdapter is required for ActivationSpecServer"));


      server = new MessageServer(ejbManager, getAnnotatedType());

      server.setConfigLocation(getFilename(), getLine());

      server.setModuleName(getEJBModuleName());
      server.setEJBName(getEJBName());
      server.setMappedName(getMappedName());
      server.setId(getEJBModuleName() + "#" + getMappedName());

      server.setContainerTransaction(isContainerTransaction());

      server.setEjbClass(getEJBClass());

      Class contextImplClass = javaGen.loadClass(getSkeletonName());

      server.setContextImplClass(contextImplClass);

      server.setActivationSpec(spec);
      server.setResourceAdapter(ra);

      // server.setMessageListenerType(_messagingType);

      javaGen.loadClass(getEJBClass().getName());

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(server.getClassLoader());

        ContainerProgram initContainer = getInitProgram();

        EjbProducer<?> producer = server.getProducer();
        
        producer.setInitProgram(initContainer);

        if (getServerProgram() != null)
          getServerProgram().configure(server);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    } catch (Exception e) {
      throw error(e);
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
      setDestination((Destination) destination.getObject());
    }
  }
}
