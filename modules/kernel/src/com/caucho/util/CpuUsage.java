/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CpuUsage {
  static Logger log = Log.open(CpuUsage.class);
  
  private static CauchoNative jni;
  private static boolean triedLoading;
    
  private int pid;
  
  private long time;
  private long interval;

  private long userTime;
  private long systemTime;    

  private CpuUsage()
  {
  }

  public static synchronized CpuUsage create()
  {
    if (jni == null && ! triedLoading) {
      triedLoading = true;
      try {
	jni = CauchoNative.create();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    CpuUsage usage = new CpuUsage();

    if (jni == null)
      return usage;

    usage.update(Alarm.getCurrentTime());
    
    return usage;
  }

  public void update(long now)
  {
    time = now;
    
    if (jni == null)
      return;

    synchronized (jni) {
      jni.calculateUsage();
      pid = jni.getPid();
      userTime = (long) (jni.getUserTime() * 1000);
      systemTime = (long) (jni.getSystemTime() * 1000);
    }
  }

  public void copy(CpuUsage source)
  {
    pid = source.pid;
    time = source.time;
    
    userTime = source.userTime;
    systemTime = source.systemTime;
  }

  public void clear()
  {
    pid = -1;
    time = 0;
    interval = 0;
    userTime = 0;
    systemTime = 0;
  }

  public void add(CpuUsage base, CpuUsage source)
  {
    if (pid == -1) {
      pid = source.getPid();
      time = source.getTime();
      interval = time - base.getTime();
    }
    else if (pid == source.getPid())
      return;

    userTime += source.getUserTime() - base.getUserTime();
    systemTime += source.getSystemTime() - base.getSystemTime();
  }

  public int getPid()
  {
    return pid;
  }

  public long getTime()
  {
    return time;
  }

  public long getInterval()
  {
    return interval;
  }

  public long getUserTime()
  {
    return userTime;
  }

  public long getSystemTime()
  {
    return systemTime;
  }

  boolean setUser(String user, String group)
    throws Exception
  {
    if (jni == null)
      return false;
    
    else if (user != null) {
      synchronized (jni) {
        return jni.setUser(user, group);
      }
    }
    else
      return false;
  }
}


