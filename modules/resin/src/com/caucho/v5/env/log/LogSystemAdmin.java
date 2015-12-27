/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.log;

import java.util.Arrays;

import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.management.server.LogMessage;
import com.caucho.v5.management.server.LogServiceMXBean;

/**
 * Persistent logging.
 */
public class LogSystemAdmin 
  extends ManagedObjectBase 
  implements LogServiceMXBean
{
  private LogSystem _logSystem;
  private String _type;

  public LogSystemAdmin(LogSystem logSystem)
  {
    _logSystem = logSystem;
    
    _type = _logSystem.createFullType("Resin|Log"); 
    
    registerSelf();
  }

  @Override
  public String getName()
  {
    return null;
  }
  
  @Override
  public long getExpireTimeout()
  {
    return _logSystem.getExpireTimeout();
  }
  
  @Override
  public void setExpireTimeout(long timeout)
  {
    _logSystem.setExpireTimeout(timeout);
  }
  
  @Override
  public LogMessage []findMessages(String level, long minTime, long maxTime)
  {
    return _logSystem.findMessages(_type, level, minTime, maxTime);
  }
  
  @Override
  public LogMessage []findMessagesByName(String name, 
                                         String level, 
                                         long minTime, 
                                         long maxTime)
  {
    return _logSystem.findMessagesByName(_type, name, level, minTime, maxTime);
  }
  
  @Override
  public long []findMessageTimesByType(String type, 
                                             String level, 
                                             long minTime, 
                                             long maxTime)
  {
    type = _logSystem.createFullType(type);

    return  _logSystem.findMessageTimes(type, level, minTime, maxTime);
  }
  
  @Override
  public LogMessage []findMessagesByType(String type, 
                                         String level, 
                                         long minTime, 
                                         long maxTime)
  {
    type = _logSystem.createFullType(type);

    return _logSystem.findMessages(type, level, minTime, maxTime);
  }
}
