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
 */

package com.caucho.admin.action;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.profile.*;
import com.caucho.util.*;

public class ProfileAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(ProfileAction.class.getName());

  private static final L10N L = new L10N(ProfileAction.class);
  
  public String execute(long activeTime, long period, int depth)
    throws ConfigException
  {
    Profile profile = Profile.createProfile();

    if (profile.isActive()) {
      throw new ConfigException(L.l("Profile is still active"));
    }

    profile.setPeriod(period);
    profile.setDepth(depth);

    long startedAt = Alarm.getCurrentTime();

    profile.start();

    long interruptedAt = -1;

    try {
      Thread.sleep(activeTime);
    } catch (InterruptedException e) {
      interruptedAt = Alarm.getCurrentTime();
    }

    profile.stop();

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer);

    ProfileEntry []entries = profile.getResults();

    if (entries == null || entries.length == 0) {
      out.println("Profile returned no entries.");
    }
    else {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

      if (interruptedAt < 0) {
        out.print(L.l("Profile started at {0}. Active for a total of {1}ms.",
                      dateFormat.format(new Date(startedAt)),
                      activeTime));
      }
      else {
        
        long et = interruptedAt - startedAt;
        
        out.print(L.l("Profile started at {0}, interrupted at {1}. Active for a total of {2}ms.",
                      dateFormat.format(new Date(startedAt)),
                      dateFormat.format(new Date(interruptedAt)),
                      et));
      }

      out.println(L.l(" Sampling rate {0}ms. Depth {1}.",
                      period,
                      String.valueOf(depth)));

      double totalTicks = 0;
      for (ProfileEntry entry : entries) {
        totalTicks += entry.getCount();
      }

      final double sampleTicks = profile.getTicks();
      double totalPercent = 0d;

      out.println("   % time  |time self(s)|   % sum    | Method Call");

      for (ProfileEntry entry : entries) {
        double timePercent = (double) 100 * (double) entry.getCount()
                             / sampleTicks;
        double selfPercent = (double) 100 * (double) entry.getCount()
                             / totalTicks;
        totalPercent += selfPercent;

        out.println(String.format("%10.3f | %10.3f | %10.3f | %s",
                                  timePercent,
                                  (float) entry.getCount() * period * 0.001,
                                  totalPercent,
                                  entry.getDescription()));

      }

      for (ProfileEntry entry : entries) {
        out.println(entry.getDescription());
        ArrayList<? extends StackEntry> stackEntries = entry.getStackTrace();
        for (StackEntry stackEntry : stackEntries) {
          out.println("  " + stackEntry.getDescription());
        }
      }
    }

    out.flush();

    return buffer.toString();
  }
}
