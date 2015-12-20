/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.profile.Profile;
import com.caucho.v5.profile.ProfileEntry;
import com.caucho.v5.profile.ProfileReport;
import com.caucho.v5.profile.StackEntry;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Health action to create a thread dump.  The thread 
 * dump will be logged to the internal log database and the resin log file 
 * using com.caucho.health.action.DumpThreads as the class, at info level.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:DumpThreads>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:DumpThreads>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class ReportProfile extends HealthActionBase
{
  private static final L10N L = new L10N(ReportProfile.class);

  private static final Logger log 
    = Logger.getLogger(ReportProfile.class.getName());

  public static final String LOG_TYPE = "Caucho|Profile";
  
  private LogSystem _logSystem;
  private String _logType;

  private boolean _isLog = true;
  private boolean _onlyActive = false;

  private int _depth = 32;
  private Profile _profile;
  
  public ReportProfile()
  {
    _profile = Profile.create();
  }
  
  @PostConstruct
  public void init()
  {
    _logSystem = LogSystem.getCurrent();
    
    super.init();
  }
  
  public boolean isOnlyActive()
  {
    return _onlyActive;
  }
  
  /**
   * Output to server log in addition to internal database (default true).
   */
  @Configurable
  public void setLog(boolean isLog)
  {
    _isLog = isLog;
  }

  /**
   * Set dump to include only active threads
   */
  @Configurable
  public void setOnlyActive(boolean onlyActive)
  {
    _onlyActive = onlyActive;
  }
  
  /**
   * Sets the maximum depth of a thread dump.
   */
  @Configurable
  public void setDepth(int depth)
  {
    _depth  = depth;
  }
  
  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    /*
    StringBuilder sb = new StringBuilder();
    String profile = execute(_depth, _onlyActive);
    
    if (_isLog) {
      log.info(profile);
      sb.append(L.l("Thread dump logged to info level\n"));
    }
    */
    
    if (_logSystem != null) {
      String json = jsonProfile();
      
      if (_logType == null)
        _logType = _logSystem.createFullType(LOG_TYPE);
      
      _logSystem.log(_logType, json);
      //sb.append(L.l("Thread dump saved as type {0}\n", _logType));
    }
    
    //return new HealthActionResult(ResultStatus.OK, sb.toString());
    return null;
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
    
    long gcTicks = (report.getGcTime() + report.getPeriod() - 1)
      / report.getPeriod();
    
    
    if (entries.length > 0)
      sb.append(",\n");
    
    jsonGc(sb, gcTicks);
    
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
