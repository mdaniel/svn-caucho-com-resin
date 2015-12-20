/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.v5.env.meter.MeterBase;

/**
 * Statistics gathering attribute.  Each time period, the attribute is polled.
 */
public class JmxPercentStatAttribute extends MeterBase
{
  private static final Logger log
    = Logger.getLogger(JmxPercentStatAttribute.class.getName());
  
  private MBeanServer _server;
  private ObjectName _objectName;
  private String _attribute;
  
  private double _value;

  JmxPercentStatAttribute(String name,
                          MBeanServer server,
                          ObjectName objectName,
                          String attribute)
  {
    super(name);

    _server = server;
    _objectName = objectName;
    _attribute = attribute;
  }

  /**
   * Polls the statistics attribute.
   */
  @Override
  public void sample()
  {
    try {
      Object value = _server.getAttribute(_objectName, _attribute);

      if (value == null) {
        _value = 0;
      }
      
      _value = 100.0 * ((Number) value).doubleValue();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      _value = 0;
    }
  }
  
  @Override
  public double calculate()
  {
    return _value;
  }
}
