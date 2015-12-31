/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.service.Startup;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthCheckResult;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.check.HealthCheck;
import com.caucho.v5.health.check.SystemHealthCheck;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Health action to create a dump of all health cehcks.  The  
 * dump will be logged to the internal log database and the resin log file 
 * using com.caucho.health.action.DumpHealth as the class, at info level.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:DumpHealth>
 *   <health:IfHealthCritical/>
 * </health:DumpHealth>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class DumpHealth extends HealthActionBase
{
  private static final L10N L = new L10N(DumpHealth.class);

  private static final Logger log 
    = Logger.getLogger(DumpHealth.class.getName());

  public static final String LOG_TYPE = "Caucho|HealthDump";
  
  private LogSystem _logSystem;
  private String _logType;

  private boolean _isLog = false;
  
  public DumpHealth()
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
      log.info(execute());
      sb.append(L.l("Health dump logged to info level\n"));
    }
      
    if (_logSystem != null) {
      if (_logType == null)
        _logType = _logSystem.createFullType(LOG_TYPE);
      
      _logSystem.log(_logType, executeJson());
      sb.append(L.l("Health dump saved as type {0}\n", _logType));
    }
    
    return new HealthActionResult(ResultStatus.OK, sb.toString());
  }
  
  public String execute()
  {
    HealthSubSystem healthService = HealthSubSystem.getCurrent();
    
    if (healthService == null) {
      throw new IllegalStateException(L.l("HealthService is not started"));
    }
    
    HealthCheckResult summaryResult = healthService.getSummaryResult();
    
    List<HealthCheck> checks = healthService.getHealthChecks();
    
    int width1 = 6;
    int width2 = 7;
    
    for (HealthCheck check : checks) {
      HealthCheckResult result = healthService.getLastResult(check);
      if(result.getStatus().toString().length() > width1)
        width1 = result.getStatus().toString().length();
      if(check.getName().length() > width2)
        width2 = check.getName().length();
    }

    String columnFormat = "%-" + (width1+1) + "s%-" + (width2+1) + "s%s";
    
    StringBuilder sb = new StringBuilder();
    
    sb.append(String.format(columnFormat, "Status", "Check", "Message"));
    sb.append("\n");

    sb.append(String.format(columnFormat, 
                            summaryResult.getStatus(),
                            "Overall",
                            summaryResult.getMessage()));
    sb.append("\n");
    
    
    for (HealthCheck check : checks) {
      if (check instanceof SystemHealthCheck)
        continue;
      
      HealthCheckResult result = healthService.getLastResult(check);
      
      sb.append(String.format(columnFormat, 
                              result.getStatus(),
                              check.getName(),
                              result.getMessage()));
      sb.append("\n");
    }
    
    return sb.toString();
  }

  public String executeJson()
  {
    StringBuilder sb = new StringBuilder();
    
    long timestamp = CurrentTime.getCurrentTime();
    
    sb.append("{\n");
    sb.append("  \"create_time\": \"" + new Date(timestamp) + "\",\n");
    sb.append("  \"timestamp\": " + timestamp + ",\n");
    sb.append("  \"health\" : [\n");
    
    HealthSubSystem healthService = HealthSubSystem.getCurrent();
    if (healthService != null) {
      HealthCheckResult summaryResult = healthService.getSummaryResult();
      if (summaryResult != null) {
        
        sb.append(jsonDumpCheck("Overall", 
                                summaryResult.getStatus().toString(),
                                summaryResult.getMessage(),
                                new Date(summaryResult.getTimestamp())));
        
        List<HealthCheck> checks = healthService.getHealthChecks();
        if (! checks.isEmpty()) {
          sb.append(",\n");
          
          boolean isFirst = true;
          for (HealthCheck check : checks) {
            if (check instanceof SystemHealthCheck)
              continue;
            
            if (! isFirst)
              sb.append(",\n");
            isFirst = false;
  
            HealthCheckResult result = healthService.getLastResult(check);
            
            sb.append(jsonDumpCheck(check.getName(), 
                                    result.getStatus().toString(), 
                                    result.getMessage(),
                                    new Date(result.getTimestamp())));
          }
        }
      }
    }
    
    sb.append("\n]");
    sb.append("\n}");
    
    return sb.toString();
  }
  
  private static String jsonDumpCheck(String name, 
                                      String status, 
                                      String message,
                                      Date timestamp)
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append("{\n");
    
    sb.append("  \"name\" : \"");
    escapeString(sb, name);
    sb.append("\",\n");
    
    sb.append("  \"status\" : \"");
    escapeString(sb, status);
    sb.append("\",\n");

    sb.append("  \"message\" : \"");
    escapeString(sb, message);
    sb.append("\",\n");
    
    sb.append("  \"timestamp\" : \"");
    sb.append(timestamp);
    sb.append("\"\n");

    sb.append("}");

    return sb.toString();
  }
  
  private static void escapeString(StringBuilder sb, String value)
  {
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
      }
    }
  }
  
}
