/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.v5.env.meter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.meter.MeterBase;
import com.caucho.v5.jmx.JmxUtilResin;

public final class JmxAttributeMeter extends MeterBase {
  private static final Logger log
    = Logger.getLogger(JmxAttributeMeter.class.getName());

  private MBeanServer _server;
  private ObjectName _objectName;
  private String _attribute;
  private boolean _isOptional;
  
  private double _lastSample;
  private double _value;

  public JmxAttributeMeter(String name,
                           String objectName,
                           String attribute,
                           boolean isOptional)
  {
    super(name);

    try {
      _objectName = JmxUtilResin.getServerObjectName(objectName);
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }

    _attribute = attribute;
    _isOptional = isOptional;
    _server = JmxUtilResin.getMBeanServer();
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
        return;
      }
      
      _value = ((Number) value).doubleValue();
    } catch (Exception e) {
      if (isOptional()
          && (e instanceof InstanceNotFoundException
              || e instanceof AttributeNotFoundException)) {
        log.log(Level.FINEST, e.toString(), e);
      }
      else {
        log.log(Level.FINE, e.toString(), e);
      }

      _value = 0;
    }
  }
  
  @Override
  public double calculate()
  {
    return _value;
  }
  
  @Override
  public double peek()
  {
    return _value;
  }

  public boolean isOptional()
  {
    return _isOptional;
  }

  public void setOptional(boolean optional)
  {
    _isOptional = optional;
  }
}
