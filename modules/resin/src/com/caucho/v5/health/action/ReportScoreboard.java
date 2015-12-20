/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Health action to create a thread scoreboard report.  The scoreboards  
 * will be logged to the internal log database and the resin log file 
 * using com.caucho.health.action.ScoreboardReport as the class, at info level.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:ScoreboardReport>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:ScoreboardReport>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class ReportScoreboard extends HealthActionBase
{
  private static final L10N L = new L10N(ReportScoreboard.class);

  private static final Logger log 
    = Logger.getLogger(ReportScoreboard.class.getName());

  private static final String LOG_TYPE_PREFIX = "Resin|Scoreboard";
  
  private LogSystem _logSystem;
  private String _logType;

  private boolean _isLog = false;
  private String _type = "resin";
  private boolean _greedy = true;
  
  public ReportScoreboard()
  {
  }
  
  /**
   * Output to server log in addition to internal database (default false).
   */
  @Configurable
  public void setLog(boolean isLog)
  {
    _isLog = isLog;
  }
  
  /**
   * Report type name
   * (ie "resin" for Resin thread overview, "database" for database threads)
   */
  @Configurable
  public void setType(String type)
  {
    _type = type;
  }
  
  /**
   * If true threads can not be in more than one scoreboard
   */
  @Configurable
  public void setGreedy(boolean greedy)
  {
    _greedy = greedy;
  }
    
  @PostConstruct
  public void init()
  {
    _logSystem = LogSystem.getCurrent();
    
    super.init();
  }
  
  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    StringBuilder sb = new StringBuilder();

    if (_isLog) {
      log.info(execute(_type, _greedy));
      sb.append(L.l("Scoreboard {0} logged to info level\n", _type));
    }
      
    if (_logSystem != null) {
      if (_logType == null)
        _logType = _logSystem.createFullType(createLogType(_type));
      
      _logSystem.log(_logType, executeJson(_type, _greedy));
      sb.append(L.l("Scoreboard \"{0}\" saved as type {1}\n", 
                    _type,
                    _logType));
    }
    
    return new HealthActionResult(ResultStatus.OK, sb.toString());
  }
  
  public static String createLogType(String type)
  {
    return LOG_TYPE_PREFIX + "|" + type;
  }
  
  public String execute(String type, boolean greedy)
  {
    return execute(type, greedy, 80);
  }

  public String execute(String type, boolean greedy, int lineWidth)
  {
    ThreadActivityReportBase report = getReportType(type);
    if (report == null) {
      return L.l("Unknown Scoreboard Report type {0}", type);
    }
    
    ThreadActivityGroup []groups = report.execute(greedy);
    
    StringBuilder sb = new StringBuilder();
    
    for (ThreadActivityGroup group : groups) {
      String scoreboard = group.toScoreboard();
      
      sb.append("[");
      sb.append(group.getName());
      sb.append("]");
      sb.append("\n");
      
      sb.append(breakIntoLines(scoreboard, lineWidth));
      sb.append("\n");
      sb.append("\n");
    }
    
    sb.append("[Scoreboard Key]");
    sb.append("\n");
   
    Map<Character, String> key = report.getScoreboardKey();
    for (Map.Entry<Character, String> entry : key.entrySet()) {
      sb.append(entry.getKey());
      sb.append("   ");
      sb.append(entry.getValue());
      sb.append("\n");
    }
    
    return sb.toString();
  }
  
  public String executeJson(String type, boolean greedy)
  {
    StringBuilder sb = new StringBuilder();
    
    long timestamp = CurrentTime.getCurrentTime();
    
    sb.append("{\n");
    sb.append("  \"create_time\": \"" + new Date(timestamp) + "\",\n");
    sb.append("  \"timestamp\": " + timestamp + ",\n");

    ThreadActivityReportBase report = getReportType(type);
    
    sb.append("  \"scoreboards\": {\n");
    if (report != null) {
      ThreadActivityGroup []groups = report.execute(greedy);
      if (groups != null) {
        boolean isFirst = true;
        for (ThreadActivityGroup group : groups) {
          if (! isFirst)
            sb.append(",\n");
          isFirst = false;
          
          sb.append("    \"" + group.getName() + "\": ");
          sb.append("\"" + group.toScoreboard() + "\"");
        }
      }
    }
    
    sb.append("\n  },\n");
    sb.append("  \"keys\": {\n");
    
    if (report != null) {
      Map<Character, String> key = report.getScoreboardKey();
      
      boolean isFirst = true;
      for (Map.Entry<Character, String> entry : key.entrySet()) {
        if (! isFirst)
          sb.append(",\n");
        isFirst = false;

        sb.append("    \"" + entry.getKey() + "\": ");
        sb.append("\"" + entry.getValue() + "\"");
      }
    }
    
    sb.append("\n  },\n");
    sb.append("}");
    
    return sb.toString();
  }
  
  protected ThreadActivityReportBase getReportType(String typeName)
  {
    if (typeName.equalsIgnoreCase("resin"))
      return new ThreadActivityReportResin();
    else if (typeName.equalsIgnoreCase("database"))
      return new ThreadActivityReportDatabase();
    
    try {
      Class c = Class.forName("com.caucho.v5.admin.thread." + typeName);
      return (ThreadActivityReportBase) c.newInstance();
    } catch (Exception e) {
      log.log(Level.FINER, L.l("Failed to load scoreboard report type {0}: {1}", 
                               typeName, e.getMessage()), e);
      return null;
    }
  }
  
  private static String breakIntoLines(String s, int w)
  {
    if (s.length() <= w)
      return s;
    
    StringBuilder sb = new StringBuilder(s);
    
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      int pos = (i * w) + i - 1;
      
      if (sb.length() <= pos) {
        break;
      }
      
      sb.insert(pos, "\n");
    }
    
    return sb.toString();
  }
}
