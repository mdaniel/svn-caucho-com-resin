/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import com.caucho.config.Service;
import com.caucho.config.types.Period;
import com.caucho.env.meter.AbstractMeter;
import com.caucho.env.service.AbstractResinSubSystem;
import com.caucho.env.service.ResinSystem;
import com.caucho.management.server.*;
import com.caucho.util.Crc64;

import java.util.ArrayList;
import java.util.Set;

import javax.annotation.PostConstruct;

@Service
public class StatSystem extends AbstractResinSubSystem
{
  protected StatSystem()
  {
  }

  public static StatSystem getCurrent()
  {
    return ResinSystem.getCurrentService(StatSystem.class);
  }

  @PostConstruct
  public void init()
  {
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

  public void addMeter(AbstractMeter probe)
  {
    throw new AbstractMethodError();
  }

  public void addSample(Sample sample)
  {
    throw new AbstractMethodError();
  }

  public void addSample(long now, long id, double data)
  {
    throw new AbstractMethodError();
  }

  public void addSample(long now, long[] sampleIds, double[] sampleData)
  {
    throw new AbstractMethodError();
  }

  public double getCpuLoad()
  {
    throw new AbstractMethodError();
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
    throw new AbstractMethodError();
  }

  public void addMeterGraphPage(MeterGraphPageInfo meterGraphPage)
  {
    throw new AbstractMethodError();
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

  public ArrayList<AbstractMeter> getCpuMeters()
  {
    throw new AbstractMethodError();
  }

  protected static class Sample
  {
    private final long _id;
    private final String _name;
    private final AbstractMeter _probe;

    Sample(String name, AbstractMeter probe)
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
    
    final AbstractMeter getMeter()
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
}
