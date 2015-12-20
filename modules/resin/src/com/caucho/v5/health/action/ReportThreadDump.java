/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.ThreadDump;

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
public class ReportThreadDump extends HealthActionBase
{
  private static final L10N L = new L10N(ReportThreadDump.class);

  private static final Logger log 
    = Logger.getLogger(ReportThreadDump.class.getName());

  public static final String LOG_TYPE = "Caucho|ThreadDump";
  
  private LogSystem _logSystem;
  private String _logType;

  private boolean _isLog = true;
  private boolean _onlyActive = false;

  private int _depth = 32;
  private ThreadDump _threadDump;
  
  public ReportThreadDump()
  {
    _threadDump = ThreadDump.create();
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
    StringBuilder sb = new StringBuilder();
    String threadDump = execute(_depth, _onlyActive);
    
    if (_isLog) {
      log.info(threadDump);
      sb.append(L.l("Thread dump logged to info level\n"));
    }
    
    if (_logSystem != null) {
      String json = executeJson();
      
      if (_logType == null)
        _logType = _logSystem.createFullType(LOG_TYPE);
      
      _logSystem.log(_logType, json);
      sb.append(L.l("Thread dump saved as type {0}\n", _logType));
    }
    
    return new HealthActionResult(ResultStatus.OK, sb.toString());
  }
  
  public String execute(int depth, boolean onlyActive)
  {
    return _threadDump.getThreadDump(depth, onlyActive);
  }

  public String executeJson()
  {
    return _threadDump.jsonThreadDump();
  }
}
