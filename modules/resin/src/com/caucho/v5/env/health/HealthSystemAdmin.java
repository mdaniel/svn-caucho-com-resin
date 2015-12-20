/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.management.server.HealthEventLog;
import com.caucho.v5.management.server.HealthSystemMXBean;
import com.caucho.v5.management.server.LogMessage;
import com.caucho.v5.management.server.HealthEventLog.HealthEventLogType;

public class HealthSystemAdmin 
  extends ManagedObjectBase implements HealthSystemMXBean
{
  private HealthSubSystem _healthSystem;
  private String[] _statusNames;
  
  HealthSystemAdmin(HealthSubSystem healthService)
  {
    _healthSystem = healthService;

    registerSelf();
  }
  
  @Override
  public boolean isEnabled()
  {
    return _healthSystem.isEnabled();
  }
  
  @Override
  public long getStartupDelay()
  {
    return _healthSystem.getDelay();
  }
  
  @Override
  public long getPeriod()
  {
    return _healthSystem.getPeriod();
  }
  
  @Override
  public long getRecheckPeriod()
  {
    return _healthSystem.getRecheckPeriod();
  }
  
  @Override
  public int getRecheckMax()
  {
    return _healthSystem.getRecheckMax();
  }
  
  @Override
  public long getSystemRecheckTimeout()
  {
    return _healthSystem.getSystemRecheckTimeout();
  }
  
  @Override
  public Date getLastCheckStartTime()
  {
    long l = _healthSystem.getLastCheckStartTime();
    return l > 0 ? new Date(l) : null;
  }
  
  @Override
  public Date getLastCheckFinishTime()
  {
    long l = _healthSystem.getLastCheckFinishTime();
    return l > 0 ? new Date(l) : null;
  }
  
  @Override
  public String[] getHealthStatusNames()
  {
    if (_statusNames == null) {
      HealthStatus[] statusValues = HealthStatus.values();
      _statusNames = new String[statusValues.length];
      for (int i = 0; i < statusValues.length; i++) {
        _statusNames[i] = statusValues[i].toString();
      }
    }
    
    return _statusNames;
  }
  
  @Override
  public String[] getLabels()
  {
    return getHealthStatusNames();
  }

  @Override
  public String getState()
  {
    return _healthSystem.getLifecycleState().getStateName();
  }
  
  public HealthEventLog []findEvents(int serverIndex, 
                                     long minTime, long maxTime, 
                                     int limit)
  {
    List<HealthEventLog> eventList = new ArrayList<HealthEventLog>();

    StatSystem statSystem = StatSystem.getCurrent();
    if (statSystem != null) {
      long[] startTimes = statSystem.getStartTimes(serverIndex, 
                                                   minTime, maxTime);
      
      if (startTimes != null) {
        for (long startTime : startTimes) {
          HealthEventLog eventLog = 
            new HealthEventLog(HealthEventLogType.START_TIME);
          eventLog.setTimestamp(startTime);
          
          eventList.add(eventLog);
        }
      }
    }
    
    LogSystem logSystem = LogSystem.getCurrent();
    if (logSystem != null) {
      String serverPrefix = String.format("%02d|", serverIndex);
      
      List<String> typesList = new ArrayList<String>();
      for (HealthEventLogType eventType : HealthEventLogType.values()) {
        if (eventType == HealthEventLogType.START_TIME)
          continue;
        
        typesList.add(serverPrefix + eventType.getName());
      }
      
      String[] types = new String[typesList.size()];
      typesList.toArray(types);
      
      LogMessage[] messages = logSystem.findMessages(types, Level.INFO.getName(), minTime, maxTime);
        
      if (messages != null) {
        for (LogMessage message : messages) {
          HealthEventLogType eventLogType = 
            HealthEventLogType.fromTypeName(message.getType());
          
          if (eventLogType == null)
            continue;
          
          HealthEventLog eventLog = new HealthEventLog(eventLogType);
          eventLog.setTimestamp(message.getTimestamp());
          eventLog.setSource(message.getName());
          eventLog.setMessage(message.getMessage());
          
          eventList.add(eventLog);
        }
      }
    }
      
    Collections.sort(eventList);
    
    if (eventList.size() > limit)
      eventList = eventList.subList(0, limit);
    
    HealthEventLog []eventArray = new HealthEventLog[eventList.size()];
    eventList.toArray(eventArray);
    
    return eventArray;
  }
  
  @Override
  public String getName()
  {
    return null;
  }
}
