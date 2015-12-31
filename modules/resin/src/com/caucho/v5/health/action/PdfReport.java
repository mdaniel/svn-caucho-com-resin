/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.service.Startup;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.profile.Profile;
import com.caucho.v5.server.admin.AdminPdfBuilder;
import com.caucho.v5.server.admin.GraphBuilderAdminPdf;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.QDate;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

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
public class PdfReport extends HealthActionBase
{
  private static final Logger log = Logger.getLogger(PdfReport.class.getName());
  
  private String _pathFormat;

  /**
   * path to the target file.
   */
  @Configurable
  public void setPath(String path)
  {
    _pathFormat = path;
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
  
  @PostConstruct
  public void init()
  {
    if (_pathFormat == null) {
      _pathFormat = "bfs:///system/report/pdf/snapshot";
    }
    
    super.init();
    /*
    if (_isPdfReport) {
      _pdfReport.init();
      add(_pdfReport);
    }
    */
    
    //super.init();
  }
  
  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
  {
    long now = CurrentTime.getCurrentTime();
    
    try {
      String pathName = QDate.formatLocal(now, _pathFormat);
      
      Path path = Vfs.lookup(pathName);
      
      AdminPdfBuilder builder = new AdminPdfBuilder();

      GraphBuilderAdminPdf graphBuilder = builder.graphBuilder("Test Graph");
      graphBuilder.build();

      builder.scoreboard();

      builder.threadDump();

      builder.profile();

      builder.build(path);
      
      Profile profile = Profile.create();
      
      if (profile.isBackground()) {
        profile.reset();
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.log(Level.WARNING, e.toString(), e);
    }
    
    return new HealthActionResult();
  }
}
