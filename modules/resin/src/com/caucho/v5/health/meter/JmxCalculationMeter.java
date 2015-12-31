/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.meter;

import io.baratine.service.Startup;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.meter.JmxExpr;
import com.caucho.v5.health.meter.MeterService;
import com.caucho.v5.util.L10N;

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
    if (true) throw new UnsupportedOperationException();
    //MeterService.createJmxCalculation(getName(), _expr);
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
