/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.util;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.jni.JniCauchoSystem;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.JniTroubleshoot;
import com.caucho.v5.util.JniUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper for Caucho system variables, allowing tests to override
 * the default variables.
 */
public class JniCauchoSystemImpl extends JniCauchoSystem {
  private static final Logger log
    = Logger.getLogger(JniCauchoSystemImpl.class.getName());

  private static long _lastLoadAvgTime;
  private static double _lastLoadAvgValue = -1.0;

  protected JniCauchoSystemImpl()
  {
    // _jniTroubleshoot.checkIsValid();
  }

  @Override
  public void initJniBackground()
  {
    /*
    if (isEnabled())
      nativeInitBackground();
      */
  }

  public boolean isEnabled()
  {
    return false; // _jniTroubleshoot.isEnabled();
  }
    
  /**
   * Returns true if we're currently running a test.
   */
  @Override
  public double getLoadAvg()
  {
    long now = CurrentTime.getCurrentTime();
    
    if (_lastLoadAvgTime != now && isEnabled()) {
      // _lastLoadAvgValue = nativeGetLoadAvg();
      _lastLoadAvgTime = now;
    }

    return _lastLoadAvgValue;
  }
}
