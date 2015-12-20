/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.check;

import io.baratine.core.Startup;

import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.health.MemoryTenuredHealthCheckImpl;

/**
 * Monitors the amount of free memory in the Tenured pool.  Requests a 
 * garbage collection if memory falls too low.
 * <p>
 * Note: This check will query heap memory for JVMs with no Tenured pool.
 * <p>
 * Generates UNKNOWN if memory pool attributes are not available in JMX.
 * <p>
 * Generates WARNING if free memory is below the warning percentage after 
 * a GC (default 1%)
 * <p>
 * Generates CRITICAL if free memory is below the the minimum after a GC 
 * (default 1 meg).  This can also be configured by setting 
 * &lt;memory-free-min &gt;. 
 *
 */
@Startup
@Singleton
@Configurable
@Named
public class MemoryTenuredHealthCheck 
  extends MemoryTenuredHealthCheckImpl
{
  public MemoryTenuredHealthCheck()
  {
  }
  
  /**
   * Set minimum amount of free memory (default 1m)
   */
  @Configurable
  public void setMemoryFreeMin(Bytes memoryMin)
  {
    MemoryTenuredHealthCheck delegate = getDelegate();
    
    if (delegate != null)
      delegate.setMemoryFreeMinImpl(memoryMin.getBytes());
    else
      super.setMemoryFreeMinImpl(memoryMin.getBytes());
  }

  /**
   * Set the warning threshold percentage (default 0.01)
   */
  @Configurable
  public void setFreeWarning(double pFree)
  {
    MemoryTenuredHealthCheck delegate = getDelegate();
    
    if (delegate != null)
      delegate.setFreeWarningImpl(pFree);
    else
      super.setFreeWarningImpl(pFree);
  }
  
  /**
   * Explicitly set the MBean name to query for memory stats
   */
  @Configurable
  public void setObjectName(String objectName)
  {
    MemoryTenuredHealthCheck delegate = getDelegate();
    
    if (delegate != null)
      delegate.setObjectNameImpl(objectName);
    else
      super.setObjectNameImpl(objectName);
  }
  
  @Override
  protected AbstractHealthCheck findDelegate(HealthSubSystem healthService)
  {
    return healthService.getHealthCheck(getClass());
  }
 
  @Override
  protected MemoryTenuredHealthCheck getDelegate()
  {
    return (MemoryTenuredHealthCheck) super.getDelegate();
  }
}
