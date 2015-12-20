/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.check;

import java.util.ArrayList;

import com.caucho.v5.env.health.HealthCheckResult;
import com.caucho.v5.env.health.HealthStatus;
import com.caucho.v5.env.meter.MeterBase;
import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

public class CpuHealthCheckImpl extends AbstractHealthCheck
{
  private static final L10N L = new L10N(CpuHealthCheckImpl.class);
  
  private int _warningThreshold = 95;
  private int _criticalThreshold = 200;

  private MeterBase []_jniCpuMeters;

  public CpuHealthCheckImpl()
  {
    
  } 

  @Override
  public HealthCheckResult checkHealth()
  {
    if (_jniCpuMeters == null) {
      StatSystem statSystem = StatSystem.getCurrent();
      if (statSystem == null) {
        return new HealthCheckResult(HealthStatus.UNKNOWN, 
                                     L.l("Stats not available"));
      }

      ArrayList<MeterBase> jniCpuMeters = statSystem.getCpuMeters();
      _jniCpuMeters = new MeterBase[jniCpuMeters.size()];
      jniCpuMeters.toArray(_jniCpuMeters);
    }
    
    HealthStatus status = HealthStatus.OK;
    StringBuilder msg = new StringBuilder();
    
    for (int i = 0; i < _jniCpuMeters.length; i++) {
      double cpuLoad = 100 * _jniCpuMeters[i].peek();
      
      if (CurrentTime.isTest())
        continue;
      
      if (cpuLoad < 0 || Double.isNaN(cpuLoad))
        continue;
      
      String cpu = String.format("cpu%d=%.0f%%", i, cpuLoad);
      msg.append(' ').append(cpu);
      
      if (cpuLoad > _criticalThreshold)
        status = HealthStatus.CRITICAL;
      else if (cpuLoad > _warningThreshold)
        status = HealthStatus.WARNING;
    }
    
    return new HealthCheckResult(status, msg.toString());
  }
  
  public void setCpuMeters(ArrayList<MeterBase> cpuMeters)
  {
    _jniCpuMeters = new MeterBase[cpuMeters.size()];
    cpuMeters.toArray(_jniCpuMeters);
  }
  
  public int getWarningThreshold()
  {
    return _warningThreshold;
  }

  public void setWarningThreshold(int warningThreshold)
  {
    _warningThreshold = warningThreshold;
  }

  public int getCriticalThreshold()
  {
    return _criticalThreshold;
  }

  public void setCriticalThreshold(int criticalThreshold)
  {
    _criticalThreshold = criticalThreshold;
  }  
}
