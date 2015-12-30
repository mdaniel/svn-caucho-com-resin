/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import java.util.ArrayList;

import com.caucho.v5.health.meter.Meter;
import com.caucho.v5.health.meter.MeterBase;
import com.caucho.v5.health.stat.StatSystem.Sample;
import com.caucho.v5.jmx.server.MeterGraphInfo;
import com.caucho.v5.jmx.server.MeterGraphPageInfo;


/**
 * statistics
 */
public interface StatServiceLocal
{
  void start();

  void addMeter(MeterBase probe);

  Meter getMeter(String name);

  void addSample(Sample sample);

  void addSampleValue(long now, long id, double data);

  void addSampleValues(long now, long[] sampleIds, double[] sampleData);

  void addMeterGraph(MeterGraphInfo meterGraph);

  void addMeterGraphPage(MeterGraphPageInfo meterGraphPage);

  ArrayList<MeterBase> getCpuMeters();
}
