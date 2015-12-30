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

package com.caucho.v5.health.stat;

import io.baratine.core.MethodRef;
import io.baratine.core.ResultFuture;
import io.baratine.core.ServiceManager;
import io.baratine.core.ServiceRef;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.health.meter.MeterBase;

public final class ServiceMeterImpl extends MeterBase {
  private static final Logger log
    = Logger.getLogger(ServiceMeterImpl.class.getName());

  private boolean _isOptional;
  
  private double _value;

  private String _address;

  private String _methodName;

  private ServiceRef _serviceRef;

  private MethodRef _methodRef;

  public ServiceMeterImpl(String name,
                           String address,
                           String methodName)
  {
    super(name);

    ServiceManager manager = ServiceManager.current();
    
    _address = address;
    _methodName = methodName;
    
    _serviceRef = manager.lookup(_address);
    _methodRef = _serviceRef.getMethod(methodName);
  }

  /**
   * Polls the statistics attribute.
   */
  @Override
  public void sample()
  {
    try {
      ResultFuture<Object> future = new ResultFuture<>();
      
      _methodRef.query(null, future);
      
      Object value = future.get(1, TimeUnit.SECONDS);

      if (value == null) {
        _value = 0;
        return;
      }
      
      _value = ((Number) value).doubleValue();
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, e.toString(), e);
      }
      else {
        log.fine(e.toString());
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
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "," + _address + "," + _methodName + "]";
  }
}
