/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.server.container.ServerContainerConfig;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.MemoryPoolAdapter.MemUsage;

public class MemoryTenuredHealthCheckImpl extends AbstractMemoryHealthCheckImpl
{
  private static final Logger log
    = Logger.getLogger(MemoryTenuredHealthCheckImpl.class.getName());
  private static final L10N L = new L10N(MemoryTenuredHealthCheckImpl.class);

  public MemoryTenuredHealthCheckImpl()
  {
    // default to resin memory-free-min for backwards compatability
    ServerContainerConfig config = null;
    
    try {
      // needs to be reverse config where builder sets health
      // XXX: config = Resin.getCurrent().getServletContainerConfig();
    } catch (Exception e) {
      log.finer(L.l("{0} failed to get {1}",
                    getName(), 
                    ServerContainerConfig.class.getSimpleName()));
    }
    
    if (config != null) {
      long memoryFreeMin = config.getMemoryFreeMin();
      log.finer(L.l("{0} defaulting memoryFreeMin to {1} using <memory-free-min>",
                    getName(), 
                    memoryFreeMin));
      setMemoryFreeMinImpl(memoryFreeMin);
    }
  }
  
  @Override
  protected MemUsage getMemoryUsage()
    throws JMException
  {
    return getMemoryPool().getTenuredMemUsage();
  }
  
  public void setObjectNameImpl(String objectName)
  {
    try {
      getMemoryPool().setTenuredName(new ObjectName(objectName));
    } catch (MalformedObjectNameException e) {
      throw ConfigException.wrap(e);
    }
  }
}
