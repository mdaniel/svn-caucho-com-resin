package com.caucho.resin.admin.service;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

import javax.inject.Inject;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import com.caucho.amp.jamp.JampServlet;
import com.caucho.amp.thread.ThreadAmp;
import com.caucho.health.action.ResinThreadActivityReport;
import com.caucho.health.action.ThreadActivityGroup;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ClusterServerMXBean;
import com.caucho.management.server.HealthCheckMXBean;
import com.caucho.management.server.HealthEventLog;
import com.caucho.management.server.HealthSystemMXBean;
import com.caucho.management.server.JvmThreadsMXBean;
import com.caucho.management.server.MemoryMXBean;
import com.caucho.management.server.ProxyCacheMXBean;
import com.caucho.management.server.ResinMXBean;
import com.caucho.management.server.ServerMXBean;
import com.caucho.management.server.TcpConnectionInfo;
import com.caucho.management.server.ThreadPoolMXBean;

import io.baratine.core.*;

@Remote
@ResourceService("public:///admin")
public class ResinAdminService
{
  @Inject
  private MBeanServerConnection _mbeanServer;

  private ResinMXBean _resin;
  private ServerMXBean _server;
  private ClusterMXBean _cluster;
  private ProxyCacheMXBean _proxyCache;

  @OnActive
  public void onActive()
  {
    System.err.println(getClass().getSimpleName() + ".onActive0");
    
    _mbeanServer = MBeanUtil.getServer();

    _resin = MBeanUtil.find("caucho:type=Resin", ResinMXBean.class);
    _server = MBeanUtil.find("caucho:type=Server", ServerMXBean.class);
    _cluster = MBeanUtil.find("caucho:type=Cluster", ClusterMXBean.class);
    _proxyCache = MBeanUtil.find("caucho:type=ProxyCache", ProxyCacheMXBean.class);
  }

  public String getCurrentServerId()
  {
    System.err.println(getClass().getSimpleName() + ".getCurrentServerId0");

    return _server.getId();
  }

