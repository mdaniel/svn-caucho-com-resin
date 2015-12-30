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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.meter.MeterBase;
import com.caucho.v5.jmx.JmxUtilResin;
import com.caucho.v5.util.L10N;

public final class JmxCalculationMeterImpl extends MeterBase {
  private double _value;
  
  private JmxExpr _expr;
  
  public JmxCalculationMeterImpl(String name, JmxExpr expr)
  {
    super(name);
    
    _expr = expr;
  }

  /**
   * Polls the statistics attribute.
   */
  @Override
  public void sample()
  {
    if (_expr != null) {
      _expr.sample();
    }
    
    _value = _expr.calculate();
  }
  
  @Override
  public double calculate()
  {
    return _value;
  }
}
