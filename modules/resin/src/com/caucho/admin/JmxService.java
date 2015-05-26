/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin;

import io.baratine.core.Startup;

import javax.enterprise.context.ApplicationScoped;

import com.caucho.cloud.jmx.JmxServiceImpl;

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
