/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.log;

import java.util.logging.*;

import com.caucho.v5.health.meter.CountSensor;
import com.caucho.v5.health.meter.MeterService;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;

/**
 * Log handler
 */
public class LogHandler
  extends Handler
{
  private static final CountSensor _severeSensor
    = MeterService.createCountMeter("Resin|Log|Severe");
  private static final CountSensor _warningSensor
    = MeterService.createCountMeter("Resin|Log|Warning");
  private static final CountSensor _infoSensor
    = MeterService.createCountMeter("Resin|Log|Info");
  
  private String _type;

  private LogSystem _logSystem;
  
  public LogHandler()
  {
    this(LogSystem.getCurrent());
  }
  
  public LogHandler(LogSystem logSystem)
  {
    _logSystem = logSystem;
    
    if (_logSystem != null)
      _type = _logSystem.createFullType("Resin|Log");
  }
  
  @Override
  public void publish(LogRecord record)
  {
    if (_logSystem == null)
      return;

    if (! isLoggable(record))
      return;

    try {
      String name = record.getLoggerName();
      String message = record.getMessage();
    
      long timestamp;

      if (CurrentTime.isTest())
        timestamp = CurrentTime.getCurrentTime();
      else
        timestamp = record.getMillis();
      
      // String threadName = Thread.currentThread().getName();
    
      Level level = record.getLevel();
      
      if (Level.SEVERE.intValue() <= level.intValue()) {
        _severeSensor.start();
      }
      else if (Level.WARNING.intValue() <= level.intValue()) {
        _warningSensor.start();
      }
      else if (Level.INFO.intValue() <= level.intValue()) {
        _infoSensor.start();
      }
      
      if (name == null) {
        // 5499
        name = "";
      }
      
      _logSystem.log(timestamp, _type, name, level, message);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void flush()
  {
  }

  public void close()
  {
  }
}
