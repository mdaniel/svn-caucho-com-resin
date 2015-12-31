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
import com.caucho.v5.jmx.server.MeterGraphSectionInfo;
import com.caucho.v5.util.L10N;

@Startup
@Configurable
@SuppressWarnings("serial")
public class MeterGraphSection implements MeterGraphSectionInfo, Serializable
{
  private static final L10N L = new L10N(MeterGraphSection.class);

  private String _name;
  
  private ArrayList<MeterGraph> _meterGraphs = new ArrayList<MeterGraph>();
  
  public MeterGraphSection()
  {
  }
  
  @ConstructorProperties({"name"})
  public MeterGraphSection(String name)
  {
    _name = name;
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
 
  public MeterGraph createGraph()
  {
    MeterGraph graph = new MeterGraph();
    
    graph.setEmbedded(true);
    
    return graph;
  }
  
  @Configurable
  public void addGraph(MeterGraph graph)
  {
    _meterGraphs.add(graph);
  }
  
  @Configurable
  public void add(MeterGraph graph)
  {
    addGraph(graph);
  }

  @Override
  public MeterGraph []getMeterGraphs()
  {
    MeterGraph []graphs = new MeterGraph[_meterGraphs.size()];
    
    return _meterGraphs.toArray(graphs);
  }

  @PostConstruct
  public void init() throws Exception
  {
    if (_name == null) 
      throw new ConfigException(L.l("<health:{0}> requires 'name' attribute",
                                    getClass().getSimpleName()));
    
    if (_meterGraphs.size() == 0)
      throw new ConfigException(L.l("<health:{0}> requires at least one graph element",
                                    getClass().getSimpleName()));
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
