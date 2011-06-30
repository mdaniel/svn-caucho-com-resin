/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.admin;

import com.caucho.config.types.Period;
import com.caucho.env.meter.AbstractMeter;
import com.caucho.env.service.ResinSystem;
import com.caucho.management.server.BaselineQueryResult;
import com.caucho.management.server.MeterGraphInfo;
import com.caucho.management.server.MeterGraphPageInfo;
import com.caucho.management.server.StatServiceValue;
import com.caucho.server.admin.StatSystem;
import com.caucho.server.resin.Resin;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Set;

@Startup
@Singleton
public class StatService extends StatSystem
{
  public StatService()
  {
    Resin.getCurrent().createStatSystem();
  }

  public static StatSystem getCurrent()
  {
    StatSystem statSystem = ResinSystem.getCurrentService(StatSystem.class);

    return statSystem;
  }

  @PostConstruct
  public void init()
  {
    ResinSystem.getCurrentService(StatSystem.class).init();
  }

  @Override
  public ArrayList<AbstractMeter> getCpuMeters()
  {
    return StatSystem.getCurrent().getCpuMeters();
  }

  @Override
  public void setSamplePeriod(Period period)
  {
    StatSystem.getCurrent().setSamplePeriod(period);
  }

  @Override
  public long getSamplePeriod()
  {
    return StatSystem.getCurrent().getSamplePeriod();
  }

  @Override
  public Set queryNames(String objectName)
  {
    return StatSystem.getCurrent().queryNames(objectName);
  }

  @Override
  public void addJmxMeter(String name, String objectName, String attribute)
  {
    StatSystem.getCurrent().addJmxMeter(name, objectName, attribute);
  }

  @Override
  public void addJmx(JmxItem item)
  {
    StatSystem.getCurrent().addJmx(item);
  }

  @Override
  public void addJmxDelta(JmxItem item)
  {
    StatSystem.getCurrent().addJmxDelta(item);
  }

  @Override
  public void addJmxDeltaMeter(String name, String objectName, String attribute)
  {
    StatSystem.getCurrent().addJmxDeltaMeter(name, objectName, attribute);
  }

  @Override
  public void addJmxPercentMeter(String name,
                                           String objectName,
                                           String attribute)
  {
    StatSystem.getCurrent().addJmxPercentMeter(name, objectName, attribute);
  }

  @Override
  public void addMeter(AbstractMeter probe)
  {
    StatSystem.getCurrent().addMeter(probe);
  }

  @Override
  public void addSample(StatSystem.Sample sample)
  {
    StatSystem.getCurrent().addSample(sample);
  }

  @Override
  public void addSample(long now, long id, double data)
  {
    StatSystem.getCurrent().addSample(now, id, data);
  }

  @Override
  public void addSample(long now, long[] sampleIds, double[] sampleData)
  {
    StatSystem.getCurrent().addSample(now, sampleIds, sampleData);
  }

  @Override
  public double getCpuLoad()
  {
    return StatSystem.getCurrent().getCpuLoad();
  }

  @Override
  public StatServiceValue[] getStatisticsData(String name,
                                                        long beginTime,
                                                        long endTime,
                                                        long step)
  {
    return StatSystem.getCurrent().getStatisticsData(name, beginTime, endTime, step);
  }

  @Override
  public StatServiceValue[] getStatisticsData(long id,
                                                        long beginTime,
                                                        long endTime,
                                                        long step)
  {
    return StatSystem.getCurrent().getStatisticsData(id, beginTime, endTime, step);
  }

  @Override
  public double getLastValue(String name)
  {
    return StatSystem.getCurrent().getLastValue(name);
  }

  @Override
  public double getLastValue(long id)
  {
    return StatSystem.getCurrent().getLastValue(id);
  }

  @Override
  public BaselineQueryResult getBaseline(String name,
                                                   long beginTime,
                                                   long endTime,
                                                   int minSampleSize)
  {
    return StatSystem.getCurrent().getBaseline(name, beginTime, endTime, minSampleSize);
  }

  @Override
  public BaselineQueryResult getBaseline(long id,
                                                   long beginTime,
                                                   long endTime,
                                                   int minSampleSize)
  {
    return StatSystem.getCurrent().getBaseline(id, beginTime, endTime, minSampleSize);
  }

  @Override
  public String[] getStatisticsNames()
  {
    return StatSystem.getCurrent().getStatisticsNames();
  }

  @Override
  public void addMeterGraph(MeterGraphInfo meterGraph)
  {
    StatSystem.getCurrent().addMeterGraph(meterGraph);
  }

  @Override
  public void addMeterGraphPage(MeterGraphPageInfo meterGraphPage)
  {
    StatSystem.getCurrent().addMeterGraphPage(meterGraphPage);
  }

  @Override
  public MeterGraphInfo[] getMeterGraphs()
  {
    return StatSystem.getCurrent().getMeterGraphs();
  }

  @Override
  public MeterGraphPageInfo[] getMeterGraphPages()
  {
    return StatSystem.getCurrent().getMeterGraphPages();
  }

  @Override
  public long[] getStartTimes(int index, long startTime, long endTime)
  {
    return StatSystem.getCurrent().getStartTimes(index, startTime, endTime);
  }
}
