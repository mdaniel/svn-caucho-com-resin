/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import com.caucho.v5.health.meter.MeterBase;
import com.caucho.v5.jni.JniFilePathImpl;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.JniTroubleshoot;
import com.caucho.v5.util.JniUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.JniUtil.JniLoad;

/**
 * Statistics gathering attribute.  Each time period, the attribute is polled.
 */
public class JniCpuStat
{
  private static final L10N L = new L10N(JniCpuStat.class);

  // linux cpu data
  private static final int CPU_STAT_SIZE = 12;
  private static final int CPU_SUM = 0;
  private static final int CPU_USER = 1;
  private static final int CPU_NICE = 2;
  private static final int CPU_SYSTEM = 3;
  private static final int CPU_IDLE = 4;
  private static final int CPU_IOWAIT = 5;
  private static final int CPU_IRQ = 6;
  private static final int CPU_SOFTIRQ = 7;
  private static final int CPU_STEAL = 8;
  private static final int CPU_GUEST = 9;
  private static final int CPU_MISC = CPU_STAT_SIZE - 1;
  
  private static final JniTroubleshoot _jniTroubleshoot;

  private static JniCpuStat _stat;

  private int _jiffiesPerSecond;
  private int _cpuCount;
  
  private long []_prevCpuData;
  private long []_currentCpuData;
  
  private long []_contextSwitchData = new long[1];
  
  private long _prevContextSwitchCount;
  private long _contextSwitchCount;
  
  private long _samplePeriod = 60000L - 5000L;
  private long _sampleExpire = 0;

  private JniCpuStat()
  {
    _jiffiesPerSecond = nativeJiffiesPerSecond();
    _cpuCount = nativeCpuCount();

    if (_cpuCount >= 0) {
      _prevCpuData = new long[CPU_STAT_SIZE * (_cpuCount + 1)];
      _currentCpuData = new long[_prevCpuData.length];

      sample();

      System.arraycopy(_currentCpuData, 0, _prevCpuData, 0,
                       _prevCpuData.length);
    }
  }

  public static JniCpuStat create()
  {
    if (! isEnabled())
      return null;

    synchronized (JniCpuStat.class) {
      if (_stat == null)
        _stat = new JniCpuStat();
    
      return _stat;
    }
  }

  public static boolean isEnabled()
  {
    return _jniTroubleshoot.isEnabled();
  }

  public int getCpuMax()
  {
    return _cpuCount;
  }

  public void setSamplePeriod(long period)
  {
    // add gap because the alarm period isn't exact.  This gives a bit of
    // leeway for the timer
    if (period >= 15000) {
      _samplePeriod = period - 5000;
    }
    else if (period >= 2000) {
      _samplePeriod = period - 1000;
    }
    else {
      _samplePeriod = period;
    }
  }

  public MeterBase createActiveProbe(String name, int cpu, double scale)
  {
    return new CpuActiveProbe(name, cpu, scale);
  }

  public MeterBase createUserProbe(String name, int cpu, double scale)
  {
    return new CpuUserProbe(name, cpu, scale);
  }

  public MeterBase createSystemProbe(String name, int cpu, double scale)
  {
    return new CpuSystemProbe(name, cpu, scale);
  }

  public MeterBase createIdleProbe(String name, int cpu, double scale)
  {
    return new CpuIdleProbe(name, cpu, scale);
  }

  public MeterBase createContextSwitchProbe(String name)
  {
    return new ContextSwitchProbe(name);
  }
  
  public double sampleCpuActive(int cpu)
  {
    if (_cpuCount < cpu)
      return 0;

    sample();

    long []currentCpu = _currentCpuData;
    long []prevCpu = _prevCpuData;

    int offset = cpu * CPU_STAT_SIZE;

    long total = currentCpu[offset + CPU_SUM] - prevCpu[offset + CPU_SUM];
    long idle = currentCpu[offset + CPU_IDLE] - prevCpu[offset + CPU_IDLE];

    return (total - idle) / (double) total;
  }

  public double sampleCpuIdle(int cpu)
  {
    if (_cpuCount < cpu)
      return 0;

    sample();

    long []currentCpu = _currentCpuData;
    long []prevCpu = _prevCpuData;

    int offset = cpu * CPU_STAT_SIZE;

    long total = currentCpu[offset + CPU_SUM] - prevCpu[offset + CPU_SUM];
    long idle = currentCpu[offset + CPU_IDLE] - prevCpu[offset + CPU_IDLE];

    return idle / (double) total;
  }

