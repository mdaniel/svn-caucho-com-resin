/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.profile.Profile;
import com.caucho.v5.profile.ProfileEntry;
import com.caucho.v5.profile.ProfileReport;
import com.caucho.v5.profile.StackEntry;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Health action to start a profiler session.  Results are logged to the 
 * the internal log database and the resin log file at INFO level.
 * <p>
 * <pre>{@code
 * <health:ActionSequence>
 *   <health:FailSafeRestart timeout="10m">
 *   <health:DumpThreads/>
 *   <health:DumpHeap/>
 *   <health:StartProfiler active-time="5m"/>
 *   <health:Restart/>
 *   
 *   <health:IfHealthCritical time="5m"/>
 * </health:ActionSequence>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class StartProfiler extends HealthActionBase
{
  private static final L10N L = new L10N(StartProfiler.class);
  private static final Logger log = 
    Logger.getLogger(StartProfiler.class.getName());
  
  private long _activeTime = 5000L;
  private long _samplingRate = 10L;
  private int _depth = 16;
  
  public static final String LOG_TYPE = "Resin|Profile";
  
  private LogSystem _logSystem;
  private String _logType;
  
  private boolean _isWait;
  
  private ProfilerTask _task; 
  private AtomicLong _cancelledTime = new AtomicLong(-1);
  
  @PostConstruct
  public void init()
  {
    _logSystem = LogSystem.getCurrent();

    super.init();
  }
  
  public long getActiveTime()
  {
    return _activeTime;
  }
  
  @Configurable
  public void setActiveTime(Period activeTime)
  {
    setActiveTimeMillis(activeTime.getPeriod());
  }
  
  @Configurable
  public void setActiveTimeMillis(long activeTime)
  {
    _activeTime = activeTime;
  }
  
  public long getSamplingRate()
  {
    return _samplingRate;
  }
  
  public void setWait(boolean isWait)
  {
    _isWait = isWait;
  }
  
  @Configurable
  public void setSamplingRate(Period samplingRate)
  {
    setSamplingRateMillis(samplingRate.getPeriod());
  }
  
  @Configurable
  public void setSamplingRateMillis(long samplingRate)
  {
    _samplingRate = samplingRate;
  }
  
  public int getDepth()
  {
    return _depth;
  }

  @Configurable
  public void setDepth(int depth)
  {
    _depth = depth;
  }
  
  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    if (_task != null && _task.isAlive()) {
      return new HealthActionResult(ResultStatus.FAILED, 
                                    L.l("Profiler is already active"));
    }
    
    _task = new ProfilerTask();
  
    if (_isWait)
      _task.run();
    else
      ThreadPool.getCurrent().schedule(_task);
    
    return new HealthActionResult(ResultStatus.OK, L.l("Profiler started"));
  }

  public void startProfile()
  {
    if (_task == null)
      _task = new ProfilerTask();
    
    _task.start();
  }
  
  @Override
  public void stop()
  {
    if (_task != null && _task.isAlive()) {
      _task.cancel();
      
      // poll for a short time to give ProfilerThread a chance to output results 
      for (int i = 0; i < 50; i++) {
        if (! _task.isAlive())
          break;
          
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          break;
        }
      }
    }
  }

  public String jsonProfile()
  {
    ProfilerTask task = _task;

    if (task != null)
      return task.jsonProfile();
    else
      return null;
  }
  
  public void cancel()
  {
    _cancelledTime.compareAndSet(-1, CurrentTime.getCurrentTime());
    
    synchronized(this) {
      this.notify();
    }

    Profile profile = Profile.create();
    
    if (profile != null)
      profile.stop();
  }
  
  public void start(long samplingRate, int depth)
  {
    Profile profile = Profile.create();

    profile.stop();

    profile.setPeriod(samplingRate);
    profile.setDepth(depth);

    profile.start();
  }
  
  public String execute(long activeTime, long samplingRate, int depth)
    throws ConfigException
  {
    if (activeTime <= 0) {
      throw new IllegalArgumentException(L.l("Profile activeTime '{0}': must be > 0.",
                                             activeTime));
    }
    
    Profile profile = Profile.create();

    if (profile.isActive()) {
      throw new ConfigException(L.l("Profile is still active"));
    }
    
    long startedAt = CurrentTime.getCurrentTime();
    
    start(samplingRate, depth);
    
    try {
      synchronized (this) {
        this.wait(activeTime);
      }
    } catch (InterruptedException e) {
      _cancelledTime.compareAndSet(-1, CurrentTime.getCurrentTime());
    }

    profile.stop();

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer);

    ProfileReport report = profile.report();
    ProfileEntry []entries = report.getEntries();

    if (entries == null || entries.length == 0) {
      out.println("Profile returned no entries.");
    }
    else {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      
      long cancelledTime = _cancelledTime.get();

      if (cancelledTime < 0) {
        out.print(L.l("Profile started at {0}. Active for a total of {1}ms.",
                      dateFormat.format(new Date(startedAt)),
                      activeTime));
      }
      else {
        
        long et = cancelledTime - startedAt;
        
        out.print(L.l("Profile started at {0}, cancelled at {1}. Active for a total of {2}ms.",
                      dateFormat.format(new Date(startedAt)),
                      dateFormat.format(new Date(cancelledTime)),
                      et));
      }

      out.println(L.l(" Sampling rate {0}ms. Depth {1}.",
                      samplingRate,
                      String.valueOf(depth)));

      double totalTicks = 0;
      for (ProfileEntry entry : entries) {
        totalTicks += entry.getCount();
      }

      final double sampleTicks = report.getTicks();
      double totalPercent = 0d;

      out.println(" ref# |   % time   |time self(s)|   % sum    | Method Call");
      for (int i = 0; i < entries.length; i++) {
        ProfileEntry entry = entries[i];
        double timePercent = (double) 100 * (double) entry.getCount()
                             / sampleTicks;
        double selfPercent = (double) 100 * (double) entry.getCount()
                             / totalTicks;
        totalPercent += selfPercent;

        out.println(String.format(" %4d | %10.3f | %10.3f | %10.3f | %s",
                                  i,
                                  timePercent,
                                  (float) entry.getCount() * samplingRate * 0.001,
                                  totalPercent,
                                  entry.getDescription()));

      }

      for (int i = 0; i < entries.length; i++) {
        ProfileEntry entry = entries[i];
        out.print(String.format(" %4d ", i));
        out.println(" " + entry.getDescription());
        ArrayList<? extends StackEntry> stackEntries = entry.getStackTrace();
        for (StackEntry stackEntry : stackEntries) {
          out.println("         " + stackEntry.getDescription());
        }
      }
    }

    out.flush();

    return buffer.toString();
  }

  /**
   * @return
   */
  public String jsonProfileImpl()
  {
    Profile profile = Profile.create();
    
    if (profile == null)
      return null;
    
    ProfileReport report = profile.report();
    
    if (report != null) {
      return report.getJson();
    }
    else {
      return null;
    }
  }

  private class ProfilerTask implements Runnable
  {
    private volatile boolean _isAlive = false;
    
    public boolean isAlive()
    {
      return _isAlive;
    }
    
    public void start()
    {
      StartProfiler.this.start(_samplingRate, _depth);
    }
    
    public void cancel()
    {
      StartProfiler.this.cancel();
    }
    
    public String jsonProfile()
    {
      return StartProfiler.this.jsonProfileImpl();
    }
    
    @Override
    public void run()
    {
      _isAlive = true;
      
      Thread thread = Thread.currentThread();
      String oldName = thread.getName();
      
      try {
        thread.setName("health-profiler-thread");
        
        // execute is a blocking call
        String result = execute(_activeTime, 
                                        _samplingRate, 
                                        _depth);
        log.log(Level.INFO, L.l("{0} {1}", 
                                this.getClass().getSimpleName(), 
                                result));
        
        if (_logSystem != null) {
          if (_logType == null)
            _logType = _logSystem.createFullType(LOG_TYPE);
          
          _logSystem.log(_logType, result); 
        }
        
      } catch (Exception e) {
        log.log(Level.WARNING, L.l("{0} failed: {1}", 
                                 this.getClass().getSimpleName(),
                                 e.getMessage()), e);
      } finally {
        _isAlive = false;
        
        thread.setName(oldName);
      }
    }
  }
}
