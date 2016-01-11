/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

import java.util.ArrayList;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.bartender.network.NetworkSystem;
import com.caucho.v5.config.InlineConfig;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.system.SubSystemBase;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.analysis.AnomalyAnalyzer;
import com.caucho.v5.health.meter.Meter;
import com.caucho.v5.health.meter.MeterBase;
import com.caucho.v5.jmx.server.MeterGraphInfo;
import com.caucho.v5.jmx.server.MeterGraphPageInfo;
import com.caucho.v5.management.server.BaselineQueryResult;
import com.caucho.v5.management.server.DownTime;
import com.caucho.v5.management.server.StatServiceValue;
import com.caucho.v5.util.Crc64;

@InlineConfig
public class StatSystem extends SubSystemBase
{
  public static final int START_PRIORITY = 
      NetworkSystem.START_PRIORITY_CLUSTER_SERVICE;

  private StatServiceLocal _statService;
  
  protected StatSystem()
  {
    StatServiceLocalImpl statServiceImpl = new StatServiceLocalImpl();
    
    ServiceManagerAmp ampManager = AmpSystem.getCurrentManager();
    
    _statService = ampManager.newService(statServiceImpl).as(StatServiceLocal.class);
  }

  public static StatSystem createAndAddSystem()
  {
    SystemManager system = preCreate(StatSystem.class);
    
    StatSystem statSystem = new StatSystem();
    system.addSystem(StatSystem.class, statSystem);
    
    return statSystem;
  }

  public static StatSystem getCurrent()
  {
    return SystemManager.getCurrentSystem(StatSystem.class);
  }

  @PostConstruct
  public void init()
  {
  }

  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  @Override
  public void start()
  {
    _statService.start();
    
    StatProbeManager statProbeManager = new StatProbeManager();

    if (statProbeManager != null) {
      statProbeManager.setService(_statService);
    }
  }

  public void setSamplePeriod(Period period)
  {
    throw new AbstractMethodError();
  }

  public long getSamplePeriod()
  {
    throw new AbstractMethodError();
  }

  public Set queryNames(String objectName)
  {
    throw new AbstractMethodError();
  }

  public void addJmxMeter(String name, String objectName, String attribute)
  {
    throw new AbstractMethodError();
  }

  public void addJmx(JmxItem item)
  {
    throw new AbstractMethodError();
  }

  public void addJmxDelta(JmxItem item)
  {
    throw new AbstractMethodError();
  }

  public void addJmxDeltaMeter(String name, String objectName, String attribute)
  {
    throw new AbstractMethodError();
  }

  public void addJmxPercentMeter(String name,
                                 String objectName,
                                 String attribute)
  {
    throw new AbstractMethodError();
  }

  public void addMeter(MeterBase probe)
  {
    _statService.addMeter(probe);
  }

  public Meter getMeter(String name)
  {
    return _statService.getMeter(name);
  }

  public void addSample(Sample sample)
  {
    _statService.addSample(sample);
  }

  public void addSample(long now, long id, double data)
  {
    _statService.addSampleValue(now, id, data);
  }

  public void addSample(long now, long[] sampleIds, double[] sampleData)
  {
    _statService.addSampleValues(now, sampleIds, sampleData);
  }

  public double getCpuLoad()
  {
    return 0;
  }

  public StatServiceValue[] getStatisticsData(String name,
                                              long beginTime,
                                              long endTime,
                                              long step)
  {
    throw new AbstractMethodError();
  }

  public StatServiceValue[] getStatisticsData(long id,
                                              long beginTime,
                                              long endTime,
                                              long step)
  {
    throw new AbstractMethodError();
  }

  public double getLastValue(String name)
  {
    throw new AbstractMethodError();
  }

  public double getLastValue(long id)
  {
    throw new AbstractMethodError();
  }

  public BaselineQueryResult getBaseline(String name,
                                         long beginTime,
                                         long endTime,
                                         int minSampleSize)
  {
    throw new AbstractMethodError();
  }

  public BaselineQueryResult getBaseline(long id,
                                         long beginTime,
                                         long endTime,
                                         int minSampleSize)
  {
    throw new AbstractMethodError();
  }

  public String[] getStatisticsNames()
  {
    throw new AbstractMethodError();
  }

  public void addMeterGraph(MeterGraphInfo meterGraph)
  {
    _statService.addMeterGraph(meterGraph);
  }

  public void addMeterGraphPage(MeterGraphPageInfo meterGraphPage)
  {
    _statService.addMeterGraphPage(meterGraphPage);
  }

  public MeterGraphInfo[] getMeterGraphs()
  {
    throw new AbstractMethodError();
  }

  public MeterGraphPageInfo[] getMeterGraphPages()
  {
    throw new AbstractMethodError();
  }
  
  public MeterGraphPageInfo getMeterGraphPage(String name)
  {
    throw new AbstractMethodError();
  }

  public long[] getStartTimes(int index, long startTime, long endTime)
  {
    throw new AbstractMethodError();
  }
  
  public DownTime []getDownTimes(int index, long beginTime, long endTime)
  {
    throw new AbstractMethodError();
  }

  public ArrayList<MeterBase> getCpuMeters()
  {
    return _statService.getCpuMeters();
  }

  protected static class Sample
  {
    private final long _id;
    private final String _name;
    private final MeterBase _probe;

    Sample(String name, MeterBase probe)
    {
      _id = Crc64.generate(name);
      _name = name;
      _probe = probe;
    }

    final long getId()
    {
      return _id;
    }

    final String getName()
    {
      return _name;
    }
    
    final MeterBase getMeter()
    {
      return _probe;
    }
    
    void sample()
    {
      _probe.sample();
    }
    
    double calculate()
    {
      return _probe.calculate();
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _name + "," + _probe + "]";
    }
  }

  public static class JmxItem
  {
    private String _name;
    private String _objectName;
    private String _attribute;

    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setObjectName(String objectName)
    {
      _objectName = objectName;
    }

    public String getObjectName()
    {
      return _objectName;
    }

    public void setAttribute(String attribute)
    {
      _attribute = attribute;
    }

    public String getAttribute()
    {
      return _attribute;
    }
  }

  public void addAnalyzer(AnomalyAnalyzer anomalyAnalyzer)
  {
    // TODO Auto-generated method stub
    
  }
}
