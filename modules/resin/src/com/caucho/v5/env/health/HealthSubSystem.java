/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.config.InlineConfig;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.action.HealthAction;
import com.caucho.v5.health.check.BasicHealthChecks;
import com.caucho.v5.health.check.HealthCheck;
import com.caucho.v5.health.check.SystemHealthCheck;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.event.RestartHealthEvent;
import com.caucho.v5.health.event.ScheduledCheckHealthEvent;
import com.caucho.v5.health.event.StartHealthEvent;
import com.caucho.v5.health.event.StopHealthEvent;
import com.caucho.v5.health.meter.MeterService;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.lifecycle.LifecycleListener;
import com.caucho.v5.lifecycle.LifecycleState;
import com.caucho.v5.server.container.ServerBaseOld;
import com.caucho.v5.subsystem.SubSystemBase;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.QDate;
import com.caucho.v5.web.server.StartInfoListener;

@InlineConfig
public class HealthSubSystem extends SubSystemBase
{
  private static final Logger log 
    = Logger.getLogger(HealthSubSystem.class.getName());
  private static final L10N L = new L10N(HealthSubSystem.class);
  
  public static final int START_PRIORITY = 100;
  public static final String METER_PREFIX = "Caucho|Health|";
  
  public static final long DEFAULT_SYSTEM_RECHECK_TIMEOUT = 10 * 60 * 1000L;
  
  public static final String CHECK_LOG_TYPE = "Caucho|Health|Check";
  public static final String RECHECK_LOG_TYPE = "Caucho|Health|Recheck";
  public static final String RECOVER_LOG_TYPE = "Caucho|Health|Recover";
  public static final String ACTION_LOG_TYPE = "Caucho|Health|Action";
  
  private LogSystem _logSystem;
  
  private String _checkLogType;
  private String _recheckLogType;
  private String _recoverLogType;
  private String _actionLogType;
  
  private boolean _enabled = true;
  private long _delay = 15 * 60 * 1000L;
  private long _period = 5 * 60 * 1000L;
  private long _recheckPeriod = 30 * 1000L;
  private int _recheckMax = 10;
  private long _checkTimeout = 5 * 60 * 1000L;
  
  // default timeouts for recheck rules
  private long _systemRecheckTimeout;

  private Lifecycle _lifecycle;

  private List<HealthCheck> _checks
    = new CopyOnWriteArrayList<HealthCheck>();
  
  private List<HealthAction> _actions
    = new CopyOnWriteArrayList<HealthAction>();
  
  private ConcurrentMap<HealthCheck, RecheckResult> _recheckMap
    = new ConcurrentHashMap<HealthCheck, RecheckResult>();
  
  private Map<HealthCheck, HealthCheckResult> _lastResultsMap
    = new ConcurrentHashMap<HealthCheck, HealthCheckResult>();
  
  private Map<HealthCheck, HealthMeter> _healthMeterMap
  = new ConcurrentHashMap<HealthCheck, HealthMeter>();

  private HealthCheckScheduler _scheduler;
  
  private long _serviceStartTime;
  
  private long _lastCheckStartTime;
  private long _lastCheckFinishTime;
  private int _currentRecheckCount = 0;
  
  private HealthCheckResult _summaryResult;
  
  private SystemHealthCheck _resinHealth;
  
  @SuppressWarnings("unused")
  private HealthSystemAdmin _admin;
  
  private HealthSubSystem()
  {
    _lifecycle = new Lifecycle();
    
    _scheduler = new HealthCheckScheduler();
  }
  
  public static HealthSubSystem createAndAddSystem()
  {
    SystemManager system = preCreate(HealthSubSystem.class);
      
    HealthSubSystem service = new HealthSubSystem();
    system.addSystem(HealthSubSystem.class, service);
    
    return service;
  }
  
  public static HealthSubSystem getCurrent()
  {
    return SystemManager.getCurrentSystem(HealthSubSystem.class);
  }
  
  //
  // attributes
  //
  
  public boolean isEnabled()
  {
    return _enabled;
  }

