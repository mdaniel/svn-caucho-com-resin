/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import java.util.logging.*;

import javax.management.JMException;

import com.caucho.v5.health.check.AbstractHealthCheck;
import com.caucho.v5.util.*;
import com.caucho.v5.util.MemoryPoolAdapter.MemUsage;

public abstract class AbstractMemoryHealthCheckImpl extends AbstractHealthCheck
{
  private static final Logger log
    = Logger.getLogger(AbstractMemoryHealthCheckImpl.class.getName());
  private static final L10N L = new L10N(AbstractMemoryHealthCheckImpl.class);
  
  private MemoryPoolAdapter _memoryPoolAdapter;

  private long _freeMin = 1024 * 1024;
  private double _pFreeWarning = 0.01;
  
  public AbstractMemoryHealthCheckImpl()
  {
    _memoryPoolAdapter = new MemoryPoolAdapter();
  }

  public void setMemoryFreeMinImpl(long min)
  {
    _freeMin = min;
  }
  
  public long getMemoryFreeMin()
  {
    return _freeMin;
  }
  
  public void setFreeWarningImpl(double pFree)
  {
    _pFreeWarning = pFree;
  }
  
  protected MemoryPoolAdapter getMemoryPool()
  {
    return _memoryPoolAdapter;
  }
  
  @Override
  public HealthCheckResult checkHealth()
  {
    try {
      return checkFreeMemory(getClass().getSimpleName());
    } catch (JMException e) {
      return new HealthCheckResult(HealthStatus.UNKNOWN, 
                                   L.l("{0} failed to gather memory usage: {1}", 
                                       getName(), e));
    }
  }
 
  /**
   * Checks the free memory available.
   * 
   * Return CRITICAL if memory is less than freeMin after a gc
   * otherwise return WARNING
   */ 
  private HealthCheckResult checkFreeMemory(String description)
    throws JMException
  {
    MemUsage usagePreGc = getMemoryUsage();
    
    if (usagePreGc == null) {
      return new HealthCheckResult(HealthStatus.UNKNOWN, 
                                   L.l("{0}: cannot gather memory usage",
                                       getName()));
    }

    HealthCheckResult resultPreGc = checkUsage(usagePreGc);

    if (resultPreGc.getStatus().compareTo(HealthStatus.CRITICAL) < 0)
      return resultPreGc;
    
    requestGc(description, usagePreGc);
    
    MemUsage usagePostGc = getMemoryUsage();
    if (usagePostGc == null) {
      return new HealthCheckResult(HealthStatus.UNKNOWN, 
                                   L.l("{0}: cannot gather memory usage after gc",
                                       getName()));
    }

    HealthCheckResult resultPostGc = checkUsage(usagePostGc);
    
    return resultPostGc;
  }
  
  private HealthCheckResult checkUsage(MemUsage usage)
  {
    long max = usage.getMax();
    long used = usage.getUsed();
    long free = usage.getFree();
    
    String msg = String.format("%.2f%% free (%.3fM free, %.3fM max, %.3fM used)",
                               (100.0 * free / max),
                               free * 1.0e-6,
                               max * 1.0e-6,
                               used * 1.0e-6);
    
    if (_freeMin <= 0 || _freeMin < free) {
      if (max * _pFreeWarning < free)
        return new HealthCheckResult(HealthStatus.OK, msg);
      else
        return new HealthCheckResult(HealthStatus.WARNING, msg);
    } else {
      return new HealthCheckResult(HealthStatus.CRITICAL, msg);
    }
  }
  
  private void requestGc(String description, MemUsage usage)
  {
    long free = usage.getFree();
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(L.l("{0}: free {1} memory max:{2} used:{3} free:{4}",
                    getName(),
                    description,
                    usage.getMax(),
                    usage.getUsed(),
                    free));
    }
    
    log.info(L.l("{0}: Forcing GC due to low {1} memory. {2} free bytes, {3} max {4} used.",
                 getName(),
                 description, 
                 free,
                 usage.getMax(),
                 usage.getUsed()));

    System.gc();
  }
  
  protected abstract MemUsage getMemoryUsage() throws JMException;
}
