/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import com.caucho.v5.amp.thread.WorkerThreadPoolBase;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.check.AbstractHealthCheck;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

public class HealthServiceHealthCheckImpl extends AbstractHealthCheck
{
  private static final L10N L = new L10N(HealthServiceHealthCheckImpl.class);

  private long _threadCheckPeriod = 60 * 1000;
  private long _freezeTimeout = 15 * 60 * 1000;
  
  private HealthSubSystem _healthService;
  
  private HealthSystemHealthCheckWorker _worker;
  
  private HealthCheckResult _currentResult
    = new HealthCheckResult(HealthStatus.OK);
  
  public HealthServiceHealthCheckImpl() 
  {
    _healthService = SystemManager.getCurrentSystem(HealthSubSystem.class);
    if (_healthService == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          this.getClass().getSimpleName(),
                                          HealthSubSystem.class.getSimpleName()));
    }
    _worker = new HealthSystemHealthCheckWorker();
  }

  @Override
  public HealthCheckResult checkHealth()
  {
    _worker.wake();
    
    return _currentResult;
  }
  
  @Override
  public void start()
  {
    _worker.wake();
  }
  
  @Override
  public void stop()
  {
    _worker.close();
  }
  
  public long getFreezeTimeout()
  {
    return _freezeTimeout;
  }

  public void setFreezeTimeoutImpl(long freezeTimeout)
  {
    _freezeTimeout = freezeTimeout;
  }
  
  public long getThreadCheckPeriod()
  {
    return _threadCheckPeriod;
  }

  public void setThreadCheckPeriodImpl(long threadCheckPeriod)
  {
    _threadCheckPeriod = threadCheckPeriod;
  }

  // this runs in a separate thread to 
  protected class HealthSystemHealthCheckWorker extends WorkerThreadPoolBase
  {
    protected HealthSystemHealthCheckWorker()
    {
      wake();
    }
    
    @Override
    public boolean isPermanent()
    {
      // return isEnabled() && ! Alarm.isTest() && _healthService.getLifecycleState().isActive();
      
      return isEnabled() && ! CurrentTime.isTest();
    }
    
    @Override
    public long runTask()
    {
      Thread thread = Thread.currentThread();

      thread.setName(getClass().getSimpleName());
      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());

      if (! isEnabled() || CurrentTime.isTest()) {
        return _threadCheckPeriod;
      }
      
      if (! _healthService.isEnabled()
          || ! _healthService.getLifecycleState().isActive()) {
        return _threadCheckPeriod;
      }
      
      long now = CurrentTime.getCurrentTime();
      
      // TODO: is maxValidInterval relevant?
      long maxValidInterval = _healthService.getPeriod() + 
                              (_healthService.getRecheckMax() * _healthService.getRecheckPeriod());
//      long maxValidInterval = _healthService.getPeriod() + getFreezeTimeout();
      
      HealthCheckResult result = null;

      if (_healthService.getLastCheckStartTime() + maxValidInterval < now) {
        result = new HealthCheckResult(HealthStatus.FATAL, 
                          "Health checks did not complete in time");
      } 
      else if (_healthService.getLastCheckFinishTime() + maxValidInterval < now) {
        result = new HealthCheckResult(HealthStatus.FATAL, 
            "Health retry checks did not complete in time");
      }
      
      if (result != null) {
        _currentResult = result;
      }
      
      return _threadCheckPeriod;
    }
  }
}
