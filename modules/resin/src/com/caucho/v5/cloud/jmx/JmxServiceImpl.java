/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.cloud.jmx;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.jmx.JmxUtilResin;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.util.L10N;

/**
 * Remote administration service for JMX
 */
public class JmxServiceImpl
{
  private static final L10N L = new L10N(JmxServiceImpl.class);

  private JmxActor _actor;
  private MBeanServer _mbeanServer;

  private Lifecycle _lifecycle = new Lifecycle();

  protected JmxServiceImpl()
  {
    AmpSystem ampService = AmpSystem.getCurrent();
    
    if (ampService == null) {
      throw new ConfigException(L.l("JmxService requires an active {0}",
                                    AmpSystem.class.getSimpleName()));
    }
  }

  /**
   * Start the JMXService
   */
  @PostConstruct
  public void init()
  {
    if (! _lifecycle.toActive()) {
      return;
    }

    _mbeanServer = JmxUtilResin.getMBeanServer();
    
    _actor = new JmxActor(_mbeanServer);
    
    String address = "/jmx";

    ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
    
    rampManager.service(_actor).address("public:///jmx").ref();
    // log.info(L.l("JMX management service '{0}' started", SERVICE_NAME));
  }
}