  public ServerInfo getServerInfo(String serverId)
  {
    System.err.println(getClass().getSimpleName() + ".getServerInfo0: " + serverId);

    ResinMXBean resinBean = getResinBean(serverId);
    ServerMXBean serverBean = getServerBean(serverId);
    serverId = serverBean.getId();

    ServerInfo info = new ServerInfo();

    info._serverId = serverBean.getId();
    info._serverIndex = serverBean.getServerIndex();

    info._user = resinBean.getUserName();
    //info._machine = resinBean.getLocalHost();

    info._resinVersion = resinBean.getVersion();

    info._watchdogMessage = resinBean.getWatchdogStartMessage();

    info._state = serverBean.getState();

    info._startTime = serverBean.getStartTime().getTime();
    info._uptime = serverBean.getUptime();

    info._freeHeap = serverBean.getRuntimeMemoryFree();
    info._totalHeap = serverBean.getRuntimeMemory();

    //info._license = getString(serverId, "caucho:type=LicenseStore", "LicenseMessage");

    // XXX: get stats based on the selected server
    try {
      info._machine = getString(serverId, "java.lang:type=Runtime", "Name");
      info._jdkVersion = getString(serverId, "java.lang:type=Runtime", "VmName")
                         + getString(serverId, "java.lang:type=Runtime", "VmVersion");

      info._osVersion = getString(serverId, "java.lang:type=OperatingSystem", "AvailableProcessors")
                        + " CPU, "
                        + getString(serverId, "java.lang:type=OperatingSystem", "Name")
                        + " "
                        + getString(serverId, "java.lang:type=OperatingSystem", "Arch")
                        + " "
                        + getString(serverId, "java.lang:type=OperatingSystem", "Version");

      info._freePhysical = getLong(serverId, "java.lang:type=OperatingSystem", "FreePhysicalMemorySize");
      info._totalPhysical = getLong(serverId, "java.lang:type=OperatingSystem", "TotalPhysicalMemorySize");

      info._freeSwap = getLong(serverId, "java.lang:type=OperatingSystem", "FreeSwapSpaceSize");
      info._totalSwap = getLong(serverId, "java.lang:type=OperatingSystem", "TotalSwapSpaceSize");

      info._fileDescriptors = getLong(serverId, "java.lang:type=OperatingSystem", "OpenFileDescriptorCount");
      info._maxFileDescriptors = getLong(serverId, "java.lang:type=OperatingSystem", "MaxFileDescriptorCount");

      info._cpuLoadAverage = getDouble(serverId, "java.lang:type=OperatingSystem", "SystemLoadAverage");
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return info;
  }

  private int getInt(String serverId, String group, String name)
  {
    Object value = getAttribute(serverId, group, name);

    if (value instanceof Integer) {
      return (Integer) value;
    }
    else {
      return -1;
    }
  }

  private long getLong(String serverId, String group, String name)
  {
    Object value = getAttribute(serverId, group, name);

    if (value instanceof Long) {
      return (Long) value;
    }
    else {
      return -1;
    }
  }

  private double getDouble(String serverId, String group, String name)
  {
    Object value = getAttribute(serverId, group, name);

    if (value instanceof Double) {
      return (Double) value;
    }
    else {
      return -1.0;
    }
  }

  private String getString(String serverId, String group, String name)
  {
    Object value = getAttribute(serverId, group, name);

    if (value != null) {
      return value.toString();
    }
    else {
      return null;
    }
  }

  public List<String> getServers()
  {
    System.err.println(getClass().getSimpleName() + ".getServers0");

    ArrayList<String> serverList = new ArrayList<String>();

    try {
      if (_cluster != null) {
        ClusterMXBean []clusters = _resin.getClusters();

        for (ClusterMXBean cluster : clusters) {
          ArrayList<String> list = new ArrayList<String>();

          if (cluster == null) {
            cluster = _cluster;
          }

          String clusterName = cluster.getName();

          for (ClusterServerMXBean server : cluster.getServers()) {
            int index = server.getClusterIndex();

            String serverId = clusterName + "-" + index;

            list.add(serverId);
          }
        }
      }
      else {
        String serverId = _server.getId();

        serverList.add(serverId);
      }

      return serverList;
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public HealthStatus getHealthStatus(String serverId)
  {
    System.err.println(getClass().getSimpleName() + ".getHealthStatus0: " + serverId);

    HealthCheckMXBean healthBean
      = MBeanUtil.find("caucho:type=HealthCheck,name=Resin",
                       HealthCheckMXBean.class,
                       serverId);

    return new HealthStatus(serverId,
                            healthBean.getStatus(),
                            healthBean.getMessage());
  }

  public HealthCheck []getHealthChecks(String serverId)
  {
    String []names = jmxQueryNames("caucho:type=HealthCheck,Server=" + serverId + ",*");

    HealthCheck []checks = new HealthCheck[names.length];

    for (int i = 0; i < names.length; i++) {
      String name = names[i];

      HealthCheckMXBean healthBean = MBeanUtil.find(name, HealthCheckMXBean.class);

      checks[i] = new HealthCheck(serverId,
                                  healthBean.getName(),
                                  healthBean.getStatus(),
                                  healthBean.getMessage());
    }

    return checks;
  }

  public HealthLog []getHealthLogs(String serverId,
                                   long start, long end, int limit)
  {
    System.err.println(getClass().getSimpleName() + ".getHealthLogs0");

    if (end < 0) {
      end = Long.MAX_VALUE / 2;
    }

    try {
      HealthSystemMXBean healthBean
        = MBeanUtil.find("caucho:type=HealthSystem",
                         HealthSystemMXBean.class,
                         serverId);

      HealthEventLog []events = healthBean.findEvents(-1, start, end, limit);

      HealthLog []logs = new HealthLog[events.length];

      for (int i = 0; i < events.length; i++) {
        HealthEventLog event = events[i];

        logs[i] = new HealthLog(serverId,
                                event.getType().name(),
                                event.getTimestamp(),
                                event.getSource(),
                                event.getMessage());
      }

      return logs;
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public MemoryState getMemoryState(String serverId)
  {
    System.err.println(getClass().getSimpleName() + ".getMemoryState0");

    try {
      MemoryMXBean memoryBean
        = MBeanUtil.find("caucho:type=Memory", MemoryMXBean.class, serverId);

      MemoryState state
        = new MemoryState(serverId,
                          memoryBean.getCodeCacheCommitted(), memoryBean.getCodeCacheMax(),
                          memoryBean.getCodeCacheUsed(), memoryBean.getCodeCacheFree(),
                          memoryBean.getEdenCommitted(), memoryBean.getEdenMax(),
                          memoryBean.getEdenUsed(), memoryBean.getEdenFree(),
                          memoryBean.getPermGenCommitted(), memoryBean.getPermGenMax(),
                          memoryBean.getPermGenUsed(), memoryBean.getPermGenFree(),
                          memoryBean.getSurvivorCommitted(), memoryBean.getSurvivorMax(),
                          memoryBean.getSurvivorUsed(), memoryBean.getSurvivorFree(),
                          memoryBean.getTenuredCommitted(), memoryBean.getTenuredMax(),
                          memoryBean.getTenuredUsed(), memoryBean.getTenuredFree(),
                          memoryBean.getGarbageCollectionTime(), memoryBean.getGarbageCollectionCount());

      return state;
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public ThreadingInfo getThreadingInfo(String serverId)
  {
    System.err.println(getClass().getSimpleName() + ".getThreadInfo0");

    try {
      ThreadPoolMXBean poolBean
        = MBeanUtil.find("caucho:type=ThreadPool",
                         ThreadPoolMXBean.class,
                         serverId);

      JvmThreadsMXBean jvmBean
        = MBeanUtil.find("caucho:type=JvmThreads",
                         JvmThreadsMXBean.class,
                         serverId);

      int peakThreadCount = getInt(serverId,
                                   "java.lang:type=Threading",
                                   "PeakThreadCount");

      ThreadingInfo info
        = new ThreadingInfo(serverId,
                            poolBean.getThreadActiveCount(),
                            poolBean.getThreadIdleCount(),
                            poolBean.getThreadCount(),
                            jvmBean != null ? jvmBean.getRunnableCount() : -1,
                            jvmBean != null ? jvmBean.getNativeCount() : -1,
                            jvmBean != null ? jvmBean.getBlockedCount() : -1,
                            jvmBean != null ? jvmBean.getWaitingCount() : -1,
                            jvmBean != null ?  jvmBean.getThreadCount() : -1,
                            peakThreadCount);

      return info;

    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public ThreadScoreboard getThreadScoreboard(String serverId)
  {
    System.err.println(getClass().getSimpleName() + ".getThreadScoreboard0");

    try {
      ResinThreadActivityReport report = new ResinThreadActivityReport();
      ThreadActivityGroup []groups = report.execute(true);

      HashMap<String,String> scoreboardMap = new HashMap<String,String>();

      for (ThreadActivityGroup group : groups) {
        String name = group.getName();
        String scoreboard = group.toScoreboard();

        scoreboardMap.put(name, scoreboard);
      }

      ThreadScoreboard threadScoreboard
        = new ThreadScoreboard(scoreboardMap, report.getScoreboardKey());

      return threadScoreboard;
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public ThreadDump []getThreadDumps(String serverId)
  {
    System.err.println(getClass().getSimpleName() + ".getThreadDumps0");

    ServerMXBean serverBean = getServerBean(serverId);

    try {
      ThreadMXBean threadBean = MBeanUtil.find("java.lang:type=Threading",
                                               ThreadMXBean.class);

      long []threadIds = threadBean.getAllThreadIds();
      ThreadDump []dumps = new ThreadDump[threadIds.length];

      for (int i = 0; i < threadIds.length; i++) {
        long id = threadIds[i];

        ThreadInfo info = threadBean.getThreadInfo(id, 64);

        ThreadDump threadDump = new ThreadDump(info, serverBean);

        dumps[i] = threadDump;
      }

      return dumps;
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public Object getAttribute(String serverId, String group, String attrName)
  {
    try {
      ObjectName objectName = getObjectName(group);

      return _mbeanServer.getAttribute(objectName, attrName);
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public Map<String,Object> jmxLookup(String name)
  {
    System.err.println("JmxService.jmxLookup0: " + name);

    try {
      ObjectName objName = getObjectName(name);

      return jmxLookup(objName);
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  private Map<String,Object> jmxLookup(ObjectName name)
  {
    try {
      MBeanInfo info = _mbeanServer.getMBeanInfo(name);

      HashMap<String,Object> attrs = new HashMap<String,Object>();

      attrs.put("ObjectName", name.getCanonicalName());

      for (MBeanAttributeInfo attrInfo : info.getAttributes()) {
        if (! attrInfo.isReadable()) {
          continue;
        }

        String attrName = attrInfo.getName();

        try {
          Object value = _mbeanServer.getAttribute(name, attrName);

          value = toSimpleObject(value);

          attrs.put(attrName, value);
        }
        catch (RuntimeMBeanException e) {
        }
        catch (UnsupportedOperationException e) {
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }

      return attrs;
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  private static Object toSimpleObject(Object obj)
  {
    if (obj instanceof ObjectName) {
      obj = ((ObjectName) obj).getCanonicalName();
    }
    else if (obj instanceof CompositeData) {
      CompositeData data = (CompositeData) obj;
      CompositeType type = data.getCompositeType();

      if (type.isArray()) {
        Collection<?> values = data.values();

        Object []list = new Object[values.size()];

        int i = 0;
        for (Object value : values) {
          value = toSimpleObject(value);

          list[i++] = value;
        }

        obj = list;
      }
      else {
        Set<String> keySet = type.keySet();

        Map<String,Object> map = new HashMap<String,Object>();

        for (String key : keySet) {
          Object value = toSimpleObject(data.get(key));

          map.put(key, value);
        }

        obj = map;
      }
    }

    return obj;
  }

  public String []jmxQueryNames(String query)
  {
    System.err.println("JmxService.queryNames0: " + query);

    try {
      ObjectName pattern = new ObjectName(query);

      Set<ObjectName> names = _mbeanServer.queryNames(pattern, null);

      if (names == null) {
        return null;
      }

      String []nameArray = new String[names.size()];
      Iterator<ObjectName> iter = names.iterator();

      int i = 0;
      while (iter.hasNext()) {
        ObjectName match = iter.next();

        nameArray[i++] = match.getCanonicalName();
      }

      return nameArray;
    } catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  public Map<String,Object> []jmxQueryMBeans(String query)
  {
    System.err.println("JmxService.jmxQueryMBeans0: " + query);

    try {
      ObjectName pattern = new ObjectName(query);

      Set<ObjectName> names = _mbeanServer.queryNames(pattern, null);

      if (names == null) {
        return null;
      }

      Map<String,Object> []list = new Map[names.size()];
      Iterator<ObjectName> iter = names.iterator();

      int i = 0;
      while (iter.hasNext()) {
        ObjectName name = (ObjectName) iter.next();

        Map<String,Object> attrMap = jmxLookup(name);

        list[i++] = attrMap;
      }

      return list;
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }

  private ObjectName getObjectName(String name)
    throws MalformedObjectNameException
  {
    ObjectName objName = new ObjectName(name);

    try {
      if (_mbeanServer.isRegistered(objName)) {
        return objName;
      }
      else if (name.startsWith("caucho:") && name.indexOf("Server=") < 0) {
        // XXX: ResinSystem no longer exists
        //name = name + ",Server=" + ResinSystem.getCurrentId();
  
        return new ObjectName(name);
      }
      else {
        return objName;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return objName;
    }
  }

  private ResinMXBean getResinBean(String serverId)
  {
    if (serverId == null) {
      return _resin;
    }
    else {
      return MBeanUtil.find("caucho:type=Resin", ResinMXBean.class, serverId);
    }
  }

  private ServerMXBean getServerBean(String serverId)
  {
    if (serverId == null) {
      return _server;
    }
    else {
      return MBeanUtil.find("caucho:type=Server", ServerMXBean.class, serverId);
    }
  }

  public static class ServerInfo {
    String _serverId;
    int _serverIndex;

    String _user;
    String _machine;

    String _resinVersion;
    String _jdkVersion;

    String _osVersion;
    //String _license;

    String _watchdogMessage;
    String _state;

    long _startTime;
    long _uptime;

    long _freeHeap = -1;
    long _totalHeap = -1;

    long _fileDescriptors = -1;
    long _maxFileDescriptors = -1;

    long _freeSwap = -1;
    long _totalSwap = -1;

    long _freePhysical = -1;
    long _totalPhysical = -1;

    double _cpuLoadAverage = -1.0;

    protected ServerInfo()
    {
    }
  }

  public static class HealthStatus {
    String _serverId;

    String _status;
    String _message;

    public HealthStatus(String serverId, String status, String message)
    {
      _serverId = serverId;
      _status = status;
      _message = message;
    }
  }

  public static class HealthCheck {
    String _serverId;

    String _name;
    String _status;
    String _message;

    public HealthCheck(String serverId,
                       String name, String status, String message)
    {
      _serverId = serverId;

      _name = name;
      _status = status;
      _message = message;
    }
  }

  public static class HealthLog {
    String _serverId;
    String _type;

    long _timestamp;
    String _source;
    String _message;

    public HealthLog(String serverId,
                     String type, long timestamp, String source, String message)
    {
      _serverId = serverId;

      _type = type;
      _timestamp = timestamp;
      _source = source;
      _message = message;
    }
  }

  public static class MemoryState
  {
    String _serverId;

    long _codeCacheCommitted;
    long _codeCacheMax;
    long _codeCacheUsed;
    long _codeCacheFree;

    long _edenCommitted;
    long _edenMax;
    long _edenUsed;
    long _edenFree;

    long _permGenCommitted;
    long _permGenMax;
    long _permGenUsed;
    long _permGenFree;

    long _survivorCommitted;
    long _survivorMax;
    long _survivorUsed;
    long _survivorFree;

    long _tenuredCommitted;
    long _tenuredMax;
    long _tenuredUsed;
    long _tenuredFree;

    long _garbageCollectionTime;
    long _garbageCollectionCount;

    public MemoryState(String serverId,
                       long codeCacheCommitted, long codeCacheMax,
                       long codeCacheUsed, long codeCacheFree,
                       long edenCommitted, long edenMax,
                       long edenUsed, long edenFree,
                       long permGenCommitted, long permGenMax,
                       long permGenUsed, long permGenFree,
                       long survivorCommitted, long survivorMax,
                       long survivorUsed, long survivorFree,
                       long tenuredCommitted, long tenuredMax,
                       long tenuredUsed, long tenuredFree,
                       long garbageCollectionTime, long garbageCollectionCount)
    {
      _serverId = serverId;

      _codeCacheCommitted = codeCacheCommitted;
      _codeCacheMax = codeCacheMax;
      _codeCacheUsed = codeCacheUsed;
      _codeCacheFree = codeCacheFree;

      _edenCommitted = edenCommitted;
      _edenMax = edenMax;
      _edenUsed = edenUsed;
      _edenFree = edenFree;

      _permGenCommitted = permGenCommitted;
      _permGenMax = permGenMax;
      _permGenUsed = permGenUsed;
      _permGenFree = permGenFree;

      _survivorCommitted = survivorCommitted;
      _survivorMax = survivorMax;
      _survivorUsed = survivorUsed;
      _survivorFree = survivorFree;

      _tenuredCommitted = tenuredCommitted;
      _tenuredMax = tenuredMax;
      _tenuredUsed = tenuredUsed;
      _tenuredFree = tenuredFree;

      _garbageCollectionTime = garbageCollectionTime;
      _garbageCollectionCount = garbageCollectionCount;
    }
  }

  public static class ThreadingInfo
  {
    String _serverId;

    int _activeResinThreads;
    int _idleResinThreads;
    int _totalResinThreads;

    int _runnableJvmThreads;
    int _nativeJvmThreads;
    int _blockedJvmThreads;
    int _waitingJvmThreads;
    int _totalJvmThreads;
    int _peakJvmThreads;

    public ThreadingInfo(String serverId,
                         int activeResinThreads,
                         int idleResinThreads,
                         int totalResinThreads,
                         int runnableJvmThreads,
                         int nativeJvmThreads,
                         int blockedJvmThreads,
                         int waitingJvmThreads,
                         int totalJvmThreads,
                         int peakJvmThreads)
    {
      _serverId = serverId;

      _activeResinThreads = activeResinThreads;
      _idleResinThreads = idleResinThreads;
      _totalResinThreads = totalResinThreads;

      _runnableJvmThreads = runnableJvmThreads;
      _nativeJvmThreads = nativeJvmThreads;
      _blockedJvmThreads = blockedJvmThreads;
      _waitingJvmThreads = waitingJvmThreads;
      _totalJvmThreads = totalJvmThreads;
      _peakJvmThreads = peakJvmThreads;
    }
  }

  public static class ThreadScoreboard
  {
    private Map<String,String> _scoreboardMap;
    private Map<Character,String> _keyMap;

    public ThreadScoreboard(Map<String,String> scoreboardMap,
                            Map<Character,String> keyMap)
    {
      _scoreboardMap = scoreboardMap;
      _keyMap = keyMap;
    }
  }

  public static class ThreadDump
  {
    long _id;
    String _name;

    String _state;
    boolean _isNative;
    String _stackTrace;

    boolean _isIdlePoolThread;
    boolean _isKeepAliveThread;

    String _appClassName;
    String _appMethodName;

    String _url;

    public ThreadDump(ThreadInfo info, ServerMXBean serverBean)
    {
      _id = info.getThreadId();
      _name = info.getThreadName();

      _state = info.getThreadState().name();
      _isNative = info.isInNative();

      StackTraceElement []stackList = info.getStackTrace();

      _stackTrace = buildStackTrace(info, stackList);

      if (stackList != null) {
        for (StackTraceElement e : stackList) {
          if (ThreadAmp.class.getName().equals(e.getClassName())
              && "park".equals(e.getMethodName())) {
            _isIdlePoolThread = true;

            break;
          }
        }

        for (StackTraceElement e : stackList) {
          String className = e.getClassName();

          if ("sun.misc.Unsafe".equals(className)
              || LockSupport.class.getName().equals(className)
              || Object.class.getName().equals(className)) {
            continue;
          }

          _appClassName = className;
          _appMethodName = e.getMethodName();

          TcpConnectionInfo connInfo = serverBean.findConnectionByThreadId(_id);

          if (connInfo != null && connInfo.hasRequest()) {
            _url = connInfo.getUrl();
          }

          break;
        }
      }

    }

    private static String buildStackTrace(ThreadInfo info,
                                          StackTraceElement []stackList)
    {
      StringBuilder sb = new StringBuilder();

      sb.append(info.getThreadName());
      sb.append("\" id=" + info.getThreadId());
      sb.append(" " + info.getThreadState());

      if (info.isInNative()) {
        sb.append(" (in native)");
      }

      if (info.isSuspended()) {
        sb.append(" (suspended)");
      }

      String lockName = info.getLockName();
      if (lockName != null) {
        sb.append("\n    waiting on ");
        sb.append(lockName);

        if (info.getLockOwnerName() != null) {
          sb.append("\n    owned by \"");
          sb.append(info.getLockOwnerName());
          sb.append("\"");
        }
      }

      sb.append("\n");

      if (stackList != null) {
        for (StackTraceElement stack : stackList) {
          sb.append("  at ");
          sb.append(stack.getClassName());
          sb.append(".");
          sb.append(stack.getMethodName());

          if (stack.getFileName() != null) {
            sb.append(" (");
            sb.append(stack.getFileName());

            if (stack.getLineNumber() > 0) {
              sb.append(":");
              sb.append(stack.getLineNumber());
            }

            sb.append(")");
          }

          if (stack.isNativeMethod()) {
            sb.append(" (native)");
          }

          sb.append("\n");
        }
      }

      return sb.toString();
    }
  }
}
