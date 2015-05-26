/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.admin.action.*;
import com.caucho.env.log.LogSystem;
import com.caucho.health.action.*;
import com.caucho.jmx.server.ManagedObjectBase;
import com.caucho.management.server.SnapshotServiceMXBean;
import com.caucho.util.L10N;

/**
 * service for managing snapshots.
 */
public class SnapshotServiceAdmin extends ManagedObjectBase
  implements SnapshotServiceMXBean
{
  private static final L10N L = new L10N(SnapshotServiceAdmin.class);
  private static final Logger log
    = Logger.getLogger(SnapshotServiceAdmin.class.getName());
  
  public static final String LOG_SNAPSHOT_BEGIN = "Resin|SnapshotBegin";
  public static final String LOG_SNAPSHOT_END = "Resin|SnapshotEnd";
  
  private ProfileAction _action = new ProfileAction();
  
  @Override
  public String getName()
  {
    return null;
  }
  
  void register()
  {
    registerSelf();
  }
  
  @Override
  public void snapshotJmx()
  {
    try {
      JmxDumpAction action = new JmxDumpAction();
      String jmxDump = action.execute();
      saveSnapshot(DumpJmx.LOG_TYPE, jmxDump);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void snapshotHeap()
  {
    try {
      HeapDumpAction action = new HeapDumpAction();
      String heapDump = action.executeJson();
      saveSnapshot(DumpHeap.LOG_TYPE, heapDump);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void snapshotThreadDump()
  {
    try {
      ThreadDumpAction action = new ThreadDumpAction();
      String threadDump = action.executeJson();
      saveSnapshot(DumpThreads.LOG_TYPE, threadDump);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void snapshotHealth()
  {
    try {
      HealthDumpAction action = new HealthDumpAction();
      String healthJson = action.executeJson();
      saveSnapshot(DumpHealth.LOG_TYPE, healthJson);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void snapshotScoreboards()
  {
    try {
      ScoreboardAction action = new ScoreboardAction();
      String json = action.executeJson("resin", true);
      saveSnapshot(ReportScoreboard.createLogType("resin"), json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void startProfile(long samplingRate, int stackDepth)
  {
    _action.start(samplingRate, stackDepth);
  }

  @Override
  public void stopProfile()
  {
    _action.cancel();
  }
 
  @Override
  public void snapshotProfile(long period, long samplingRate, int stackDepth)
  {
    _action.execute(period, samplingRate, stackDepth);
    snapshotProfile();
  }

  @Override
  public void snapshotProfile()
  {
    String profileDump = _action.jsonProfile();
    saveSnapshot(StartProfiler.LOG_TYPE, profileDump);
  }

  private void saveSnapshot(String type, String value)
  {
    LogSystem logSystem = LogSystem.getCurrent();

    if (logSystem != null) {
      String logType = logSystem.createFullType(type);
      logSystem.log(logType, value);
      
      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("Snapshot {0}:\n{1}", logType,value));
    }
  }
}
