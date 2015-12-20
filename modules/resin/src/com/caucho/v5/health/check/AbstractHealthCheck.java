/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.v5.health.check;

import io.baratine.core.Startup;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthCheckResult;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

@Startup
public abstract class AbstractHealthCheck implements HealthCheck
{
  private static final L10N L = new L10N(AbstractHealthCheck.class);
  
  private AbstractHealthCheck _delegate;
  
  private String _name;
  private boolean _isEnabled = true;
  
  private long _logPeriod = 0;
  private long _nextLog = 0;
  
  private boolean _initialized = false;
  
  public AbstractHealthCheck()
  {
  }
  
  /**
   * Finds and returns any delegate health check. Used to configure 
   * singleton system health checks.
   */
  protected AbstractHealthCheck findDelegate(HealthSubSystem healthService)
  {
    return null;
  }
  
  @Configurable
  public void setEnabled(boolean isEnabled)
  {
    _isEnabled = isEnabled;
  }

  @Override
  public boolean isEnabled()
  {
    return _isEnabled;
  }
  
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }
  
  @Override
  public String getName()
  {
    if (_name == null) {
      initName();
    }
      
    return _name;
  }
  
  @PostConstruct
  public void init()
  {
    if (_initialized) {
      return;
    }
    
    _initialized = true;
    
    HealthSubSystem healthService = HealthSubSystem.getCurrent();
    
    if (healthService == null) {
      throw new ConfigException(L.l("{0} requires an active {1}",
                                    getClass().getSimpleName(),
                                    HealthSubSystem.class.getSimpleName()));
    }

    AbstractHealthCheck delegate = findDelegate(healthService);
    
    if (delegate == null) {
      healthService.addHealthCheck(this);
    }
    else if (delegate == this) {
      delegate = null;
    }
    else {
      _delegate = delegate;
    }
  }
  
  private void initName()
  {
    String name = _name;
    
    if (name == null) {
      name = getBeanName();
    }
    
    if (name == null) {
      name = getClass().getSimpleName();
    }
    
    _name = name;
  }
  
  private String getBeanName()
  {
    /*
    if (_beanManager == null)
      return null;
    
    String name = null;
    
    Set<Bean<?>> beans = _beanManager.getBeans(this.getClass());
    if (beans.size() == 1) {
      Bean<?> myBean = beans.iterator().next();
      name = myBean.getName();
    } else if (beans.size() > 1) {
      Context context = _beanManager.getContext(Singleton.class);
      for(Bean<?> bean : beans) {
        AbstractHealthCheck healthCheck = 
          (AbstractHealthCheck) context.get(bean);
        if (healthCheck != null && healthCheck == this) {
          name = bean.getName();
        }
      }
    }
      
    if (name != null &&
        name.length() > 0 &&
        ! name.equalsIgnoreCase(this.getClass().getSimpleName())) {
      return name;
    }
    */
    
    return null;
  }
  
  @Override
  public void start()
  {
  }
  
  @Override
  public void stop()
  {
  }
  
  protected AbstractHealthCheck getDelegate()
  {
    return _delegate;
  }
  
  protected boolean hasDelegate()
  {
    return _delegate != null;
  }
  
  @Override
  public String toString()
  {
    if (hasDelegate()) {
      return getDelegate().toString();
    }
    
    return getClass().getSimpleName() + "[]";
  }
  
  @Override
  @Configurable
  public void silenceFor(Period period)
  {
    _nextLog = CurrentTime.getCurrentTime() + period.getPeriod();
  }

  @Override
  @Configurable
  public void setLogPeriod(Period period)
  {
    _logPeriod = period.getPeriod();
  }
  
  public long getLogPeriod()
  {
    return _logPeriod;
  }

  @Override
  public void logResult(HealthCheckResult result, Logger log)
  {
    Level level = Level.FINER;
    switch (result.getStatus()) {
      case WARNING:
        level = Level.INFO;
        break;
      case CRITICAL:
        level = Level.WARNING;
        break;
      case FATAL:
        level = Level.SEVERE;
        break;
      case UNKNOWN:
        level = Level.FINE;
        break;
      default:
        level = Level.FINER;
    }
    
    if (! log.isLoggable(level)) {
      return;
    }
    
    long time = CurrentTime.getCurrentTime();
    if (_nextLog > time) {
      return;
    }
    
    _nextLog = time + _logPeriod;
    
    log.log(level, L.l("{0}[{1}]", getName(), result.getDescription()));
  }
  
  @Override
  public HealthCheckResult getLastResult(HealthSubSystem healthService)
  {
    if (hasDelegate())
      return healthService.getLastResult(getDelegate());
    else
      return healthService.getLastResult(this);
  }
}
