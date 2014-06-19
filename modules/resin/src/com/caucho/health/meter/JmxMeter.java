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
import com.caucho.env.meter.MeterService;
import com.caucho.util.L10N;

@Startup
@Configurable
public class JmxMeter
{
  private static final L10N L = new L10N(JmxMeter.class);

  private String _name;
  private String _objectName;
  private String _attribute;
  private boolean _isOptional;

  @PostConstruct
  public void init() throws Exception
  {
    if (_name == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'name' attribute",
                                    getClass().getSimpleName()));
    
    if (_objectName == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'objectName' attribute",
                                    getClass().getSimpleName()));
   
    if (_attribute == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'attribute' attribute",
                                    getClass().getSimpleName()));
    
    createMeter();
  }
  
  protected void createMeter() throws Exception
  {
    MeterService.createJmx(getName(),
                           getObjectName(),
                           getAttribute(),
                           isOptional());
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

  public String getObjectName()
  {
    return _objectName;
  }

  @ConfigArg(1)
  public void setObjectName(String objectName)
  {
    _objectName = objectName;
  }

  public String getAttribute()
  {
    return _attribute;
  }

  @ConfigArg(2)
  public void setAttribute(String attribute)
  {
    _attribute = attribute;
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