  public void setEnabled(boolean enabled)
  {
    _enabled = enabled;
  }

  public long getDelay()
  {
    return _delay;
  }

  public void setDelay(long delay)
  {
    _delay = delay;
  }

  public long getPeriod()
  {
    return _period;
  }

  public void setPeriod(long period)
  {
    _period = period;
  }
  
  public long getCheckTimeout()
  {
    return _checkTimeout;
  }
  
  public void setCheckTimeout(long timeout)
  {
    _checkTimeout = timeout;
  }

  public long getRecheckPeriod()
  {
    return _recheckPeriod;
  }

  public void setRecheckPeriod(long recheckPeriod)
  {
    _recheckPeriod = recheckPeriod;
  }
  
  public int getRecheckMax()
  {
    return _recheckMax;
  }
  
  public void setRecheckMax(int recheckMax)
  {
    _recheckMax = recheckMax;
  }
  
  public void setSystemRecheckTimeout(long systemRecheckTimeout)
  {
    _systemRecheckTimeout = systemRecheckTimeout;
  }
  
  public long getSystemRecheckTimeout()
  {
    if (_systemRecheckTimeout > 0)
      return _systemRecheckTimeout;
    else
      return _recheckPeriod * _recheckMax;
  }

  //
  // checks
  //

  public void addHealthCheck(HealthCheck healthCheck)
  {
    _checks.add(healthCheck);
  }

  public <T extends HealthCheck> T getHealthCheck(Class<T> cl)
  {
    List<T> list = getHealthChecks(cl);
    
    if (list.isEmpty())
      return null;
    else
      return list.get(0);
  }
  
  @SuppressWarnings("unchecked")
  public <T extends HealthCheck> List<T> getHealthChecks(Class<T> cl)
  {
    List<HealthCheck> list = new ArrayList<HealthCheck>();
    
    for (HealthCheck check : _checks) {
      if (check.getClass().equals(cl))
        list.add(check);
    }
    
    return (List<T>)list;
  }
  
  public boolean containsHealthCheck(HealthCheck check)
  {
    return _checks.contains(check);
  }

  public <T extends HealthCheck> boolean containsHealthCheck(Class<T> cl)
  {
    for (HealthCheck check : _checks) {
      if (check.getClass().equals(cl)) {
        return true;
      }
    }
    
    return false;
  }

  public List<HealthCheck> getHealthChecks()
  {
    return _checks;
  }
  
  //
  // actions
  //
  
  @SuppressWarnings("unchecked")
  public synchronized <I extends HealthAction> I addHealthAction(I action)
  {
    int index = _actions.indexOf(action);
    if (index >= 0)
      return (I) _actions.get(index);
    
    _actions.add(action);

    // TODO: register w/ JMX
//    HealthActionAdmin admin = new HealthActionAdmin(action);
//    admin.register();
    
    return action;
  }
  
  public <T extends HealthAction> T getHealthAction(Class<T> cl)
  {
    List<T> list = getHealthActions(cl);
    
    if (list.isEmpty())
      return null;
    else
      return list.get(0);
  }
  
  @SuppressWarnings("unchecked")
  public <T extends HealthAction> List<T> getHealthActions(Class<T> cl)
  {
    List<HealthAction> list = new ArrayList<HealthAction>();
    
    for (HealthAction action : _actions) {
      if (action.getClass().equals(cl))
        list.add(action);
    }
    
    return (List<T>)list;
  }
  
  public List<HealthAction> getHealthActions()
  {
    return _actions;
  }
  
  //
  // Service API
  //
  
  public LifecycleState getLifecycleState()
  {
    return _lifecycle.getState();
  }
  
  public void addLifecycleListener(LifecycleListener listener)
  {
    _lifecycle.addListener(listener);
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }

