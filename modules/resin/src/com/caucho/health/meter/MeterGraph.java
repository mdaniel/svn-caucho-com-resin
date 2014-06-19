/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.health.meter;

import io.baratine.core.Startup;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigArg;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.health.stat.StatSystem;
import com.caucho.jmx.server.MeterGraphInfo;
import com.caucho.util.L10N;

@Startup
@Configurable
@SuppressWarnings("serial")
public class MeterGraph implements MeterGraphInfo, Serializable
{
  private static final L10N L = new L10N(MeterGraph.class);

  private boolean _isEmbedded;
  private String _name;
  private ArrayList<String> _meterNames = new ArrayList<String>();
  
  public MeterGraph()
  {
  }
  
  @ConstructorProperties({"name", "embedded"})
  public MeterGraph(String name,
                    boolean isEmbedded)
  {
    _name = name;
    _isEmbedded = isEmbedded;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @ConfigArg(0)
  public void setName(String name)
  {
    _name = name;
  }
  
  @Configurable
  public void addMeter(String name)
  {
    _meterNames.add(name);
  }

  @Override
  public String []getMeterNames()
  {
    String []names = new String[_meterNames.size()];
    
    return _meterNames.toArray(names);
  }
  
  public void setEmbedded(boolean isEmbedded)
  {
    _isEmbedded = true;
  }

  @PostConstruct
  public void init() throws Exception
  {
    /*
    if (_name == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'name' attribute",
                                    getClass().getSimpleName()));
                                    */
    
    if (_meterNames.size() == 0)
      throw new ConfigException(L.l("<health:{0}> requires at least one meter attribute",
                                    getClass().getSimpleName()));
    
    StatSystem statSystem = StatSystem.getCurrent();
    
    if (statSystem != null && _name != null && ! _isEmbedded)
      statSystem.addMeterGraph(this);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
