/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.management.server;

import java.beans.ConstructorProperties;
import java.io.Serializable;

import com.caucho.v5.util.QDate;

/**
 * Persistent logging.
 */
@SuppressWarnings("serial")
public class HealthEventLog 
  implements Serializable, Comparable<HealthEventLog>
{
  
  private HealthEventLogType _type;
  private long _timestamp;
  private String _source;
  private String _message;

  public HealthEventLog()
  {
    
  }

  @ConstructorProperties({"timestamp", "typeName", "source", "message"})
  public HealthEventLog(long timestamp,
                        String typeName,
                        String source,
                        String message)
  {
    _timestamp = timestamp;
    _type = HealthEventLogType.fromTypeName(typeName);
    _source = source;
    _message = message;
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
  public static enum HealthEventLogType
  {
    START_MESSAGE("Caucho|Startup", "Startup Message"),
    START_TIME("Caucho|StartTime", "Server Startup"),
    CHECK("Caucho|Health|Check", "Health Alarm"),
    RECHECK("Caucho|Health|Recheck", "Recheck"),
    RECOVER("Caucho|Health|Recover", "Health Recovery"),
    ACTION("Caucho|Health|Action", "Health Action"),
    ANOMALY("Caucho|Health|Anomaly", "Anomaly Detected");
    
    private final String _name;
    private final String _description;
    
    private HealthEventLogType(String name, String description)
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
      for (HealthEventLogType type : HealthEventLogType.values()) {
        if (name.endsWith(type.getName()))
          return type;
      }
      
      return null;
    }
  }
}
