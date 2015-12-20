/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import com.caucho.v5.env.meter.MeterBase;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.JniTroubleshoot;
import com.caucho.v5.util.JniUtil;
import com.caucho.v5.util.JniUtil.JniLoad;

/**
 * Statistics gathering attribute.  Each time period, the attribute is polled.
 */
public class JniVmStat
{
  // linux vm data
  private static final int VM_STAT_SIZE = 2;
  
  public static final int VM_SIZE = 0;
  public static final int VM_RSS = 1;
  
  private static final JniTroubleshoot _jniTroubleshoot;

  private static JniVmStat _stat;
  
  private long []_vmData;
  
  private long _samplePeriod = 60000L - 5000L;
  private long _sampleExpire = 0;

  private JniVmStat()
  {
    _vmData = new long[VM_STAT_SIZE];
  }

  public static JniVmStat create()
  {
    if (! isEnabled())
      return null;

    if (! nativeIsVmActive())
      return null;

    synchronized (JniVmStat.class) {
      if (_stat == null)
        _stat = new JniVmStat();
    
      return _stat;
    }
  }

  public static boolean isEnabled()
  {
    return _jniTroubleshoot.isEnabled();
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

  public MeterBase createActiveMeter(String name, int type)
  {
    return new VmProbe(name, type);
  }

  public double sampleVm(int type)
  {
    if (type < 0 || VM_STAT_SIZE <= type)
      return 0;

    sample();

    return _vmData[type];
  }

  private synchronized void sample()
  {
    long now = CurrentTime.getCurrentTime();

    if (now < _sampleExpire)
      return;

    _sampleExpire = now + _samplePeriod;

    if (nativeVmSample(_vmData) < 0) {
      System.out.println("nativeVmSample FAILURE:");
    }
  }

  private static native boolean nativeIsVmActive();
  private native int nativeVmSample(long []vmValues);

  public class VmProbe extends MeterBase {
    private final int _index;

    private double _value;

    VmProbe(String name, int index)
    {
      super(name);

      _index = index;
    }

    @Override
    public void sample()
    {
      _value = sampleVm(_index);
    }

    @Override
    public double calculate()
    {
      return _value;
    }
  }

  static {
    _jniTroubleshoot
    = JniUtil.load(JniVmStat.class,
                   new JniLoad() { 
                     public void load(String path) { System.load(path); }},
                   "baratine");
  }
}
