/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.action.HealthActionBase;
import com.caucho.v5.health.action.JmxSetQueryReply;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.L10N;

/**
 * Health action to set a JMX MBean attribute value.
 * <p>
 * <pre>{@code
 * <health:SetJmxAttribute>
 *   <objectName>java.lang:type=ClassLoading</objectName>
 *   <attribute>Verbose</attribute>
 *   <value>true</value>
 *   <health:OnStart/>
 * </health:SetJmxAttribute> 
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class SetJmxAttribute extends HealthActionBase
{
  private static final L10N L = new L10N(SetJmxAttribute.class);
  private static final Logger log
    = Logger.getLogger(SetJmxAttribute.class.getName());
  
  private String _objectName;
  private String _attribute;
  private String _value;
  
  @PostConstruct
  public void init()
  {
    if (_objectName == null)
      throw new ConfigException(L.l("<health:{0}> requires 'object-name' attribute",
                                    getClass().getSimpleName()));
    
    if (_attribute == null)
      throw new ConfigException(L.l("<health:{0}> requires 'attribute' attribute",
                                    getClass().getSimpleName()));
    
    if (_value == null)
      throw new ConfigException(L.l("<health:{0}> requires 'value' attribute",
                                    getClass().getSimpleName()));

    super.init();
  }

  public String getObjectName()
  {
    return _objectName;
  }

  @Configurable
  public void setObjectName(String objectName)
  {
    _objectName = objectName;
  }

  public String getAttribute()
  {
    return _attribute;
  }

  @Configurable
  public void setValue(String value)
  {
    _value = value;
  }
  
  public String getValue()
  {
    return _value;
  }

  @Configurable
  public void setAttribute(String attribute)
  {
    _attribute = attribute;
  }

  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    JmxSetQueryReply reply = execute(_objectName,
                                                         _attribute,
                                                         _value);

    String message = L.l(
      "value for attribute `{0}' on bean `{1}' is changed from `{2}' to `{3}'",
      reply.getAttribute(),
      reply.getBean(),
      reply.getOldValue(),
      reply.getNewValue());
    
    return new HealthActionResult(ResultStatus.OK, message);
  }

  public JmxSetQueryReply execute(String pattern,
                                  String attributeName,
                                  String value)
    throws ConfigException, JMException, ClassNotFoundException
  {
    final List<MBeanServer> servers = new LinkedList<MBeanServer>();
  
    servers.addAll(MBeanServerFactory.findMBeanServer(null));

    ObjectName nameQuery = ObjectName.getInstance(pattern);
    
    ObjectName serverNameQuery = null;
    
    try {
      String serverId = SystemManager.getCurrentId();
      
      if (pattern.indexOf("*") < 0) {
        serverNameQuery = ObjectName.getInstance(pattern + ",Server=" + serverId);
      }
    } catch (Exception e) {
    }
    
    ObjectName subjectBean = null;
    MBeanServer subjectBeanServer = null;

    for (final MBeanServer server : servers) {
      for (final ObjectName mbean : server.queryNames(nameQuery, null)) {
        if (subjectBean != null) {
          throw new ConfigException(L.l("multiple beans match `{0}'", pattern));
        }

        subjectBean = mbean;
        subjectBeanServer = server;
      }
    }
    
    if (subjectBean == null) {
      for (final MBeanServer server : servers) {
        for (final ObjectName mbean : server.queryNames(serverNameQuery, null)) {
          if (subjectBean != null) {
            throw new ConfigException(L.l("multiple beans match `{0}'", pattern));
          }

          subjectBean = mbean;
          subjectBeanServer = server;
        }
      }
    }

    MBeanAttributeInfo attributeInfo = null;
    if (subjectBean != null) {
      for (MBeanAttributeInfo info : subjectBeanServer.getMBeanInfo(
        subjectBean).getAttributes()) {
        if (info.getName().equals(attributeName)) {
          attributeInfo = info;
          break;
        }
      }
    }

    if (subjectBean == null) {
      throw new ConfigException(L.l("no beans match `{0}'", pattern));
    }
    else if (attributeInfo == null) {
      throw new ConfigException(L.l("bean at `{0}' does not appear to have attribute `{1}'",
                                    pattern,
                                    attributeName));
    }
    else {
      Object oldValue = subjectBeanServer.getAttribute(subjectBean,
                                                       attributeInfo.getName());

      final Object attribValue = null;//XXX:toValue(attributeInfo.getType(), value);
      final Attribute attribute = new Attribute(attributeName, attribValue);

      subjectBeanServer.setAttribute(subjectBean, attribute);

      JmxSetQueryReply reply
        = new JmxSetQueryReply(subjectBean.getCanonicalName(),
                               attributeName,
                               String.valueOf(oldValue),
                               String.valueOf(attribValue));
      
      return reply;
    }
  }
}
