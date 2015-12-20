/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.meter;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.InlineConfig;
import com.caucho.v5.health.stat.ServiceMeterImpl;
import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.util.L10N;

@Configurable
@InlineConfig
public class ServiceMeter
{
  private static final L10N L = new L10N(ServiceMeter.class);

  private String _name;
  private String _address;
  private String _methodName;
  private boolean _isOptional;

  private ServiceMeterImpl _meter;
  
  public String getName()
  {
    return _name;
  }

  @ConfigArg(0)
  public void setName(String name)
  {
    _name = name;
  }

  public String getAddress()
  {
    return _address;
  }

  @ConfigArg(1)
  public void setAddress(String objectName)
  {
    _address = objectName;
  }

  public String getMethod()
  {
    return _methodName;
  }

  @ConfigArg(2)
  public void setMethod(String methodName)
  {
    _methodName = methodName;
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

  @PostConstruct
  public void init() throws Exception
  {
    if (_name == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'name' attribute",
                                    getClass().getSimpleName()));
    
    if (_address == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'address' attribute",
                                    getClass().getSimpleName()));
   
    if (_methodName == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'method' attribute",
                                    getClass().getSimpleName()));
    _meter = new ServiceMeterImpl(_name, _address, _methodName);
    
    StatSystem statSystem = StatSystem.getCurrent();
    
    statSystem.addMeter(_meter);
  }
}
