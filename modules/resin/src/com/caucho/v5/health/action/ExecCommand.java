/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.Vfs;

/**
 * Health action to execute a shell command.  
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:ExecCommand>
 *   <command>/opt/company/custom_command.sh</command>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:Restart>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class ExecCommand extends HealthActionBase
{
  private static final L10N L = new L10N(ExecCommand.class);
  private static final Logger log
    = Logger.getLogger(ExecCommand.class.getName());

  private String _command;
  private Map<String, String> _customEnv = new HashMap<String, String>();
  private File _dir;
  private long _timeout = 2000L;
  
  private ProcessBuilder _processBuilder;
  
  public ExecCommand()
  {
    
  }
  
  @PostConstruct
  public void init()
  {
    if (_command == null) 
      throw new ConfigException(L.l("'command' is a required attribute of <health:{0}>",
                                    this.getClass().getSimpleName()));
    
    _processBuilder = new ProcessBuilder();
    
    List<String> args = new ArrayList<String>();
    StringTokenizer st = new StringTokenizer(_command);
    while(st.hasMoreTokens())
      args.add(st.nextToken());
    
    _processBuilder.command(args);
    
    if (_dir != null)
      _processBuilder.directory(_dir);
    
    Map<String,String> env = _processBuilder.environment();
    env.putAll(System.getenv());
    env.putAll(_customEnv);
    
    _processBuilder.redirectErrorStream(true);
    
    super.init();
  }

  public String getCommand()
  {
    return _command;
  }

  /**
   * Set the command line to execute
   */
  @Configurable
  public void setCommand(String command)
  {
    _command = command;
  }
  
  public File getDir()
  {
    return _dir;
  }

  /**
   * Set the directory to execute from
   */
  @Configurable
  public void setDir(File dir)
  {
    _dir = dir;
  }
  
  public long getTimeout()
  {
    return _timeout;
  }

  /**
   * Set the timeout on execution of the command, after which it will be killed
   */
  @Configurable
  public void setTimeout(Period timeout)
  {
    setTimeoutMillis(timeout.getPeriod());
  }
  
  @Configurable
  public void setTimeoutMillis(long timeout)
  {
    _timeout = timeout;
  }
  
  /**
   * Add a custom env variable, system variable are available by default
   */
  @Configurable
  public void addEnv(HealthEnv env)
  {
    _customEnv.put(env.getName(), env.getValue());
  }
  
  public Map<String, String> getEnv()
  {
    return _customEnv;
  }
  
  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    Process process = _processBuilder.start();
    
    InputStream inputStream = process.getInputStream();
    ReadStream readStream = Vfs.openRead(inputStream);
    
    TimeoutAlarm timeout = new TimeoutAlarm(process, readStream);
    Alarm alarm = new Alarm(timeout, _timeout);

    char []buf = new char[256]; 
    
    StringBuilder sb = new StringBuilder();
    
    try {
      int len;
      while ((len = readStream.read(buf, 0, buf.length)) > 0) {
        sb.append(buf, 0, len);
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    int exitCode = -1;
    
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    alarm.dequeue();
    
    ResultStatus status = exitCode != 0 ? ResultStatus.FAILED : ResultStatus.OK;
    return new HealthActionResult(status, L.l("Command '{0}' completed with exit code {1}\n{2}",
                                              _command,
                                              exitCode,
                                              sb.toString()));
  }
  
  class TimeoutAlarm implements AlarmListener 
  {
    private Process _process;
    private ReadStream _readStream;

    TimeoutAlarm(Process process, ReadStream readStream)
    {
      _process = process;
      _readStream = readStream;
    }

    public void handleAlarm(Alarm alarm)
    {
      log.log(Level.WARNING, L.l("{0} killing command '{1}' due to timeout",
                                 ExecCommand.class.getSimpleName(),
                                 _command));

      try {
        if (_readStream != null)
          _readStream.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
      
      try {
        _process.destroy();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }
  
  @Configurable
  public static class HealthEnv
  {
    private String _name;
    private String _value;
    
    public String getName()
    {
      return _name;
    }

    @Configurable
    public void setName(String name)
    {
      _name = name;
    }
    
    public String getValue()
    {
      return _value;
    }
    
    @Configurable
    public void setValue(String value)
    {
      _value = value;
    }
  }
}