  public double sampleCpuUser(int cpu)
  {
    if (_cpuCount < cpu)
      return 0;

    sample();

    long []currentCpu = _currentCpuData;
    long []prevCpu = _prevCpuData;

    int offset = cpu * CPU_STAT_SIZE;

    long total = currentCpu[offset + CPU_SUM] - prevCpu[offset + CPU_SUM];
    long user = 0;

    user += currentCpu[offset + CPU_USER] - prevCpu[offset + CPU_USER];
    user += currentCpu[offset + CPU_NICE] - prevCpu[offset + CPU_NICE];

    return user / (double) total;
  }

  public double sampleCpuSystem(int cpu)
  {
    if (_cpuCount < cpu)
      return 0;

    sample();

    long []currentCpu = _currentCpuData;
    long []prevCpu = _prevCpuData;

    int offset = cpu * CPU_STAT_SIZE;

    long total = currentCpu[offset + CPU_SUM] - prevCpu[offset + CPU_SUM];
    long idle = currentCpu[offset + CPU_IDLE] - prevCpu[offset + CPU_IDLE];
    long user = 0;

    user += currentCpu[offset + CPU_USER] - prevCpu[offset + CPU_USER];
    user += currentCpu[offset + CPU_NICE] - prevCpu[offset + CPU_NICE];

    return (total - idle - user) / (double) total;
  }
  
  public double sampleContextSwitch()
  {
    sample();

    return (_contextSwitchCount - _prevContextSwitchCount);
  }

  private synchronized void sample()
  {
    long now = CurrentTime.getCurrentTime();

    if (now < _sampleExpire || _currentCpuData == null)
      return;

    _sampleExpire = now + _samplePeriod;

    System.arraycopy(_currentCpuData, 0, _prevCpuData, 0, _prevCpuData.length);

    if (nativeCpuSample(_cpuCount, _currentCpuData, _contextSwitchData) < 0) {
      System.out.println("nativeCpuSample FAILURE:");
    }

    _prevContextSwitchCount = _contextSwitchCount;
    _contextSwitchCount = _contextSwitchData[0];
  }

  private native int nativeJiffiesPerSecond();
  private native int nativeCpuCount();
  private native int nativeCpuSample(int cpuMax,
                                     long []cpuValues,
                                     long []contextSwitchValue);

  public class CpuActiveProbe extends MeterBase {
    private final int _index;
    private final double _scale;

    private double _lastValue;
    
    private double _value;
    
    CpuActiveProbe(String name, int index, double scale)
    {
      super(name);

      _index = index;
      _scale = scale;
    }

    @Override
    public void sample()
    {
      _lastValue = sampleCpuActive(_index);
      
      _value = _scale * _lastValue;
    }
    
    @Override
    public double calculate()
    {
      return _value;
    }

    public double peek()
    {
      return _lastValue;
    }
  }

  public class CpuUserProbe extends MeterBase {
    private final int _index;
    private final double _scale;
    
    private double _value;
    
    CpuUserProbe(String name, int index, double scale)
    {
      super(name);

      _index = index;
      _scale = scale;
    }

    @Override
    public void sample()
    {
      _value = _scale * sampleCpuUser(_index);
    }
    
    @Override
    public double calculate()
    {
      return _value;
    }
  }

  public class CpuSystemProbe extends MeterBase {
    private final int _index;
    private final double _scale;
    
    private double _value;
    
    CpuSystemProbe(String name, int index, double scale)
    {
      super(name);

      _index = index;
      _scale = scale;
    }

    @Override
    public void sample()
    {
      _value = _scale * sampleCpuSystem(_index);
    }
    
    @Override
    public double calculate()
    {
      return _value;
    }
  }

  public class CpuIdleProbe extends MeterBase {
    private final int _index;
    private final double _scale;
    
    private double _value;
    
    CpuIdleProbe(String name, int index, double scale)
    {
      super(name);

      _index = index;
      _scale = scale;
    }

    @Override
    public void sample()
    {
      _value = _scale * sampleCpuIdle(_index);
    }
    
    @Override
    public double calculate()
    {
      return _value;
    }
  }

  public class ContextSwitchProbe extends MeterBase {
    private double _value;
    
    ContextSwitchProbe(String name)
    {
      super(name);
    }

    @Override
    public void sample()
    {
      _value = sampleContextSwitch();
    }

    @Override
    public double calculate()
    {
      return _value;
    }
  }

  static {
    _jniTroubleshoot
    = JniUtil.load(JniCpuStat.class,
                   new JniLoad() { 
                     public void load(String path) { System.load(path); }},
                   "baratine");
  }
}
