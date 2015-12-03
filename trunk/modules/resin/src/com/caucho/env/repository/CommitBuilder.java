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

package com.caucho.env.repository;

import java.util.HashMap;
import java.util.Map;

import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

/**
 * Builds a repository commit.
 */
public class CommitBuilder
{
  private static final L10N L = new L10N(CommitBuilder.class);
  
  private String _stage = "production";
  private String _type;
  private String _tagKey;
  private String _version;
  
  private Map<String,String> _attributes
    = new HashMap<String,String>();
  
  private long _date;
  private String _timestamp;
  
  /**
   * Assigns the commit's stage.
   */
  public CommitBuilder stage(String stage)
  {
    if (stage.isEmpty())
      throw new IllegalArgumentException(L.l("{0} stage must not be empty",
                                             this));
    
    _stage = stage;
    
    return this;
  }
  
  public String getStage()
  {
    return _stage;
  }
  
  /**
   * Assigns the commit's type.
   */
  public CommitBuilder type(String type)
  {
    if (type.isEmpty())
      throw new IllegalArgumentException(L.l("{0} type must not be empty",
                                             type));
    
    _type = type;
    
    return this;
  }
  
  public String getType()
  {
    return _type;
  }
  
  /**
   * Sets the tagKey
   */
  public CommitBuilder tagKey(String key)
  {
    _tagKey = key;
    
    return this;
  }
  
  public String getTagKey()
  {
    return _tagKey;
  }

  /**
   * Assign a version.
   */
  public CommitBuilder version(int major, int minor, int micro, 
                               String qualifier)
  {
    if (qualifier != null && ! qualifier.isEmpty())
      _version = major + "." + minor + "." + micro + "." + qualifier;
    else
      _version = major + "." + minor + "." + micro;
    
    attribute("version", _version);
    
    return this;
  }
  
  public String getVersion()
  {
    return _version;
  }
  
  /**
   * Assign the commit message.
   */
  public CommitBuilder message(String message)
  {
    return attribute("message", message);
  }
  
  /**
   * Assign a generic attribute
   */
  public CommitBuilder attribute(String key, String value)
  {
    _attributes.put(key, value);
    
    return this;
  }
  
  public Map<String,String> getAttributes()
  {
    return _attributes;
  }
  
  public String getId()
  {
    String stage = getStage();
    String type = getType();
    String key = getTagKey();
    String version = getVersion();
    
    if (type == null)
      throw new IllegalStateException(L.l("{0} requires a non-empty type",
                                          this));
    
    if (key == null) {
      throw new IllegalStateException(L.l("{0} requires a non-empty key",
                                          this));
      
    }
    
    if (version != null)
      return stage + "/" + type + "/" + key + "-" + version;
    else
      return stage + "/" + type + "/" + key;
  }
  
  public String getArchiveId()
  {
    String type = getType();
    String key = getTagKey();
    String version = getVersion();
    
    if (type == null)
      throw new IllegalStateException(L.l("{0} requires a non-empty type",
                                          this));
    
    if (key == null) {
      throw new IllegalStateException(L.l("{0} requires a non-empty key",
                                          this));
    }
    
    if (_timestamp == null) {
      throw new IllegalStateException(L.l("{0} requires an active timestamp",
                                          this));
    }
    
    if (version != null)
      return "archive/" + type + "/" + key + "-" + version + "/" + _timestamp;
    else
      return "archive/" + type + "/" + key + "/" + _timestamp;
  }
  
  public void validate()
  {
    if (getId() == null)
      throw new IllegalStateException(L.l("{0} requires a non-empty id",
                                          this));

    if (_date == 0)
      _date = CurrentTime.getCurrentTime();
    
    QDate qDate = QDate.allocateLocalDate();
    qDate.setGMTTime(_date);
    
    _timestamp = qDate.printISO8601();
    
    QDate.freeLocalDate(qDate);
    
    _attributes.put("date", _timestamp);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName();
  }
}
