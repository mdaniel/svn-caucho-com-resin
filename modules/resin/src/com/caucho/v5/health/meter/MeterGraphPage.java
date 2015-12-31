/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.meter;

import io.baratine.service.Startup;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.jmx.server.MeterGraphPageInfo;
import com.caucho.v5.util.L10N;

@Startup
@Configurable
@SuppressWarnings("serial")
public class MeterGraphPage implements MeterGraphPageInfo, Serializable
{
  private static final L10N L = new L10N(MeterGraphPage.class);

  private String _name;
  
  private int _columns;
  private long _period;
  
  private boolean _isSummary = true;
  private boolean _isHeapDump = true;
  private boolean _isProfile = true;
  private boolean _isLog = true;
  private boolean _isThreadDump = true;
  private boolean _isJmxDump = true;
  
  private ArrayList<MeterGraphSection> _meterSections 
    = new ArrayList<MeterGraphSection>();
  
  public MeterGraphPage()
  {
  }
  
  @ConstructorProperties({"name", "columns", "period",
                          "summary", "profile", "log" })
  public MeterGraphPage(String name,
                        int columns,
                        long period,
                        boolean isSummary,
                        boolean isProfile,
                        boolean isLog)
  {
    _name = name;
    _columns = columns;
    _isSummary = isSummary;
    _isProfile = isProfile;
    _isLog = isLog;
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
  
  public void setColumns(int columns)
  {
    _columns = columns;
  }
  
  @Override
  public int getColumns()
  {
    return _columns;
  }
  
  public void setPeriod(Period period)
  {
    _period = period.getPeriod();
  }
  
  @Override
  public long getPeriod()
  {
    return _period;
  }
  
  @Configurable
  public void setSummary(boolean isEnable)
  {
    _isSummary = isEnable;
  }

  @Override
  public boolean isSummary()
  {
    return _isSummary;
  }
  
  @Configurable
  public void setLog(boolean isEnable)
  {
    _isLog = isEnable;
  }

  @Override
  public boolean isLog()
  {
    return _isLog;
  }
  
  @Configurable
  public void setHeapDump(boolean isEnable)
  {
    _isHeapDump = isEnable;
  }

  @Override
  public boolean isHeapDump()
  {
    return _isHeapDump;
  }
  
  @Configurable
  public void setProfile(boolean isEnable)
  {
    _isProfile = isEnable;
  }

  @Override
  public boolean isProfile()
  {
    return _isProfile;
  }
  
  @Configurable
  public void setThreadDump(boolean isEnable)
  {
    _isThreadDump = isEnable;
  }

  @Override
  public boolean isThreadDump()
  {
    return _isThreadDump;
  }
  
  @Configurable
  public void setJmxDump(boolean isEnable)
  {
    _isJmxDump = isEnable;
  }

  @Override
  public boolean isJmxDump()
  {
    return _isJmxDump;
  }

  public MeterGraphSection createSection()
  {
    return new MeterGraphSection();
  }
  
  @Configurable
  public void addSection(MeterGraphSection section)
  {
    _meterSections.add(section);
  }
  
  @Configurable
  public void add(MeterGraphSection section)
  {
    addSection(section);
  }

  public MeterGraph createGraph()
  {
    MeterGraph graph = new MeterGraph();
    
    graph.setEmbedded(true);
    
    return graph;
  }
  
  @Configurable
  public void addGraph(MeterGraph graph)
  {
    getDefaultSection().addGraph(graph);
  }
  
  @Configurable
  public void add(MeterGraph graph)
  {
    getDefaultSection().addGraph(graph);
  }

  @Override
  public MeterGraphSection []getMeterSections()
  {
    MeterGraphSection []sections = new MeterGraphSection[_meterSections.size()];
    
    return _meterSections.toArray(sections);
  }

  @Override
  public MeterGraph []getMeterGraphs()
  {
    if (_meterSections.size() > 0) {
      return _meterSections.get(0).getMeterGraphs();
    }
    else {
      return new MeterGraph[0];
    }
  }
  
  private MeterGraphSection getDefaultSection()
  {
    if (_meterSections.size() == 0) {
      MeterGraphSection section = new MeterGraphSection();
      section.setName("");
      
      addSection(section);
    }
    
    return _meterSections.get(0);
  }

  @PostConstruct
  public void init() throws Exception
  {
    if (_name == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'name' attribute",
                                    getClass().getSimpleName()));
    
    if (_meterSections.size() == 0)
      throw new ConfigException(L.l("<health:{0}> requires at least one graph element",
                                    getClass().getSimpleName()));
    
    
    StatSystem statSystem = StatSystem.getCurrent();
    
    if (statSystem != null)
      statSystem.addMeterGraphPage(this);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
