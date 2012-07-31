/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin.action;

import java.io.IOException;
import java.util.logging.Logger;

import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.config.ConfigException;
import com.caucho.hemp.services.MailService;
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
  
  private static final long HOUR = 3600 * 1000L;
  private static final long DAY = 24 * 3600 * 1000L;
  
  private String _serverId;
  private String _logDirectory;
  
  private String _path;
  private long _period;
  private String _report;
  private String _title;

  private MailService _mailService = new MailService();
  private String _mailTo;
  private String _mailFrom;
  
  private boolean _isSnapshot;
  private long _profileTime;
  private long _profileTick;
  
  private boolean _isWatchdog;

  private boolean _isReturnPdf;
  
  private QuercusContext _quercus;
  
  private Path _phpPath;
  private Path _logPath;
  private String _fileName;

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
  
  public String getTitle()
  {
    return _title;
  }
  
  public void setTitle(String title)
  {
    _title = title;
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
  
  public void setWatchdog(boolean isWatchdog)
  {
    _isWatchdog = isWatchdog;
  }
  
  public boolean isWatchdog()
  {
    return _isWatchdog;
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
  
  public Path getLogPath()
  {
    return _logPath;
  }
  
  public void setLogDirectory(String logDirectory)
  {
    _logDirectory = logDirectory;
  }
  
  public String getMailTo()
  {
    return _mailTo;
  }
  
  public void setMailTo(String mailTo)
  {
    if (! "".equals(mailTo))
      _mailTo = mailTo;
  }
  
  public String getMailFrom()
  {
    return _mailFrom;
  }
  
  public void setMailFrom(String mailFrom)
  {
    if (! "".equals(mailFrom))
      _mailFrom = mailFrom;
  }

  public boolean isReturnPdf()
  {
    return _isReturnPdf;
  }

  public void setReturnPdf(boolean returnPdf)
  {
    _isReturnPdf = returnPdf;
  }

  private String calculateReport()
  {
    if (_report != null)
      return _report;
    else if (isWatchdog())
      return "Watchdog";
    else
      return "Snapshot";
  }
  
  private String calculateTitle()
  {
    if (_title != null)
      return _title;
    else
      return calculateReport();
  }
  
  private long calculatePeriod()
  {
    if (_period != 0)
      return _period;
    else if (isWatchdog())
      return 2 * HOUR;
    else
      return DAY;
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

    if (_path != null) {
      _phpPath = Vfs.lookup(_path);
    }
    else if (resin != null) {
      if (_path == null) {
        Path path = resin.getRootDirectory().lookup("doc/admin/pdf-gen.php");

        if (path.canRead()) {
          _path = path.getNativePath();
        }
      }
      
      if (_path == null) {
        Path path = resin.getResinHome().lookup("doc/admin/pdf-gen.php");

        if (path.canRead()) {
          _path = path.getNativePath();
        }
      }

      if (_path == null) {
        Path path = resin.getResinHome().lookup("php/admin/pdf-gen.php");

        if (path.canRead())
          _path = path.getNativePath();
      }

      if (_path != null)
        _phpPath = Vfs.lookup(_path);
    }
    
    if (_phpPath == null) {
      log.warning(L.l("{0} requires a 'path' attribute to a PDF generating .php file or '{1}'",
                      getClass().getSimpleName(),
                      resin.getResinHome().lookup("php/admin/pdf-gen.php").getNativePath()));
    }
    
    if (_logPath == null)
      _logPath = Vfs.lookup(_logDirectory);
    
    if (_mailTo != null) {
      try {
        _mailService.addTo(new InternetAddress(_mailTo));
        _mailService.init();
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    if (_mailFrom != null) {
      try {
        _mailService.addFrom(new InternetAddress(_mailFrom));
        _mailService.init();
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
  }

  public String getReportFileName() {
    if (_fileName == null) {
      String date = QDate.formatLocal(CurrentTime.getCurrentTime(), "%Y%m%dT%H%M");

      String serverId = Resin.getCurrent().getServerIdFilePart();

      _fileName = String.format("%s-%s-%s.pdf",
                                serverId,
                                calculateTitle(),
                                date);
    }

    return _fileName;
  }

  public PdfReportActionResult execute()
    throws IOException
  {
    Env env = null;
    WriteStream ws = null;
    TempOutputStream pdfOut = null;

    try {
      QuercusPage page = getQuercusContext().parse(_phpPath);
  
      TempStream ts = new TempStream();
      ts.openWrite();
      ws = new WriteStream(ts);
      
      HttpServletRequest request = new StubServletRequest();
      HttpServletResponse response = new StubServletResponse();
      
      env = _quercus.createEnv(page, ws, request, response);
      //env.setGlobalValue("health_service", env.wrapJava(healthService));
      env.setGlobalValue("g_report", env.wrapJava(calculateReport()));
      env.setGlobalValue("g_title", env.wrapJava(calculateTitle()));
      env.setGlobalValue("period", env.wrapJava(calculatePeriod() / 1000));

      env.setGlobalValue("g_is_snapshot", env.wrapJava(isSnapshot()));
      env.setGlobalValue("g_is_watchdog", env.wrapJava(isWatchdog()));

      if (getProfileTime() > 0) {
        env.setGlobalValue("profile_time", 
                           env.wrapJava(getProfileTime() / 1000));
      }

      if (getProfileTick() > 0) {
        env.setGlobalValue("profile_tick", env.wrapJava(getProfileTick()));
      }
      
      env.start();
  
      Value result = env.executeTop();

      if (! result.toString().equals("ok")) {
        throw new RuntimeException(L.l("{0} report generation failed: {1}", 
                                       calculateReport(), 
                                       result.toString()));
      }

      ws.flush();

      if (_mailTo != null && ! "".equals(_mailTo)) {
        mailPdf(ts);

        String message = L.l("{0} report mailed to {1}", 
                             calculateReport(), 
                             _mailTo);

        PdfReportActionResult actionResult =
          new PdfReportActionResult(message, null, null);

        return actionResult;
      }

      Path path = writePdfToFile(ts);

      if (_isReturnPdf) {
        pdfOut = new TempOutputStream();
        ts.writeToStream(pdfOut);
      }

      String message = L.l("{0} report generated at {1}", 
                           calculateReport(),
                           path);

      PdfReportActionResult actionResult
        = new PdfReportActionResult(message, path.getPath(), pdfOut);

      return actionResult;
    } finally {
      IoUtil.close(ws);
      IoUtil.close(pdfOut);

      if (env != null)
        env.close();

    }
  }
  
  private QuercusContext getQuercusContext()
  {
    synchronized (this) {
      if (_quercus == null) {
        _quercus = new QuercusContext();
        _quercus.setPwd(_phpPath.getParent());
        _quercus.init();
        _quercus.start();
      }
    }
    
    return _quercus;
  }
  
  private void mailPdf(TempStream ts)
    throws IOException
  {
    String fileName = getReportFileName();
    
    String userDate = QDate.formatLocal(CurrentTime.getCurrentTime(),
                                        "%Y-%m-%d %H:%M");
    
    String subject = "[Resin] PDF Report: " + calculateTitle() + "@" + _serverId
                     + " " + userDate;
    
    StringBuilder text = new StringBuilder();
    
    text.append("Resin generated PDF Report");
    text.append("\n");
    text.append("\nReport: ").append(calculateReport());
    text.append("\nGenerated: ").append(userDate);
    text.append("\nServer: ").append(_serverId);
    
    _mailService.sendWithAttachment(subject,
                                    text.toString(),
                                    "application/pdf",
                                    fileName,
                                    ts.openInputStream());
  }
  
  private Path writePdfToFile(TempStream ts)
    throws IOException
  {
    String fileName = getReportFileName();

    Path path = _logPath.lookup(fileName);

    path.getParent().mkdirs();
    
    WriteStream os = path.openWrite();
    try {
      ts.writeToStream(os);
    } finally {
      IoUtil.close(os);
    }
    
    return path;
  }

  public static class PdfReportActionResult
  {
    private String _message;
    private String _fileName;
    private TempOutputStream _pdfOutputStream;

    public PdfReportActionResult(String message,
                                 String fileName,
                                 TempOutputStream out)
    {
      _message = message;
      _fileName = fileName;
      _pdfOutputStream = out;
    }

    public String getMessage()
    {
      return _message;
    }

    public TempOutputStream getPdfOutputStream()
    {
      return _pdfOutputStream;
    }

    public String getFileName()
    {
      return _fileName;
    }
  }
}
