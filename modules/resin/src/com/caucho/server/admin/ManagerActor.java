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
 * @author Alex Rojkov
 */

package com.caucho.server.admin;

import com.caucho.bam.Query;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.config.ConfigException;
import com.caucho.jmx.Jmx;
import com.caucho.profile.HeapDump;
import com.caucho.profile.Profile;
import com.caucho.profile.ProfileEntry;
import com.caucho.profile.StackEntry;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.MemoryPoolAdapter;
import com.caucho.util.ThreadDump;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManagerActor extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(ManagerActor.class.getName());

  private static final L10N L = new L10N(ManagerActor.class);
  private static ClassLoader _systemClassLoader;

  private Server _server;
  private MBeanServer _mBeanServer;
  private File _hprofDir;
  private Map<String, Level> _defaultLevels = new HashMap<String, Level>();

  private AtomicBoolean _isInit = new AtomicBoolean();

  public ManagerActor()
  {
    super("manager@resin.caucho", BamSystem.getCurrentBroker());
  }

  @PostConstruct
  public void init()
  {
    if (_isInit.getAndSet(true))
      return;

    _server = Server.getCurrent();

    if (_server == null)
      throw new ConfigException(L.l(
        "resin:ManagerService requires an active Server.\n  {0}",
        Thread.currentThread().getContextClassLoader()));

    setBroker(getBroker());
    MultiworkerMailbox mailbox
      = new MultiworkerMailbox(getActor().getAddress(),
                               getActor(), getBroker(), 2);

    getBroker().addMailbox(mailbox);

    _mBeanServer = Jmx.getGlobalMBeanServer();
  }

  public File getHprofDir()
  {
    return _hprofDir;
  }

  public void setHprofDir(String hprofDir)
  {
    if (hprofDir.isEmpty())
      throw new ConfigException("hprof-dir can not be set to an emtpy string");

    File file = new File(hprofDir);

    if (! file.isAbsolute())
      throw new ConfigException("hprof-dir must be an absolute path");

    _hprofDir = file;
  }

  @Query
  public String doThreadDump(long id,
                             String to,
                             String from,
                             ThreadDumpQuery query)
  {
    String dump = ThreadDump.getThreadDump();

    getBroker().queryResult(id, from, to, dump);

    return dump;
  }

  @Query
  public String doHeapDump(long id, String to, String from, HeapDumpQuery query)
  {
    String result = null;
    if (query.isRaw()) {
      result = doRawHeapDump();
    }
    else {
      result = getProHeapDump();
    }

    getBroker().queryResult(id, from, to, result);

    return result;
  }

  private String doRawHeapDump()
  {
    try {
      ObjectName name = new ObjectName(
        "com.sun.management:type=HotSpotDiagnostic");

      final String base = "hprof-" + _server.getServerId();
      final Calendar date = new GregorianCalendar();
      date.setTimeInMillis(Alarm.getCurrentTime());
      DecimalFormat f = new DecimalFormat("00");
      String suffix = f.format(date.get(Calendar.YEAR)) + "-" +
                      f.format(date.get(Calendar.MONTH)) + "-" +
                      f.format(date.get(Calendar.DAY_OF_MONTH)) + "-" +
                      f.format(date.get(Calendar.HOUR_OF_DAY)) + "-" +
                      f.format(date.get(Calendar.MINUTE)) + "-" +
                      f.format(date.get(Calendar.SECOND));

      File hprofDir = _hprofDir;

      if (hprofDir == null)
        hprofDir = new File(System.getProperty("java.io.tmpdir"));

      final String fileName = base + "-" + suffix + ".hprof";

      MemoryPoolAdapter memoryAdapter  = new MemoryPoolAdapter();
      if (memoryAdapter.getEdenUsed() > hprofDir.getFreeSpace())
        return L.l("Not enough disk space for `{0}'", fileName);

      File file = new File(hprofDir, fileName);
      if (file.exists())
        return L.l("File `{0}' exists.", file);

      _mBeanServer.invoke(name,
                          "dumpHeap",
                          new Object[]{file.getCanonicalPath(), Boolean.TRUE},
                          new String[]{String.class.getName(), boolean.class.getName()});

      final String result = L.l("Heap dump is written to `{0}'.\n"
                      + "To view the file on the target machine use\n"
                      + "jvisualvm --openfile {0}", file);

      return result;
    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);

      return e.getMessage();
    }
  }

  @Query
  public String setLogLevel(long id,
                            String to,
                            String from,
                            LogLevelQuery query)
  {
    final String logger = query.getLogger();
    final Level newLevel = query.getLevel();
    final long time = query.getPeriod();
    String result = null;
    try {
      final Level oldLevel = getLoggerLevel(logger);

      AlarmListener listener = new AlarmListener()
      {
        @Override
        public void handleAlarm(Alarm alarm)
        {
          setLoggerLevel(logger, oldLevel);
        }
      };

      new Alarm("log-level", listener, time);

      setLoggerLevel(logger, newLevel);

      result = L.l("Log {0}.level is set to `{1}'. Active time {2} seconds.",
                   (logger.isEmpty() ? "{root}" : logger),
                   newLevel,
                   (time / 1000));
    } catch (Exception e) {
      log.log(Level.INFO, e.getMessage(), e);

      result = e.getMessage();
    }

    getBroker().queryResult(id, from, to, result);

    return result;
  }

  @Query
  public String profile(long id, String to, String from, ProfileQuery query)
  {
    Profile profile = Profile.createProfile();

    if (profile.isActive()) {
      return "Profile is still active";
    }

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    Calendar startedAt = new GregorianCalendar();
    startedAt.setTimeInMillis(Alarm.getCurrentTime());

    profile.setPeriod(query.getPeriod());
    profile.setDepth(query.getDepth());
    final long activeTime = query.getActiveTime();
    final long period = query.getPeriod();

    profile.start();

    Calendar interruptedAt = null;

    Object lock = new Object();
    synchronized (lock) {
      try {
        lock.wait(activeTime);
      } catch (InterruptedException e) {
        interruptedAt = new GregorianCalendar();
      }
    }

    profile.stop();

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer);

    ProfileEntry[] entries = profile.getResults();

    if (entries == null || entries.length == 0) {
      out.println("Profile returned no entries.");
    }
    else {
      if (interruptedAt == null) {
        out.print(L.l("Profile started at {0}. Active for a total of {1}ms.",
                      dateFormat.format(startedAt.getTime()),
                      activeTime));
      }
      else {
        out.print(L.l("Profile started at {0}, interruped at {1}.",
                      dateFormat.format(startedAt.getTime()),
                      dateFormat.format(interruptedAt.getTime())));
      }

      out.println(L.l(" Sampling rate {0}ms. Depth {1}.",
                      period,
                      String.valueOf(query.getDepth())));

      double totalTicks = 0;
      for (ProfileEntry entry : entries) {
        totalTicks += entry.getCount();
      }

      final double sampleTicks = profile.getTicks();
      double totalPercent = 0d;

      out.println("   % time  |time self(s)|   % sum    | Method Call");

      for (ProfileEntry entry : entries) {
        double timePercent = (double)100 * (double) entry.getCount()
                             / sampleTicks;
        double selfPercent = (double) 100 * (double) entry.getCount()
                             / totalTicks;
        totalPercent += selfPercent;

        out.println(String.format("%10.3f | %10.3f | %10.3f | %s",
                                  timePercent,
                                  (float)entry.getCount() * period * 0.001,
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
    String result = buffer.toString();

    getBroker().queryResult(id, from, to, result);

    return result;
  }

  private void setLoggerLevel(final String name, final Level level)
  {
    final Logger logger = Logger.getLogger(name);
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      thread.setContextClassLoader(_systemClassLoader);
      logger.setLevel(level);
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  private synchronized Level getLoggerLevel(String name) {
    Level level = _defaultLevels.get(name);
    if (level != null)
      return level;

    final Logger logger = Logger.getLogger(name);
    final Thread thread = Thread.currentThread();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      thread.setContextClassLoader(_systemClassLoader);

      level = logger.getLevel();
      _defaultLevels.put(name, level);

      return level;
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  private String getProHeapDump()
  {
    try {
      HeapDump dump = HeapDump.create();
      StringWriter buffer = new StringWriter();
      PrintWriter writer = new PrintWriter(buffer);
      dump.writeExtendedHeapDump(writer);
      writer.flush();

      return buffer.toString();
    } catch (ConfigException e) {
      log.log(Level.FINE, e.getMessage(), e);

      return e.getMessage();
    } catch (IOException e) {
      log.log(Level.FINE, e.getMessage(), e);

      return e.getMessage();
    }
  }

  static {
    _systemClassLoader = ClassLoader.getSystemClassLoader();
  }
}
