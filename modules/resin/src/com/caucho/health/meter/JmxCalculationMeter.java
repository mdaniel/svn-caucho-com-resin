/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.health.meter;

import io.baratine.core.Startup;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigArg;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.env.meter.JmxExpr;
import com.caucho.env.meter.MeterService;
import com.caucho.util.L10N;

@Startup
@Configurable
public class JmxCalculationMeter
{
  private static final L10N L = new L10N(JmxCalculationMeter.class);

  private String _name;
  private JmxExpr _expr;
  private boolean _isOptional;
  
  public void addValue(JmxExpr.JmxValue expr)
  {
    add(expr);
  }

  public void addDelta(JmxExpr.JmxDelta expr)
  {
    add(expr);
  }

  public void addRate(JmxExpr.JmxRate expr)
  {
    add(expr);
  }

  public void addRatio(JmxExpr.JmxRatio expr)
  {
    add(expr);
  }

  public void addAdd(JmxExpr.JmxAdd expr)
  {
    add(expr);
  }
  
  public void add(JmxExpr expr)
  {
    _expr = expr;
  }

  @PostConstruct
  public void init() throws Exception
  {
    if (_name == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'name' attribute",
                                    getClass().getSimpleName()));
    
    createMeter();
  }
  
  protected void createMeter() throws Exception
  {
    MeterService.createJmxCalculation(getName(), _expr);
  }
  
  public String getName()
  {
    return _name;
  }

  @ConfigArg(0)
  public void setName(String name)
  {
    _name = name;
  }

  public boolean isOptional()
  {
    return _isOptional;
  }

  @Configurable
  public void setOptional(boolean optional)
  {
    _isOptional = optional;
  }
}
