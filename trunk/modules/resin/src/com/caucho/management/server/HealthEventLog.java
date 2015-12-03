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

package com.caucho.management.server;

import java.io.Serializable;

import com.caucho.util.QDate;

/**
 * Persistent logging.
 */
@SuppressWarnings("serial")
public class HealthEventLog 
  implements Serializable, Comparable<HealthEventLog>
{
  public static enum HealthEventLogType
  {
    START_MESSAGE("Resin|Startup", "Startup Message"),
    START_TIME("Resin|StartTime", "Server Startup"),
    CHECK("Resin|Health|Check", "Health Alarm"),
    RECHECK("Resin|Health|Recheck", "Recheck"),
    RECOVER("Resin|Health|Recover", "Health Recovery"),
    ACTION("Resin|Health|Action", "Health Action"),
    ANOMALY("Resin|Health|Anomaly", "Anomaly Detected");
    
    private final String _name;
    private final String _description;
    
    HealthEventLogType(String name, String description)
    {
      _name = name;
      _description = description;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public String getDescription()
    {
      return _description;
    }
    
    public static HealthEventLogType fromTypeName(String name)
    {
      for(HealthEventLogType type : HealthEventLogType.values()) {
        if (name.endsWith(type.getName()))
          return type;
      }
      
      return null;
    }
  }
  
  private HealthEventLogType _type;
  private long _timestamp;
  private String _source;
  private String _message;

  public HealthEventLog()
  {
    
  }

  public HealthEventLog(HealthEventLogType type)
  {
    _type = type;
  }

  public long getTimestamp()
  {
    return _timestamp;
  }

  public void setTimestamp(long timestamp)
  {
    _timestamp = timestamp;
  }

  public HealthEventLogType getType()
  {
    return _type;
  }
  
  public String getTypeName()
  {
    return _type.getName();
  }
  
  public String getTypeDescription()
  {
    return _type.getDescription();
  }

  public void setType(HealthEventLogType type)
  {
    _type = type;
  }

  public String getSource()
  {
    return _source;
  }

  public void setSource(String source)
  {
    _source = source;
  }

  public String getMessage()
  {
    return _message;
  }

  public void setMessage(String message)
  {
    _message = message;
  }

  @Override
  public String toString()
  {
    return String.format("%s[%s, %s, %s, %s]",
                         this.getClass().getSimpleName(),
                         _type,
                         QDate.formatISO8601(_timestamp),
                         _source,
                         _message);
  }

  @Override
  public int compareTo(HealthEventLog o)
  {
    long thisVal = getTimestamp();
    long anotherVal = o.getTimestamp();
    
    
    if (thisVal == anotherVal)
      return this.getType().compareTo(o.getType());
    else
      return (thisVal<anotherVal ? 1 : -1);
  }
}
