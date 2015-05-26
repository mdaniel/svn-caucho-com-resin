/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.health.action;

import io.baratine.core.Startup;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.config.Configurable;
import com.caucho.config.types.Period;

/**
 * A specific sequence of health actions: thread dump, heap dump, jmx dump, 
 * and pdf report.  This is intended to generate a permanent representation, or 
 * "snapshot" of the system at a point in time that includes all the 
 * information necessary to debug server issues.  It is usually intended to run 
 * in response to a unexpected issue.
 * 
 * <p>
 * <pre>{@code
 * <health:Snapshot>
 *   <health:OnAbnormalStop/>
 * </health:Snapshot 
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class Snapshot extends ActionSequence
{
  private final DumpThreads _dumpThreads = new DumpThreads();
  private final DumpHeap _dumpHeap = new DumpHeap();
  private final DumpJmx _dumpJmx = new DumpJmx();
  //private final PdfReport _pdfReport = new PdfReport();
  private final ReportScoreboard _threadScoreboard = new ReportScoreboard();
  
  private boolean _isThreadDump = true;
  private boolean _isHeapDump = true;
  private boolean _isJmxDump = true;
  private boolean _isPdfReport = true;
  private boolean _isThreadScoreboard = true;
  
  private boolean _isLog = false;

  /**
   * Enable a thread dump for the snapshot.
   */
  @Configurable
  public void setThreadDump(boolean isEnable)
  {
    _isThreadDump = isEnable;
  }
  
  /**
   * Enable a heap dump for the snapshot.
   */
  public void setHeapDump(boolean isEnable)
  {
    _isHeapDump = isEnable;
  }
  
  /**
   * Enable a jmx dump for the snapshot.
   */
  public void setJmxDump(boolean isEnable)
  {
    _isJmxDump = isEnable;
  }
  
  /**
   * Enable a pdf report for the snapshot.
   */
  public void setPdfReport(boolean isEnable)
  {
    _isPdfReport = isEnable;
  }
  
  /**
   * Enable a scoreboard report for the snapshot.
   */
  public void setThreadScoreboard(boolean isEnable)
  {
    _isThreadScoreboard = isEnable;
  }
  
  @PostConstruct
  public void init()
  {
    if (_isThreadDump) {
      _dumpThreads.setLog(_isLog);
      _dumpThreads.init();
      add(_dumpThreads);
    }

    if (_isHeapDump) {
      _dumpHeap.setLog(_isLog);
      _dumpHeap.init();
      add(_dumpHeap);
    }
    
    if (_isJmxDump) {
      _dumpJmx.setLog(_isLog);
      _dumpJmx.init();
      add(_dumpJmx);
    }
    
    if (_isThreadScoreboard) {
      _threadScoreboard.setLog(_isLog);
      _threadScoreboard.init();
      add(_threadScoreboard);
    }
    
    /*
    if (_isPdfReport) {
      _pdfReport.init();
      add(_pdfReport);
    }
    */
    
    super.init();
  }
  
  /**
   * Output to server log in addition to internal database (default false).
   */
  public void setLog(boolean isLog)
  {
    _isLog = isLog;
  }
  
  /**
   * path to a PDF generating .php file 
   * (defaults to ${resin.home}/doc/admin/pdf-gen.php)
   */
  @Configurable
  public void setPath(String path)
  {
    //_pdfReport.setPath(path);
  }
  
  /**
   * report type key (default Summary)
   */
  @Configurable
  public void setReport(String report)
  {
    //_pdfReport.setReport(report);
  }
  
  /**
   * specifies look back period of time. e.g. '-period 1D' create the report 
   * since the same time yesterday (default 7D)
   */
  @Configurable
  public void setPeriod(Period period)
  {
    //_pdfReport.setPeriod(period);
  }
  
  /**
   * PDF output directory  (defaults to resin log directory)
   * @param logDirectory
   */
  @Configurable
  public void setLogDirectory(String logDirectory)
  {
    //_pdfReport.setLogDirectory(logDirectory);
  }
}
