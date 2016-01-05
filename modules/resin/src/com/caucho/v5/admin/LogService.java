/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.admin;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.util.L10N;

@Startup
@Singleton
@Configurable
public class LogService
{
  private static final L10N L = new L10N(LogService.class);
  
  private LogSystem _logSystem;
  
  public LogService()
  {
    _logSystem = LogSystem.getCurrent();
  }
  
  @PostConstruct
  public void init()
  {
    _logSystem.init();
  }
  
  /**
   * Deprecated, has no effect.  For BC only.
   */
  @Deprecated
  @Configurable
  public void setEnable(boolean isEnable)
  {
    
  }

  /**
   * Deprecated, has no effect.  For BC only.
   */
  @Deprecated
  @Configurable
  public void setPath(String path)
  {
    
  }

  /**
   * Sets the level of records to capture, default is "info".
   */
  @Configurable
  public void setLevel(String level)
    throws ConfigException
  {
    try {
      _logSystem.setLevel(Level.parse(level.toUpperCase()));
    } catch (Exception e) {
      throw new ConfigException(L.l("'{0}' is an unknown log level.  Log levels are:\noff - disable logging\nsevere - severe errors only\nwarning - warnings\ninfo - information\nconfig - configuration\nfine - fine debugging\nfiner - finer debugging\nfinest - finest debugging\nall - all debugging",
                                    level));
    }
  }
  
  /**
   * Sets the length of time the log entries will be saved before being
   * removed. 
   */
  @Configurable
  public void setExpireTimeout(Period period)
  {
    _logSystem.setExpireTimeout(period.getPeriod());
  }
}
