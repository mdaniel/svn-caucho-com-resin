/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin.action;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.config.ConfigException;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.server.http.StubServletRequest;
import com.caucho.server.http.StubServletResponse;
import com.caucho.server.resin.Resin;
import com.caucho.util.*;
import com.caucho.vfs.*;

public class PdfReportAction implements AdminAction
{
  private static final L10N L = new L10N(PdfReportAction.class);
  private static final Logger log
    = Logger.getLogger(PdfReportAction.class.getName());

  private String _serverId;
  private String _logDirectory;
  
  private String _path;
  private long _period = 7 * 24 * 3600 * 1000L;
  private String _report = "Summary";
  
  private boolean _isSnapshot;
  private long _profileTime;
  private long _profileTick;
  
  private QuercusContext _quercus;
  
  private Path _phpPath;
  private Path _logPath;

  public String getPath()
  {
    return _path;
  }

  public void setPath(String path)
  {
    _path = path;
  }
  
  public String getReport()
  {
    return _report;
  }
  
  public void setReport(String report)
  {
    _report = report;
  }
  
  public long getPeriod()
  {
    return _period;
  }
  
  public void setPeriod(long period)
  {
    _period = period;
  }
  
  public boolean isSnapshot()
  {
    return _isSnapshot;
  }
  
  public void setSnapshot(boolean isSnapshot)
  {
    _isSnapshot = isSnapshot;
  }
  
  public long getProfileTime()
  {
    return _profileTime;
  }
  
  public void setProfileTime(long profileTime)
  {
    _profileTime = profileTime;
  }
  
  public long getProfileTick()
  {
    return _profileTick;
  }
  
  public void setProfileTick(long profileTick)
  {
    _profileTick = profileTick;
  }

  public String getLogDirectory()
  {
    return _logDirectory;
  }
  
  public void setLogDirectory(String logDirectory)
  {
    _logDirectory = logDirectory;
  }
  
  public void init()
  {
    Resin resin = Resin.getCurrent();
    if (resin != null) {
      _serverId = resin.getServerId();
      
      if (_logDirectory == null)
        _logPath = resin.getLogDirectory();
    } else {
      _serverId = "unknown";
      
      if (_logDirectory == null)
        _logPath = Vfs.getPwd();
    }
    
    // If Resin is running then path is optional and should default 
    // to ${resin.home}/doc/admin/pdf-gen.php
    //
    // If Resin is not running, then path is required
    
    if (resin != null) {
      if (_path == null)
        _path = resin.getResinHome() + "/doc/admin/pdf-gen.php";
      _phpPath = Vfs.lookup(_path);
    } else if (_path != null) {
        _phpPath = Vfs.lookup(_path);
    }
    
    if (_phpPath == null) {
      throw new ConfigException(L.l("{0} requires a path to a PDF generating .php file",
                                    getClass().getSimpleName()));
    }
    
    if (_logPath == null)
      _logPath = Vfs.lookup(_logDirectory);
    
    _quercus = new QuercusContext();
    _quercus.setPwd(_phpPath.getParent());
    _quercus.init();
    _quercus.start();
  }

  public String execute()
    throws Exception
  {
    Env env = null;
    
    try {
      QuercusPage page = _quercus.parse(_phpPath);
  
      TempStream ts = new TempStream();
      ts.openWrite();
      WriteStream ws = new WriteStream(ts);
      
      HttpServletRequest request = new StubServletRequest();
      HttpServletResponse response = new StubServletResponse();
      
      env = _quercus.createEnv(page, ws, request, response);
      //env.setGlobalValue("health_service", env.wrapJava(healthService));
      env.setGlobalValue("pdf_name", env.wrapJava(_report));
      env.setGlobalValue("period", env.wrapJava(_period / 1000));

      env.setGlobalValue("g_is_snapshot", env.wrapJava(isSnapshot()));

      if (getProfileTime() > 0) {
        env.setGlobalValue("profile_time", 
                           env.wrapJava(getProfileTime() / 1000));
      }

      if (getProfileTick() > 0) {
        env.setGlobalValue("profile_tick", env.wrapJava(getProfileTick()));
      }
      
      env.start();
  
      Value result = env.executeTop();
  
      ws.close();
        
      if (result.toString().equals("ok")) {
        String date = QDate.formatLocal(Alarm.getCurrentTime(), "%Y%m%dT%H%M");
        Path path = _logPath.lookup(String.format("%s-%s-%s.pdf",
                                                  _serverId,
                                                  _report,
                                                  date));
        path.getParent().mkdirs();
        
        WriteStream os = path.openWrite();
        try {
          ts.writeToStream(os);
        } finally {
          IoUtil.close(os);
        }
        
        return(L.l("generated {0}", path));
      }
      else {
        throw new Exception(L.l("generation failed: {0}", result.toString()));
      }
    } finally {
      if (env != null)
        env.close();
    }
  }
}
