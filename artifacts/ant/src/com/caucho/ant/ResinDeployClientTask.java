/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public abstract class ResinDeployClientTask extends Task
{
  protected String _server;
  protected int _port = -1;
  protected String _user;
  protected String _message;
  protected String _password;
  protected String _resinHome;
  protected String _resinConf;

  protected String _stage;
  protected String _host;
  protected String _context;
  protected String _version;
  protected Level _level = Level.WARNING;

  private File _resinJar;

  public ResinDeployClientTask()
  {
  }

  public String getResinHome()
  {
    return _resinHome;
  }

  public void setResinHome(String resinHome)
  {
    _resinHome = resinHome;
  }

  public String getResinConf()
  {
    return _resinConf;
  }

  public void setResinConf(String resinConf)
  {
    _resinConf = resinConf;
  }

  public void setServer(String server)
  {
    _server = server;
  }

  public String getServer()
  {
    return _server;
  }

  public void setPort(int port)
  {
    _port = port;
  }

  public int getPort()
  {
    return _port;
  }

  public void setUser(String user)
  {
    _user = user;
  }

  public String getUser()
  {
    return _user;
  }

  public void setPassword(String password)
  {
    _password = password;
  }

  public String getPassword()
  {
    return _password;
  }

  public void setCommitMessage(String message)
  {
    _message = message;
  }

  public String getCommitMessage()
  {
    return _message;
  }

  public void setStage(String stage)
  {
    _stage = stage;
  }

  public String getStage()
  {
    return _stage;
  }

  public void setHost(String host)
  {
    _host = host;
  }

  public String getHost()
  {
    return _host;
  }

  public void setContext(String context)
  {
    _context = context;
  }

  public String getContext()
  {
    return _context;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public String getVersion()
  {
    return _version;
  }

  public String getMessage()
  {
    return _message;
  }

  public void setMessage(String message)
  {
    _message = message;
  }

  public Level getLevel()
  {
    return _level;
  }

  public void setLevel(Level level)
  {
    _level = level;
  }

  public void setLogLevel(String level)
  {
    if (level == null || level.isEmpty())
      return;

    level = level.toUpperCase(Locale.ENGLISH);

    _level = Level.parse(level);
  }

  protected void validate()
    throws BuildException
  {
    if (_resinHome == null)
      throw new BuildException("resin-home is requried by " + getTaskName());

    _resinJar = new File(_resinHome, "lib/resin.jar");
    if (!_resinJar.exists() || !_resinJar.canRead())
      throw new BuildException("resin-home '"
        + _resinHome
        + "' appears invalid");

    if (_server == null)
      throw new BuildException("server is required by " + getTaskName());

    if (_port == -1)
      throw new BuildException("port is required by " + getTaskName());

    if (_user == null)
      throw new BuildException("user is required by " + getTaskName());

    if (_password == null)
      throw new BuildException("password is required by " + getTaskName());
  }

  public void fillBaseArgs(List<String> args)
  {
    args.add("-resin-home");
    args.add('"' + _resinHome + '"');

    if (_resinConf != null) {
      args.add("-conf");
      args.add('"' + _resinConf + '"');
    }

    args.add("-address");
    args.add(_server);

    args.add("-port");
    args.add(Integer.toString(_port));

    args.add("-user");
    args.add(_user);

    args.add("-password");
    args.add(_password);

    if (getMessage() != null) {
      args.add("-m");
      args.add(getMessage());
    }
  }

  protected abstract void fillArgs(List<String> args);

  /**
   * Executes the ant task.
   */
  @Override
  public void execute()
    throws BuildException
  {
    validate();

    Java java = new Java(this);
    java.setFailonerror(true);
    java.setFork(true);
    java.setJar(_resinJar);

    List<String> args = new ArrayList<String>();
    fillArgs(args);

    for (String arg : args)
      java.createArg().setLine(arg);

    log(java.getCommandLine().toString(), _level.intValue());

    java.executeJava();
  }

  private class AntLogHandler extends Handler
  {
    @Override
    public void close()
      throws SecurityException
    {
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void publish(LogRecord record)
    {
      if (Level.ALL.equals(record.getLevel())
        || Level.INFO.equals(record.getLevel())
        || Level.CONFIG.equals(record.getLevel()))
        log(record.getMessage());

      else if (Level.FINE.equals(record.getLevel()))
        log(record.getMessage(), Project.MSG_VERBOSE);

      else if (Level.FINER.equals(record.getLevel())
        || Level.FINEST.equals(record.getLevel()))
        log(record.getMessage(), Project.MSG_DEBUG);

      else if (Level.SEVERE.equals(record.getLevel()))
        log(record.getMessage(), Project.MSG_ERR);

      else if (Level.WARNING.equals(record.getLevel()))
        log(record.getMessage(), Project.MSG_WARN);
    }
  }
}
