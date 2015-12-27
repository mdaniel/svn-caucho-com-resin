/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.jmx.JmxUtilResin;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.QDate;

/**
 * Health action to create a dump of all JMX attributes and values.  The jmx 
 * dump will be logged to the internal log database and the resin log file 
 * using com.caucho.health.action.DumpJmx as the class, at info level.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:DumpJmx>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:DumpJmx>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class DumpJmx extends HealthActionBase
{
  private static final L10N L = new L10N(DumpJmx.class);

  private static final Logger log 
    = Logger.getLogger(DumpJmx.class.getName());

  public static final String LOG_TYPE = "Resin|JmxDump";
  
  private LogSystem _logSystem;
  private String _logType;

  private boolean _isLog = false;
  
  private boolean _isAsync = false;
  private long _delay = 60000L;
  
  public DumpJmx()
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
   * If set, spawn a thread to dump the jmx.
   */
  @Configurable
  public void setAsync(boolean isAsync)
  {
    _isAsync = isAsync;
  }
  
  @Configurable
  public void setDelay(Period delay)
  {
    setDelayMillis(delay.getPeriod());
  }
  
  @Configurable
  public void setDelayMillis(long delay)
  {
    _delay = delay;
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
    if (_isAsync) {
      new Alarm(new JmxTask()).queue(_delay);
      return new HealthActionResult(ResultStatus.OK, 
                                    L.l("JMX dump scheduled with a {0} ms delay", _delay));
    }
    else
      return executeTask();
  }
  
  public HealthActionResult executeTask()
    throws Exception
  {
    StringBuilder sb = new StringBuilder();
    
    String jmxDump = execute();
 
    if (_isLog) {
      log.info(jmxDump);
      sb.append(L.l("JMX dump logged to info level\n"));
    }
    
    if (_logSystem != null) {
      if (_logType == null)
        _logType = _logSystem.createFullType(LOG_TYPE);
      
      _logSystem.log(_logType, jmxDump);
      sb.append(L.l("JMX dump saved as type {0}\n", _logType));
    }
    
    return new HealthActionResult(ResultStatus.OK, sb.toString());
  }
  
  public String execute()
    throws ConfigException, JMException, ClassNotFoundException
  {
    MBeanServer server = JmxUtilResin.getMBeanServer();
    if (server == null)
      server = ManagementFactory.getPlatformMBeanServer();
    
    if (server == null)
      return null;

    StringBuilder sb = new StringBuilder();
    
    long timestamp = CurrentTime.getCurrentTime();
    
    sb.append("{\n");
    sb.append("  \"create_time\": \"" + new Date(timestamp) + "\",\n");
    sb.append("  \"timestamp\": " + timestamp + ",\n");
    sb.append("  \"jmx\" : {\n");
    
    fillServer(sb, server);

    sb.append("\n  }");
    sb.append("\n}");
    
    return sb.toString();
  }
  
  private void fillServer(StringBuilder sb, MBeanServer server)
  {
    Set<ObjectName> beans = new HashSet<ObjectName>();

    //Set<ObjectName> objectNames = server.queryNames(new ObjectName("java.lang:type=Runtime"), null);
    ArrayList<ObjectName> objectNames = new ArrayList<ObjectName>();
    
    objectNames.addAll(server.queryNames(ObjectName.WILDCARD, null));
    
    Collections.sort(objectNames);
    
    boolean isFirst = true;
    
    for (ObjectName objectName : objectNames) {
      if (beans.contains(objectName))
        continue;

      beans.add(objectName);
      
      if (! isFirst)
        sb.append(",\n");
      
      isFirst = false;
      
      sb.append("\"");
      escapeString(sb, String.valueOf(objectName));
      sb.append("\" : {\n");

      dumpMBean(sb, server, objectName);
      
      sb.append("\n}");
    }
  }
  
  private void dumpMBean(StringBuilder sb, 
                         MBeanServer server,
                         ObjectName objectName)
  {
    MBeanAttributeInfo []attributes = null;
    
    try {
      synchronized (server) {
        attributes = server.getMBeanInfo(objectName).getAttributes();
      }
    } catch (Exception e) {
      sb.append("\"mbean_exception\": \"" + e + "\"\n");
      return;
    }
    
    boolean isFirst = true;
    
    for (MBeanAttributeInfo attribute : attributes) {
      if (! isFirst)
        sb.append(",\n");
      isFirst = false;
      
      Object value = null;
      
      try {
        value = server.getAttribute(objectName, attribute.getName());
      } catch (Throwable e) {
        value = e;
      }
      
      dumpNameValue(sb, attribute.getName(), value, "  ");
    }
  }
  
  private void dumpNameValue(StringBuilder sb,
                             String name,
                             Object value, String padding)
  {
    sb.append(padding);
    sb.append("\"");
    escapeString(sb, name);
    sb.append("\"");
    sb.append(": ");
    
    dumpValue(sb, value, padding);
  }
  
  private void dumpValue(StringBuilder sb, Object value, String padding)
  {
    if (value == null) {
      sb.append("null");
    } else if (value instanceof Object[]) {
      Object[] values = (Object[]) value;
      sb.append("[");
      
      boolean isFirst = true;
      for (Object v : values) {
        if (! isFirst)
          sb.append(",");
        isFirst = false;
        
        sb.append("\n" + padding + "  ");
        dumpValue(sb, v, padding + "  ");
      }
      
      sb.append("\n" + padding + "]");
    } else if (value instanceof CompositeData) {
      CompositeData data  = (CompositeData) value;
      CompositeType type = data.getCompositeType();

      sb.append(" {\n");
      sb.append(padding);
      sb.append("  \"java_class\": \"" + type.getTypeName() + "\"");
      
      for (String key : type.keySet()) {
        sb.append(",\n");
        dumpNameValue(sb, key, data.get(key), padding + "  ");
      }
      
      sb.append("\n" + padding + "}");
//    } else if (value instanceof TabularData) {
//      TabularData data = (TabularData) value;
//      TabularType type = data.getTabularType();
//      List<String> names = type.getIndexNames();
//      dump.append(type.getTypeName());
//      dump.append(" {\n");
//      for(String name : names) {
//        dump.append(name);
//        //dumpNameValue(name, data.get(name), dump, padding + "  ");
//      }
//      dump.append(padding);
//      dump.append("}\n");
    } else if (value instanceof Map) {
      Map<Object, Object> data = (Map<Object, Object>) value;
      sb.append("{\n");
      sb.append(padding);
      sb.append("  \"java_class\":\"" + value.getClass().getName() + "\"");
      
      for(Map.Entry<Object, Object> entry : data.entrySet()) {
        sb.append(",\n");
        dumpNameValue(sb, entry.getKey().toString(), entry.getValue(), 
                      padding + "  ");
      }
      sb.append("\n" + padding);
      sb.append("}");
    } else if (value instanceof List) {
      List<Object> values = (List<Object>) value;
      sb.append("[\n");
      
      boolean isFirst = true;
      
      for (Object v : values) {
        if (! isFirst) {
          sb.append(",\n");
        }
        isFirst = false;
        
        sb.append(padding + "  ");
        dumpValue(sb, v, padding + "  ");
      }
      sb.append(padding);
      sb.append("]");
    } else if (value instanceof Throwable) {
      Throwable e = (Throwable) value;
      if (e instanceof UnsupportedOperationException) {
        sb.append("\"Not supported\"");
      } else {
        Throwable cause = e.getCause();
        if (cause != null) {
          dumpValue(sb, cause, padding);
        } else {
          sb.append("\"" + e + "\"");
        }
      }
    } else if (value instanceof Number) {
      sb.append(value);
    } else if (value instanceof Boolean) {
      sb.append(value);
    } else if (value instanceof Date) {
      sb.append("\"" + QDate.formatISO8601(((Date) value).getTime()) + "\"");
    } else {
      sb.append("\"");
      escapeString(sb, String.valueOf(value));
      sb.append("\"");
    }
  }
  
  private void escapeString(StringBuilder sb, String value)
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
        break;
      }
    }
  }
  
  
  class JmxTask implements AlarmListener {
    public void handleAlarm(Alarm alarm)
    {
      try {
        executeTask();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