  @Override
  public void start()
  {
    if (! _lifecycle.toStarting())
      return;
    
    _serviceStartTime = CurrentTime.getCurrentTime();

    BasicHealthChecks basicHealth = new BasicHealthChecks();
    
    boolean isHealthConfigured = true;
    if (_checks.isEmpty() && _actions.isEmpty()) {
      isHealthConfigured = false;
    }
    
    if (! CurrentTime.isTest()) {
      basicHealth.initStandardHealthChecks();
    }
    
    if (! isHealthConfigured && ! CurrentTime.isTest()) {
      log.config(L.l("No health checks are configured; setting up basic checks..."));
      basicHealth.initDefaultActions();
    }
    
    _logSystem = LogSystem.getCurrent();
    if (_logSystem != null) {
      _checkLogType = _logSystem.createFullType(CHECK_LOG_TYPE);
      _recheckLogType = _logSystem.createFullType(RECHECK_LOG_TYPE);
      _recoverLogType = _logSystem.createFullType(RECOVER_LOG_TYPE);
      _actionLogType = _logSystem.createFullType(ACTION_LOG_TYPE);
    }

    _resinHealth = getHealthCheck(SystemHealthCheck.class);
    
    if (_resinHealth == null) {
      _resinHealth = new SystemHealthCheck();
      _resinHealth.init();
    }
    
    registerChecks();
    
    for (HealthCheck check : _checks) {
      check.start();
    }
    
    for (HealthAction action : _actions) {
      action.start();
    }

    _scheduler.start();
    
    _admin = new HealthSystemAdmin(this);
    
    _lifecycle.toActive();
    
    ServerBaseOld resin = ServerBaseOld.current();
    if (resin != null) {
      resin.addStartInfoListener(new ResinRestartListener());
    }

    fireEvent(new StartHealthEvent(this));
  }

