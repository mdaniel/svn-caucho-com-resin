/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.admin;

import io.baratine.service.Startup;

import javax.enterprise.context.ApplicationScoped;

import com.caucho.v5.cloud.jmx.JmxServiceImpl;

/**
 * Remote administration service for JMX
 */
@Startup
@ApplicationScoped
public class JmxService extends JmxServiceImpl
{
  public JmxService()
  {
  }
}
