/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.cloud.jmx;

import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Remote mbean
 */
public class RemoteMBean {
  private JmxClient _client;
  private String _name;
  private HashMap<String,Object> _attr;
  private long _expireTime;
  
  RemoteMBean(JmxClient client, String name, HashMap<String,Object> attr)
  {
    _client = client;
    _name = name;
    _attr = attr;
    _expireTime = CurrentTime.getCurrentTime() + 5000L;
  }

  public Iterator iterator()
  {
    ArrayList<String> names = new ArrayList<String>(_attr.keySet());

    Collections.sort(names);

    return names.iterator();
  }
  
  /**
   * Returns the given attribute.
   */
  public Object __getField(String name)
  {
    fillAttributes();
    
    return _attr.get(name);
  }

  private void fillAttributes()
  {
    if (CurrentTime.getCurrentTime() < _expireTime)
      return;
    
    _expireTime = CurrentTime.getCurrentTime() + 5000L;
  }

  public String toString()
  {
    return "RemoteMBean[" + _name + "]";
  }
}
