/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.cloud.jmx;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;

import com.caucho.amp.AmpSystem;
import com.caucho.amp.ServiceManagerAmp;
import com.caucho.config.ConfigException;
import com.caucho.jmx.JmxUtil;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.L10N;

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

    _mbeanServer = JmxUtil.getMBeanServer();
    
    _actor = new JmxActor(_mbeanServer);
    
    String address = "/jmx";

    ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
    
    rampManager.service(_actor).bind("public:///jmx");
    // log.info(L.l("JMX management service '{0}' started", SERVICE_NAME));
  }
}
