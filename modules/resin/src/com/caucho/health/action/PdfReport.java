/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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
public class PdfReport extends ActionSequence
{
  @PostConstruct
  public void init()
  {
    /*
    if (_isPdfReport) {
      _pdfReport.init();
      add(_pdfReport);
    }
    */
    
    //super.init();
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
  
  public void setSnapshot(boolean isSnapshot)
  {
    
  }
  
  public void setMailTo(String address)
  {
    
  }
  
  public void setMailFrom(String address)
  {
    
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
