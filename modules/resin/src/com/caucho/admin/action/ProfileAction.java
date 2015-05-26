/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 */

package com.caucho.admin.action;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.profile.*;
import com.caucho.util.*;

public class ProfileAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(ProfileAction.class.getName());

  private static final L10N L = new L10N(ProfileAction.class);
  
  private AtomicLong _cancelledTime = new AtomicLong(-1);
  
  public void cancel()
  {
    _cancelledTime.compareAndSet(-1, CurrentTime.getCurrentTime());
    
    synchronized(this) {
      this.notify();
    }

    Profile profile = Profile.create();
    
    if (profile != null)
      profile.stop();
  }
  
  public void start(long samplingRate, int depth)
  {
    Profile profile = Profile.create();

    profile.stop();

    profile.setPeriod(samplingRate);
    profile.setDepth(depth);

    profile.start();
  }
  
  public String execute(long activeTime, long samplingRate, int depth)
    throws ConfigException
  {
    if (activeTime <= 0) {
      throw new IllegalArgumentException(L.l("Profile activeTime '{0}': must be > 0.",
                                             activeTime));
    }
    
    Profile profile = Profile.create();

    if (profile.isActive()) {
      throw new ConfigException(L.l("Profile is still active"));
    }
    
    long startedAt = CurrentTime.getCurrentTime();
    
    start(samplingRate, depth);
    
    try {
      synchronized (this) {
        this.wait(activeTime);
      }
    } catch (InterruptedException e) {
      _cancelledTime.compareAndSet(-1, CurrentTime.getCurrentTime());
    }

    ProfileReport report = profile.stop();

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer);

    if (report == null) {
      out.println("Profile returned no entries.");
    }
    else {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      
      long cancelledTime = _cancelledTime.get();

      if (cancelledTime < 0) {
        out.print(L.l("Profile started at {0}. Active for a total of {1}ms.",
                      dateFormat.format(new Date(startedAt)),
                      activeTime));
      }
      else {
        
        long et = cancelledTime - startedAt;
        
        out.print(L.l("Profile started at {0}, cancelled at {1}. Active for a total of {2}ms.",
                      dateFormat.format(new Date(startedAt)),
                      dateFormat.format(new Date(cancelledTime)),
                      et));
      }

      out.println(L.l(" Sampling rate {0}ms. Depth {1}.",
                      samplingRate,
                      String.valueOf(depth)));
      
      ProfileEntry[] entries = report.getEntries();

      double totalTicks = 0;
      for (ProfileEntry entry : entries) {
        totalTicks += entry.getCount();
      }

      final double sampleTicks = report.getTicks();
      double totalPercent = 0d;

      out.println(" ref# |   % time   |time self(s)|   % sum    | Method Call");
      for (int i = 0; i < entries.length; i++) {
        ProfileEntry entry = entries[i];
        double timePercent = (double) 100 * (double) entry.getCount()
                             / sampleTicks;
        double selfPercent = (double) 100 * (double) entry.getCount()
                             / totalTicks;
        totalPercent += selfPercent;

        out.println(String.format(" %4d | %10.3f | %10.3f | %10.3f | %s",
                                  i,
                                  timePercent,
                                  (float) entry.getCount() * samplingRate * 0.001,
                                  totalPercent,
                                  entry.getDescription()));

      }

      for (int i = 0; i < entries.length; i++) {
        ProfileEntry entry = entries[i];
        out.print(String.format(" %4d ", i));
        out.println(" " + entry.getDescription());
        ArrayList<? extends StackEntry> stackEntries = entry.getStackTrace();
        for (StackEntry stackEntry : stackEntries) {
          out.println("         " + stackEntry.getDescription());
        }
      }
    }

    out.flush();

    return buffer.toString();
  }

  /**
   * @return
   */
  public String jsonProfile()
  {
    Profile profile = Profile.create();
    
    if (profile == null) {
      return null;
    }
    
    ProfileReport report = profile.report();
      
    if (report == null) {
      return null;
    }
    
    ProfileEntry []entries = report.getEntries();

    if (entries == null || entries.length == 0) {
      return null;
    }
    
    StringBuilder sb = new StringBuilder();
    
    long timestamp = CurrentTime.getCurrentTime();
    
    sb.append("{\n");
    sb.append("  \"create_time\": \"" + new Date(timestamp) + "\"");
    sb.append(",\n  \"timestamp\": " + timestamp);
    sb.append(",\n  \"ticks\" : " + report.getTicks());
    sb.append(",\n  \"depth\" : " + report.getDepth());
    sb.append(",\n  \"period\" : " + report.getPeriod());
    sb.append(",\n  \"end_time\" : " + report.getEndTime());
    sb.append(",\n  \"gc_time\" : " + report.getGcTime());
    sb.append(",\n  \"profile\" :  [\n");
    
    for (int i = 0; i < entries.length; i++) {
      if (i != 0)
        sb.append(",\n");
     
      jsonEntry(sb, entries[i]);
    }
    
    /*
    long gcTicks = (profile.getGcTime() + profile.getPeriod() - 1)
      / profile.getPeriod();
    
    
    if (entries.length > 0)
      sb.append(",\n");
    
    jsonGc(sb, gcTicks);
    */
    
    sb.append("\n  ]");
    sb.append("\n}");
 
    return sb.toString();
  }
  
  private void jsonEntry(StringBuilder sb, ProfileEntry entry)
  {
    sb.append("{");
    sb.append("\n  \"name\" : \"");
    escapeString(sb, entry.getDescription());
    sb.append("\"");
    
    sb.append(",\n  \"ticks\" : " + entry.getCount());
    sb.append(",\n  \"state\" : \"" + entry.getState() + "\"");
    
    if (entry.getStackTrace() != null && entry.getStackTrace().size() > 0) {
      jsonStackTrace(sb, entry.getStackTrace());
    }

    sb.append("\n}");
  }
  
  private void jsonGc(StringBuilder sb, long ticks)
  {
    sb.append("{");
    sb.append("\n  \"name\" : \"HeapMemory.gc\"");
    
    sb.append(",\n  \"ticks\" : " + ticks);
    sb.append(",\n  \"state\" : \"RUNNABLE\"");

    sb.append("\n}");
  }
  
  private void jsonStackTrace(StringBuilder sb, 
                              ArrayList<? extends StackEntry> stack)
  {
    sb.append(",\n  \"stack\" : ");
    sb.append("[\n");
    
    int size = stack.size();
    
    for (int i = 0; i < size; i++) {
      StackEntry entry = stack.get(i);
      
      if (i != 0)
        sb.append(",\n");
      
      sb.append("  {");
      
      sb.append("\n    \"class\" : \"" + entry.getClassName() + "\"");
      sb.append(",\n    \"method\" : \"" + entry.getMethodName() + "\"");
      
      if (entry.getArg() != null && ! "".equals(entry.getArg())) {
        sb.append(",\n    \"arg\" : \"");
        escapeString(sb, entry.getArg());
        sb.append("\"");
        
      }
      sb.append("\n  }");
    }
    sb.append("\n  ]");
  }
  
  private void escapeString(StringBuilder sb, String value)
  {
    if (value == null)
      return;
    
    int len = value.length();
    
    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);
      
      switch (ch) {
      case '"':
        sb.append("\\\"");
        break;
      case '\\':
        sb.append("\\\\");
        break;
      default:
        sb.append(ch);
        break;
      }
    }
  }
}
