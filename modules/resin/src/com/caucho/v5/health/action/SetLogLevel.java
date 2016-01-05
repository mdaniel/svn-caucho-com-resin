/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Health action to change a log level, optionally temporarily. 
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:SetLogLevel>
 *   <level>FINEST</level>
 *   <logger>com.caucho</logger>
 *   <logger>com.foo</logger>
 *   <time>60s</time>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 * </health:SetLogLevel>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class SetLogLevel extends HealthActionBase
{
  private static final L10N L = new L10N(SetLogLevel.class);
  private static final Logger log
    = Logger.getLogger(SetLogLevel.class.getName());
  
  private static final String[] _defaultLoggers = {"", "com.caucho"};
  
  private List<String> _loggersList = new ArrayList<String>();
  private Level _level;
  private long _time = 0;
  
  private String[] _loggers;
  
  public List<String> getLoggers()
  {
    return _loggersList;
  }

  @Configurable
  public void setLoggers(List<String> loggers)
  {
    _loggersList = loggers;
  }
  
  @Configurable
  public void addLogger(String logger)
  {
    _loggersList.add(logger);
  }

  public Level getLevel()
  {
    return _level;
  }
  
  @Configurable
  public void setLevel(Level level)
  {
    _level = level;
  }
  
  @Configurable
  public void setLevel(String level)
  {
    _level = Level.parse(level.trim().toUpperCase());
  }

  public long getTime()
  {
    return _time;
  }
  
  @Configurable
  public void setTime(Period time)
  {
    setTimeMillis(time.getPeriod());
  }

  @Configurable
  public void setTimeMillis(long time)
  {
    _time = time;
  }
  
  @PostConstruct
  public void init()
  {
    if (_level == null) 
      throw new ConfigException(L.l("'level' is a required attribute of <health:{0}>",
                                    this.getClass().getSimpleName()));
    
    if (_loggersList.isEmpty()) {
      _loggers = _defaultLoggers;
    } else {
      _loggers = new String[_loggersList.size()];
      _loggers = _loggersList.toArray(_loggers);
    }
    
    super.init();
  }

  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    String result = execute(_loggers, _level, _time);
    
    return new HealthActionResult(ResultStatus.OK, result);
  }
  
  private static ClassLoader _systemClassLoader = 
    ClassLoader.getSystemClassLoader();

  public String execute(final String[] loggerNames, 
                        final Level newLevel, 
                        final long time)
  {
    if (time > 0) {
      final Map<String, Level> oldLevels = getLoggerLevels(loggerNames);
  
      AlarmListener listener = new AlarmListener()
      {
        @Override
        public void handleAlarm(Alarm alarm)
        {
          setLoggerLevels(oldLevels);
        }
      };
  
      new Alarm("log-level", listener, time);
    }

    setLoggerLevels(loggerNames, newLevel);
    
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i<loggerNames.length; i++) {
      if (loggerNames[i].length() == 0)
        sb.append("{root}");
      else
        sb.append(loggerNames[i]);
      
      if (i < (loggerNames.length-1))
        sb.append(", ");
    }

    if (time > 0) {
      return L.l("Logger '{2}' level is set to '{0}', active time {1} seconds",
                 newLevel,
                 (time / 1000),
                 sb.toString());
    } else {
      return L.l("Logger '{1}' level is set to '{0}'",
                 newLevel,
                 sb.toString());
    }
  }
  
  private static void setLoggerLevels(final String[] loggerNames, final Level level)
  {
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      if (! CurrentTime.isTest()) {
        thread.setContextClassLoader(_systemClassLoader);
      }
      
      for (String loggerName : loggerNames) {
        Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(level);
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
  
  private static void setLoggerLevels(final Map<String, Level> levelsMap)
  {
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      if (! CurrentTime.isTest()) {
        thread.setContextClassLoader(_systemClassLoader);
      }
      
      for (Map.Entry<String, Level> entry : levelsMap.entrySet()) {
        Logger logger = Logger.getLogger(entry.getKey());
        logger.setLevel(entry.getValue());
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
  

  private static Map<String, Level> getLoggerLevels(String[] loggerNames)
  {
    Map<String, Level> oldLevels = new HashMap<String, Level>();
    
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_systemClassLoader);
      
      for (String loggerName : loggerNames) {
        Logger logger = Logger.getLogger(loggerName);
        Level level = logger.getLevel();
        oldLevels.put(loggerName, level);
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
    
    return oldLevels;
  }
}
