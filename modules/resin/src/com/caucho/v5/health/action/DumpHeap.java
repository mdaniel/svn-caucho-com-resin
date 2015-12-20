/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.jmx.JmxUtil;
import com.caucho.v5.profile.HeapDump;
import com.caucho.v5.server.container.ServerBase;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.QDate;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

/**
 * Health action to create a heap dump.  The heap 
 * dump will be logged to the internal log database and the resin log file 
 * using com.caucho.health.action.DumpHeap as the class, at info level.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:DumpHeap>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:DumpHeap>
 * }</pre>
 */
@Startup
@Singleton
@Configurable
public class DumpHeap extends HealthActionBase
{
  private static final L10N L = new L10N(DumpHeap.class);

  private static final Logger log 
    = Logger.getLogger(DumpHeap.class.getName());
  
  public static final String LOG_TYPE = "Resin|HeapDump";
  public static final String HPROF_LOG_TYPE = "Resin|HeapDump|Hprof";
  
  private LogSystem _logSystem;
  private String _logType;

  private boolean _isLog = true;
  private boolean _isHprof = false;

  private Path _hprofPath;
  private String _hprofPathFormat;
  
  private String _serverId;
    
  public DumpHeap()
  {
  }
  
  @PostConstruct
  public void init()
  {
    ServerBartender server = BartenderSystem.getCurrentSelfServer();
    
    if (server != null)
      _serverId = server.getId();
    else
      _serverId = "unknown";
    
    _logSystem = LogSystem.getCurrent();
    
    super.init();
  }
  
  public boolean isRaw()
  {
    return _isHprof;
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
   * Generate an HPROF heap dump instead of human readable heap dump. 
   */
  @Deprecated
  @Configurable
  public void setRaw(boolean raw)
  {
    setHprof(raw);
  }
  
  /**
   * Generate an HPROF heap dump instead of human readable heap dump. 
   */
  @Configurable
  public void setHprof(boolean isHprof)
  {
    _isHprof = isHprof;
  }
  
  /**
   * Output path for the hprof dump file.
   * Default log/heap.hprof  
   */
  @Configurable
  public void setHprofPath(Path hprofPath)
  {
    _hprofPath = hprofPath;
  }
  
  @Deprecated
  @Configurable
  public void setHprofDir(Path hprofPath)
  {
    setHprofPath(hprofPath);
  }
  
  /**
   * Format for generating dynamic path names with timestamps.
   * Default log/heap.hprof  
   */
  public void setHprofPathFormat(String hprofPathFormat)
  {
    _hprofPathFormat = hprofPathFormat;
  }
  
  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    StringBuilder sb = new StringBuilder();
    
    Path hprofPath = _hprofPath;
    if (_isHprof && hprofPath == null && _hprofPathFormat != null) {
      long time = CurrentTime.getCurrentTime();
      String formattedPath = QDate.formatLocal(time, _hprofPathFormat);
      hprofPath = Vfs.lookup(formattedPath);
    }
    
    if (hprofPath != null)
      sb.append(L.l("HPROF heap dump written to {0}\n", hprofPath));
    
    String heapDump = execute(_isHprof, _serverId, hprofPath);

    if (_isLog || _isHprof) {
      log.info(heapDump);
      sb.append(L.l("Heap dump logged to info level\n"));
    }
    
    if (_logSystem != null) {
      if (_logType == null) {
        String logType = _isHprof ? HPROF_LOG_TYPE : LOG_TYPE;
        _logType = _logSystem.createFullType(logType);
      }

      if (_isHprof) {
        _logSystem.log(_logType, heapDump);
        sb.append(L.l("HPROF heap dump saved as type {0}\n", _logType));
      }
      else {
        String json = executeJson();
        _logSystem.log(_logType, json);
        sb.append(L.l("Heap dump saved as type {0}\n", _logType));
      }
    }
    
    return new HealthActionResult(ResultStatus.OK, sb.toString());
  }
  
  public String execute(boolean isJvmHprof, String serverId, Path hprofPath)
    throws ConfigException, JMException, IOException
  {
    if (isJvmHprof)
      return doJvmHprofHeapDump(serverId, hprofPath);
    else
      return doProHeapDump();
  }
  
  private String doJvmHprofHeapDump(String serverId, Path hprofPath)
    throws ConfigException, JMException, IOException
  {
    ObjectName name = new ObjectName(
      "com.sun.management:type=HotSpotDiagnostic");
    
    if (hprofPath == null) {
      ServerBase resin = ServerBase.getCurrent();
      
      if (resin == null)
        hprofPath = Vfs.lookup(System.getProperty("java.io.tmpdir"));
      else
        hprofPath = resin.getLogDirectory();
      
      hprofPath = hprofPath.lookup("heap.hprof");
    } else if (hprofPath.isDirectory()) {
      hprofPath = hprofPath.lookup("heap.hprof");
    }

    hprofPath.getParent().mkdirs();

    //MemoryPoolAdapter memoryAdapter = new MemoryPoolAdapter();
    //if (memoryAdapter.getEdenUsed() > hprofPath.getDiskSpaceFree())
    //  throw new ConfigException(L.l("Not enough disk space for `{0}'", fileName));

    // dumpHeap fails if file exists, it will not overwrite, so we have to delete
    if (hprofPath.exists() && hprofPath.isFile())
      hprofPath.remove();

    MBeanServer mBeanServer = JmxUtil.getMBeanServer();
    mBeanServer.invoke(name,
                       "dumpHeap",
                       new Object[]{hprofPath.getPath(), Boolean.TRUE},
                       new String[]{String.class.getName(), boolean.class.getName()});

    final String result = L.l("Heap dump is written to `{0}'.\n"
                              + "To view the file on the target machine use\n"
                              + "jvisualvm --openfile {0}", hprofPath.getPath());

    return result;
  }
  
  private String doProHeapDump()
    throws IOException
  {
    HeapDump dump = HeapDump.create();
    
    StringWriter buffer = new StringWriter();
    PrintWriter writer = new PrintWriter(buffer);
    dump.writeExtendedHeapDump(writer);
    writer.flush();
    
    return buffer.toString();
  }
  
  public String executeJson()
    throws IOException
  {
    HeapDump dump = HeapDump.create();
   
    return dump.jsonHeapDump();
  }
}
