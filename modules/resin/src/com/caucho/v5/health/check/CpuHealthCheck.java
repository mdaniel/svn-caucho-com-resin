/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.v5.health.check;

import io.baratine.service.Startup;

import java.util.ArrayList;

import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.health.meter.MeterBase;

/**
 * Monitors CPU usage.
 * <p>
 * Generates WARNING if any CPU exceeds the warning threshold (default 95%)
 * <p>
 * Generates CRITICAL if any CPU exceeds the failure threshold (default disabled)
 * 
 */
@Startup
@Singleton
@Configurable
@Named
public class CpuHealthCheck extends CpuHealthCheckImpl
{
  @Override
  public void setCpuMeters(ArrayList<MeterBase> cpuMeters)
  {
    CpuHealthCheck delegate = getDelegate();
    
    if (delegate != null)
      delegate.setCpuMeters(cpuMeters);
    else
      super.setCpuMeters(cpuMeters);
  }

  /**
   * Set CPU usage warning threshold (0-100, default 95)
   */
  @Configurable
  @Override
  public void setWarningThreshold(int warningThreshold)
  {
    CpuHealthCheck delegate = getDelegate();
    
    if (delegate != null)
      delegate.setWarningThreshold(warningThreshold);
    else
      super.setWarningThreshold(warningThreshold);
  }

  /**
   * Set CPU usage critical threshold (0-100, default 200 (disabled))
   */
  @Configurable
  @Override
  public void setCriticalThreshold(int criticalThreshold)
  {
    CpuHealthCheck delegate = getDelegate();
    
    if (delegate != null)
      delegate.setCriticalThreshold(criticalThreshold);
    else
      super.setCriticalThreshold(criticalThreshold);
  }
  
  @Override
  protected AbstractHealthCheck findDelegate(HealthSubSystem healthService)
  {
    return healthService.getHealthCheck(getClass());
  }
  
  @Override
  protected CpuHealthCheck getDelegate()
  {
    return (CpuHealthCheck) super.getDelegate();
  }
}