  @Override
  public void stop(ShutdownModeAmp mode)
  {
    if (! _lifecycle.toStopping())
      return;
    
    try {
      executeActions(new StopHealthEvent(this));
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    _scheduler.stop();

    for (HealthCheck check : _checks) {
      try {
        check.stop();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
    
    for (HealthAction action : _actions) {
      try {
        action.stop();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
    
    _lifecycle.toStop();
  }

  @Override
  public void destroy()
  {
    _lifecycle.toDestroy();
  }
  
  private void registerChecks()
  {
    MeterService meterService = MeterService.getCurrent();
    
    for (HealthCheck healthCheck : _checks) {
      try {
        String checkName = healthCheck.getName();
        if (checkName == null) {
          checkName = healthCheck.getClass().getSimpleName();
        }
        
        String publicName = null;
        
        if (checkName.endsWith("HealthCheck")) {
          int prefixLen = checkName.length() - "HealthCheck".length();
          publicName = "Resin|" + checkName.substring(0, prefixLen);
        } else if (checkName.endsWith("HealthCheckImpl")) {
          int prefixLen = checkName.length() - "HealthCheckImpl".length();
          publicName = "Resin|" + checkName.substring(0, prefixLen);
        } else if (checkName.equals(ResinHealthCheckImpl.NAME)) {
          // special case for BC
          publicName = checkName;
        } else {
          publicName = "Resin|" + checkName;
        }
        
        HealthCheckAdmin checkAdmin = 
          new HealthCheckAdmin(publicName, healthCheck, this);
        checkAdmin.register();
  
        String meterName = "Caucho|Health|" + publicName;

        if (meterService != null) {
          HealthMeter meter = new HealthMeter(meterName);
          meter = (HealthMeter) meterService.createMeter(meter);
          _healthMeterMap.put(healthCheck, meter);
        }
        
        if (log.isLoggable(Level.FINER)) {
          log.finer(L.l("registered health check '{0}', admin name '{1}', meter name '{2}'",
                       healthCheck.getName(),
                       publicName,
                       meterName));
        }
      } catch (Exception e) {
        log.log(Level.WARNING, L.l("failed to registered health check '{0}': {2}",
                                   healthCheck.getName(),
                                   e.getMessage()), e);
      }
    }
  }
  
  //
  // checking
  //
  
  public HealthCheckResult getLastResult(HealthCheck healthCheck)
  {
    if (healthCheck != null)
      return _lastResultsMap.get(healthCheck);
    else
      return _summaryResult;
  }
  
  public long getLastCheckStartTime()
  {
    return _lastCheckStartTime;
  }

  public long getLastCheckFinishTime()
  {
    return _lastCheckFinishTime;
  }
  
  public int getCurrentRecheckCount()
  {
    return _currentRecheckCount;
  }
  
  public HealthCheckResult getSummaryResult()
  {
    return _summaryResult;
  }
  
  public HealthMeter getHealthMeter(HealthCheck healthCheck)
  {
    return _healthMeterMap.get(healthCheck);
  }
  
  public boolean isStartupDelayExpired(long time)
  {
    return (_serviceStartTime + getDelay() <= time);
  }
  
  public HealthCheckResult checkHealth()
  {
    long now = CurrentTime.getCurrentTime();
    
    HealthCheckSummary summary = new HealthCheckSummary(now);

    for (HealthCheck check : _checks) {
      if (check instanceof SystemHealthCheck)
        continue;
      
      HealthCheckResult result = null;

      if (check.isEnabled()) {
        try {
          result = check.checkHealth();
        } catch (Throwable e) {
          log.log(Level.FINER, e.toString(), e);
          
          result = new HealthCheckResult(HealthStatus.UNKNOWN, e.toString());
          result.setException(e);
        }
        
        processHealthResult(check, result, summary);
      }
    }
    
    StringBuilder sb = new StringBuilder();
    
    if (summary.getFatalCount() > 0) {
      sb.append(L.l("{0} fatal", summary.getFatalCount()));
    }
    
    if (summary.getCriticalCount() > 0) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      
      sb.append(L.l("{0} critical", summary.getCriticalCount()));
    }
    
    if (summary.getWarningCount() > 0) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      
      sb.append(L.l("{0} warning", summary.getWarningCount()));
    }
    
    if (sb.length() > 0) {
      sb.append(", ");
    }
      
    sb.append(L.l("{0} active checks", summary.getActiveCount()));
    
    for (HealthCheckResult subResult : summary.getFatalList()) {
      sb.append("\n  Fatal: " + subResult.getMessage());
    }
    
    for (HealthCheckResult subResult : summary.getCriticalList()) {
      sb.append("\n  Critical: " + subResult.getMessage());
    }

    String msg = sb.toString();
    
    HealthCheckResult result = new HealthCheckResult(summary.getStatus(), msg);
    result.setRecover(summary.isRecovered());
    _lastResultsMap.put(_resinHealth, result);
    
    _summaryResult = result;
    
    processHealthResult(_resinHealth, result, summary);
    
    return _summaryResult;
  }
  
  private void processHealthResult(HealthCheck check,
                                   HealthCheckResult result,
                                   HealthCheckSummary summary)
  {
    if (result == null) {
      log.fine(L.l("{0} returned no check result", check));
      return;
    }
    
    HealthStatus resultStatus = result.getStatus();

    RecheckResult recheckResult;

    long now = summary.getNow();
      
    summary.updateStatus(resultStatus);
    summary.addActive();

    if (check != _resinHealth) {
      check.logResult(result, log);
      if (! result.isOk()) {
        logCheck(check, result);
      }
    }

    switch (resultStatus) {
      case WARNING:
        summary.addWarning(result);
  
        recheckResult = addRecheck(check, now);
        recheckResult.startWarning(now);
        break;
  
      case CRITICAL:
        summary.addCritical(result);
  
        recheckResult = addRecheck(check, now);
        recheckResult.startCritical(now);
        break;
  
      case FATAL:
        summary.addFatal(result);

        recheckResult = addRecheck(check, now);
        recheckResult.startFatal(now);
        break;
  
      default:
    }

    if (result.isOk()) {
      RecheckResult startOfFailure = _recheckMap.remove(check);

      if (startOfFailure != null) {
        long et = result.getTimestamp() - startOfFailure.getStartTime();

        if (check != _resinHealth) {
          String msg = L.l("{0} recovered after {1} ms", 
                           check.getName(), et);
          log.info(msg);
          
          logRecover(check, msg);
        }

        summary.setRecovered();
      }
    }
    
    _lastResultsMap.put(check, result);
    
    HealthMeter meter = _healthMeterMap.get(check);
    if (meter != null)
      meter.updateStatus(resultStatus);
  }
  
  public long getWarningStartTime(HealthCheck check)
  {
    if (check == null)
      check = _resinHealth;
    
    RecheckResult result = _recheckMap.get(check);
    
    if (result != null)
      return result.getWarningStartTime();
    else
      return 0;
  }
  
  public long getCriticalStartTime(HealthCheck check)
  {
    if (check == null)
      check = _resinHealth;
    
    RecheckResult result = _recheckMap.get(check);
    
    if (result != null)
      return result.getCriticalStartTime();
    else
      return 0;
  }
  
  public long getFatalStartTime(HealthCheck check)
  {
    if (check == null)
      check = _resinHealth;
    
    RecheckResult result = _recheckMap.get(check);
    
    if (result != null)
      return result.getFatalStartTime();
    else
      return 0;
  }
  
  public int getWarningCount(HealthCheck check)
  {
    if (check == null)
      check = _resinHealth;
    
    RecheckResult result = _recheckMap.get(check);
    
    if (result != null)
      return result.getWarningRecheckCount();
    else
      return 0;
  }
  
  public int getCriticalCount(HealthCheck check)
  {
    if (check == null)
      check = _resinHealth;
    
    RecheckResult result = _recheckMap.get(check);
    
    if (result != null)
      return result.getCriticalRecheckCount();
    else
      return 0;
  }
  
  public int getFatalCount(HealthCheck check)
  {
    if (check == null)
      check = _resinHealth;
    
    RecheckResult result = _recheckMap.get(check);
    
    if (result != null)
      return result.getFatalRecheckCount();
    else
      return 0;
  }
  
  private RecheckResult addRecheck(HealthCheck healthCheck, long now)
  {
    RecheckResult result = new RecheckResult(now);

    // don't update during startup
    if (! isStartupDelayExpired(now))
      return result;
    
    RecheckResult oldResult = _recheckMap.putIfAbsent(healthCheck, result);
    
    if (oldResult != null)
      return oldResult;
    else
      return result;
  }
  
  private void logCheck(HealthCheck check, HealthCheckResult result)
  {
    if (_checkLogType != null) {
      _logSystem.log(_checkLogType, 
                     check.getName(), 
                     Level.INFO, 
                     result.getDescription());
    }
  }
  
  private void logRecover(HealthCheck check, String message)
  {
    if (_recoverLogType != null) {
      _logSystem.log(_recoverLogType, 
                     check.getName(), 
                     Level.INFO, 
                     message);
    }
  }
  
  private void logAction(HealthAction action, HealthActionResult result)
  {
    if (_actionLogType != null) {
      _logSystem.log(_actionLogType, 
                     action.getClass().getSimpleName(),
                     Level.INFO, 
                     result.getDescription());
    }
  }

  private void logRecheck(HealthCheck check, String message)
  {
    if (_recheckLogType != null) {
      _logSystem.log(_recheckLogType, 
                     check.getName(), 
                     Level.INFO, 
                     message);
    }
  }

  public void fireEvent(String eventName)
  {
    fireEvent(new HealthEvent(this, eventName));
  }
  
  public void fireEvent(HealthEvent event)
  {
    ThreadPool.current().schedule(new ExecuteActionsTask(event));
    
    // executeActions(event);
  }
  
  public void executeActions(HealthEvent event)
  {
    for (HealthAction action : _actions) {
      HealthActionResult result = action.doAction(event);
      
      if (! result.isSkipped()) {
        Level level = result.isFailure() ? Level.WARNING : Level.INFO;
        log.log(level, L.l("{0} {1}",
                           action.toString(),
                           result.getDescription()));
        
        logAction(action, result);
      }
    }
  }
  
  private class ResinRestartListener implements StartInfoListener
  {
    @Override
    public void setStartInfo(boolean isRestart,
                             String startMessage,
                             ExitCode exitCode)
    {
      // PWC: StartHealthEvent is fired in HealthService.init
      // can not fire restart from there because start info not available yet
      if (isRestart) {
        RestartHealthEvent event = new RestartHealthEvent(HealthSubSystem.this,
                                                          startMessage,
                                                          exitCode);
        fireEvent(event);
      }
    }
  }
  
  protected class HealthCheckScheduler implements AlarmListener
  {
    private Alarm _alarm;
    
    protected void start()
    {
      long now = CurrentTime.getExactTime();
      
      _lastCheckStartTime = now;
      _lastCheckFinishTime = now;
      
      _alarm = new Alarm(toString(), this);
      
      handleAlarm(_alarm);
    }

    protected void stop()
    {
      if (_alarm != null)
        _alarm.dequeue();
    }
    
    @Override
    public void handleAlarm(Alarm alarm)
    {
      long now = CurrentTime.getCurrentTime();
      _lastCheckStartTime = now;
      
      long alarmPeriod = getPeriod();
      
      try {
        alarmPeriod = handleAlarmImpl(alarm, now);
      } finally {
        if (! _lifecycle.isAfterStopping() && isEnabled()) {
          if (alarmPeriod < 1000) {
            alarmPeriod = 1000;
          }
          
          now = CurrentTime.getCurrentTime();
          
          long wakeTime = now + alarmPeriod;
          
          wakeTime = wakeTime - (wakeTime % alarmPeriod);
          
          if (wakeTime < now)
            wakeTime = now + alarmPeriod;
          
          alarm.queueAt(wakeTime);
        }
        
      }

    }
    
    private long handleAlarmImpl(Alarm alarm, long now)
    {
      if (_lifecycle.isAfterStopping() || ! isEnabled()) {
        _lastCheckFinishTime = now;
        return getPeriod();
      }
      
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("health checking at {0}",
                                 QDate.formatLocal(now)));

      checkHealth();
      
      HealthStatus summaryStatus = _summaryResult.getStatus();
      
      now = CurrentTime.getCurrentTime();
      _lastCheckFinishTime = now;
      
      boolean isRecheck = false;
      boolean resetRecheckCount = false;

      if (summaryStatus.compareTo(HealthStatus.CRITICAL) >= 0 
        && isStartupDelayExpired(now)) {
        
        log.warning(L.l("{0} status, recheck {1} of {2}",
                        _summaryResult.getDescription(),
                        _currentRecheckCount,
                        _recheckMax));
        
        logRecheck(_resinHealth, L.l("{0}, recheck {1} of {2}",
                                     _summaryResult.getDescription(),
                                     _currentRecheckCount,
                                     _recheckMax));
        
        if (_recheckMax <= _currentRecheckCount)
          resetRecheckCount = true;
          
        isRecheck = true;
      } else {
        resetRecheckCount = true;
        log.finer(L.l("status: {0}",
                      _summaryResult));
      }
      
      if (_lifecycle.isAfterStopping() || ! isEnabled())
        return getPeriod();
      
      if (isStartupDelayExpired(now)) {
        executeActions(new ScheduledCheckHealthEvent(HealthSubSystem.this));
      }
      
      if (resetRecheckCount)
        _currentRecheckCount = 0;
      else
        _currentRecheckCount++;
      
      if (isRecheck) {
        log.fine(L.l("scheduling health check retry in {0} ms",  
                      getRecheckPeriod()));
        return getRecheckPeriod();
      } else {
        return getPeriod();
      }
    }
  }
  
  static class RecheckResult {
    private long _timestamp;
    
    private long _startWarningTime;
    private long _startCriticalTime;
    private long _startFatalTime;
    
    private int _warningRecheckCount;
    private int _criticalRecheckCount;
    private int _fatalRecheckCount;
    
    RecheckResult(long now)
    {
      _timestamp = now;
    }
    
    public long getStartTime()
    {
      return _timestamp;
    }
    
    public void startWarning(long now)
    {
      if (_startWarningTime <= 0)
        _startWarningTime = now;
      
      _warningRecheckCount++;
      
      _startCriticalTime = 0;
      _criticalRecheckCount = 0;
      
      _startFatalTime = 0;
      _fatalRecheckCount = 0;
    }
    
    public long getWarningStartTime()
    {
      return _startWarningTime;
    }
    
    public int getWarningRecheckCount()
    {
      return _warningRecheckCount;
    }
    
    public void startCritical(long now)
    {
      if (_startWarningTime <= 0)
        _startWarningTime = now;
      
      _warningRecheckCount++;
      
      if (_startCriticalTime <= 0)
        _startCriticalTime = now;

      _criticalRecheckCount++;
      
      _startFatalTime = 0;
      _fatalRecheckCount = 0;
    }
    
    public long getCriticalStartTime()
    {
      return _startCriticalTime;
    }
    
    public int getCriticalRecheckCount()
    {
      return _criticalRecheckCount;
    }
    
    public void startFatal(long now)
    {
      if (_startWarningTime <= 0)
        _startWarningTime = now;
      
      _warningRecheckCount++;
      
      if (_startCriticalTime <= 0)
        _startCriticalTime = now;
      
      _criticalRecheckCount++;
      
      if (_startFatalTime <= 0)
        _startFatalTime = now;
      
      _fatalRecheckCount++;
    }
    
    public long getFatalStartTime()
    {
      return _startFatalTime;
    }
    
    public int getFatalRecheckCount()
    {
      return _fatalRecheckCount;
    }
  }
  
  static class HealthCheckSummary {
    private long _now;
    
    private HealthStatus _summary = HealthStatus.UNKNOWN;
    private int _activeCount;
    private int _warningCount;
    private int _criticalCount;
    private int _fatalCount;
    private boolean _isRecovered;
    
    private ArrayList<HealthCheckResult> _fatalList = new ArrayList<>();
    private ArrayList<HealthCheckResult> _criticalList = new ArrayList<>();
    private ArrayList<HealthCheckResult> _warningList = new ArrayList<>();
    
    HealthCheckSummary(long now)
    {
      _now = now;
    }

    public long getNow()
    {
      return _now;
    }
    
    public HealthStatus getStatus()
    {
      return _summary;
    }
    
    public void updateStatus(HealthStatus status)
    {
      if (_summary.compareTo(status) < 0)
        _summary = status;
    }
    
    public void addActive()
    {
      _activeCount++;
    }
    
    public int getActiveCount()
    {
      return _activeCount;
    }
    
    public void addWarning(HealthCheckResult result)
    {
      _warningCount++;
      
      _warningList.add(result);
    }
    
    public int getWarningCount()
    {
      return _warningCount;
    }
    
    public Iterable<HealthCheckResult> getWarningList()
    {
      return _warningList;
    }
    
    public void addCritical(HealthCheckResult result)
    {
      _criticalCount++;
      
      _criticalList.add(result);
    }
    
    public int getCriticalCount()
    {
      return _criticalCount;
    }
    
    public Iterable<HealthCheckResult> getCriticalList()
    {
      return _criticalList;
    }
    
    public void addFatal(HealthCheckResult result)
    {
      _fatalCount++;
      
      _fatalList.add(result);
    }
    
    public int getFatalCount()
    {
      return _fatalCount;
    }
    
    public Iterable<HealthCheckResult> getFatalList()
    {
      return _fatalList;
    }
    
    public void setRecovered()
    {
      _isRecovered = true;
    }
    
    public boolean isRecovered()
    {
      return _isRecovered;
    }
  }
  
  class ExecuteActionsTask implements Runnable {
    private HealthEvent _event;
    
    ExecuteActionsTask(HealthEvent event)
    {
      _event = event;
    }
    
    @Override
    public void run()
    {
      executeActions(_event);
    }
  }
}
