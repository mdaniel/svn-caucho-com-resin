/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.env.log;

import java.io.InputStream;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import com.caucho.config.*;
import com.caucho.env.service.*;
import com.caucho.management.server.LogMessage;
import com.caucho.vfs.WriteStream;

@Service
public class LogSystem extends AbstractResinSubSystem
{
  public LogSystem()
  {
    
  }
  
  @PostConstruct
  public void init()
  {
  }
  
  public static LogSystem getCurrent()
  {
    return ResinSystem.getCurrentService(LogSystem.class);
  }
  
  public void setLevel(Level level) 
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void setExpireTimeout(long timeout)
  {
  }
  
  public long getExpireTimeout()
  {
    return -1;
  }
  
  public void setLogMax(int max)
  {
  }
  
  public String createFullType(String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void log(String fullType, String message)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void log(String fullType, String name, Level level, String message)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void log(long timestamp, 
                  String fullType, 
                  String name, 
                  Level level, 
                  String message)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void logStream(String fullType, InputStream is)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void logStream(long timestamp, 
                        String fullType, 
                        String name, 
                        Level level, 
                        InputStream is)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public WriteStream openLogStream(String fullType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }


  public WriteStream openLogStream(long timestamp, 
                                   String fullType, 
                                   String name, 
                                   Level level)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long []findMessageTimes(String fullType, 
                                 String levelName,
                                 long minTime, 
                                 long maxTime)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public LogMessage []findMessages(String fullType, 
                                   String levelName,
                                   long minTime, 
                                   long maxTime)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public LogMessage []findMessages(String []fullTypes, 
                                   String levelName,
                                   long minTime, 
                                   long maxTime)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public LogMessage []findMessagesByName(String fullType,
                                         String logName,
                                         String levelName,
                                         long minTime, 
                                         long maxTime)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public LogMessage []findMessagesByName(String []fullTypes,
                                         String logName,
                                         String levelName,
                                         long minTime, 
                                         long maxTime)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
