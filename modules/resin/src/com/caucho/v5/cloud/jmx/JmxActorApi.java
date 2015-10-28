/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.cloud.jmx;

import java.util.HashMap;

import javax.management.MBeanInfo;

/**
 * Remote administration service for JMX
 */
public interface JmxActorApi
{
  public MBeanInfo getMBeanInfo(String name);

  public HashMap<String,Object> lookup(String name);

  public String []query(String name);

  public Object invoke(String name, String opName,
                       Object []args, String []sig);
}
