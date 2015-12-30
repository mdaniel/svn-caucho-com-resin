/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import com.caucho.v5.health.meter.MeterBase;
import com.caucho.v5.jni.JniFilePathImpl;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.JniTroubleshoot;
import com.caucho.v5.util.JniUtil;
import com.caucho.v5.util.JniUtil.JniLoad;

/**
 * Statistics gathering attribute.  Each time period, the attribute is polled.
 *
 * Samples the network every _samplePeriod seconds. Because the netstat
 * sample can be slow, it has its own thread independent of the standard
 * stat sample.
 */
public class JniNetStat implements AlarmListener
{
  // linux cpu data
  private static final int NET_STAT_SIZE = 13;

  public static final int TCP_ESTABLISHED = 1;
  public static final int TCP_SYN_SENT = 2;
  public static final int TCP_SYN_RECV = 3;
  public static final int TCP_FIN_WAIT1 = 4;
  public static final int TCP_FIN_WAIT2 = 5;
  public static final int TCP_TIME_WAIT = 6;
  public static final int TCP_CLOSE = 7;
  public static final int TCP_CLOSE_WAIT = 8;
  public static final int TCP_LAST_ACK = 9;
  public static final int TCP_LISTEN = 10;
  public static final int TCP_CLOSING = 12;

  private static final JniTroubleshoot _jniTroubleshoot;

  private static JniNetStat _stat;

  private long []_netstatData;

  private long _samplePeriod = 60000L;
  
  private Alarm _alarm;

  private JniNetStat()
  {
    _netstatData = new long[NET_STAT_SIZE];

    _alarm = new Alarm(this);
    _alarm.queue(0);
  }

  public static JniNetStat create()
  {
    if (! isEnabled())
      return null;

    if (! nativeIsNetActive())
      return null;

    synchronized (JniNetStat.class) {
      if (_stat == null)
        _stat = new JniNetStat();

      return _stat;
    }
  }

  public static boolean isEnabled()
  {
    return _jniTroubleshoot.isEnabled();
  }

  public void setSamplePeriod(long period)
  {
    /*
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
    */
  }

  public MeterBase createActiveProbe(String name, int type)
  {
    return new NetProbe(name, type);
  }

  public double sampleNet(int type)
  {
    if (type < 0 || NET_STAT_SIZE <= type)
      return 0;

    return _netstatData[type];
  }

  public void handleAlarm(Alarm alarm)
  {
    try {
      if (nativeNetSample(_netstatData) < 0) {
        System.out.println("nativeNetSample FAILURE:");
      }
    } finally {
      alarm.queue(_samplePeriod);
    }
  }

  private static native boolean nativeIsNetActive();
  private native int nativeNetSample(long []netValues);

  public class NetProbe extends MeterBase {
    private final int _index;

    private double _value;

    NetProbe(String name, int index)
    {
      super(name);

      _index = index;
    }

    @Override
    public void sample()
    {
      _value = sampleNet(_index);
    }

    @Override
    public double calculate()
    {
      return _value;
    }
  }

  static {
    _jniTroubleshoot
    = JniUtil.load(JniNetStat.class,
                   new JniLoad() { 
                     public void load(String path) { System.load(path); }},
                   "baratine");
  }
}
