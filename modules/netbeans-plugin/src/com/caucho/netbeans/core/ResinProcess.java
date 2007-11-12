/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.netbeans.core;

import com.caucho.netbeans.PluginL10N;
import com.caucho.netbeans.PluginLogger;

import org.openide.execution.NbProcessDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.logging.Level;

public class ResinProcess
{
  private static final PluginL10N L = new PluginL10N(ResinProcess.class);
  private static final PluginLogger log = new PluginLogger(ResinProcess.class);

  public static final int LIFECYCLE_NEW = 0;
  public static final int LIFECYCLE_INITIALIZING = 1;
  public static final int LIFECYCLE_INIT = 2;
  public static final int LIFECYCLE_STARTING = 3;
  public static final int LIFECYCLE_STANDBY = 4;
  public static final int LIFECYCLE_WARMUP = 5;
  public static final int LIFECYCLE_ACTIVE = 6;
  public static final int LIFECYCLE_FAILED = 7;
  public static final int LIFECYCLE_STOPPING = 8;
  public static final int LIFECYCLE_STOPPED = 9;
  public static final int LIFECYCLE_DESTROYING = 10;
  public static final int LIFECYCLE_DESTROYED = 11;

  private int _lifecycle = LIFECYCLE_NEW;

  private static final int TIMEOUT_TICK = 250;

  private final ResinConfiguration _properties;
  private final String _id;

  private File _javaHome;
  private String _displayName;

  private File _javaExe;
  private File _resinJar;

  private int _debugPort = 11999;

  private int _startTimeout = 60 * 1000;
  private int _stopTimeout = 60 * 1000;

  private boolean _isDebug;
  private Process _process;
  private Console _console;

  private final Object _lock = new Object();

  public ResinProcess(ResinConfiguration properties, String id)
  {
    _properties = properties;
    _id = id;
  }

  private boolean isInit()
  {
    return _lifecycle >= LIFECYCLE_INIT;
  }

  public boolean isActive()
  {
    return _lifecycle == LIFECYCLE_ACTIVE;
  }

  public File getResinConf()
  {
    return _properties.getResinConf();
  }

  public File getResinHome()
  {
    return _properties.getResinHome();
  }

  public String getServerId()
  {
    return _properties.getServerId();
  }

  public String getServerAddress()
  {
    return _properties.getServerAddress();
  }

  public int getServerPort()
  {
    return _properties.getServerPort();
  }

  public File getJavaHome()
  {
    return _javaHome;
  }

  public void setJavaHome(File javaHome)
  {
    if (isInit())
      throw new IllegalStateException();

    _javaHome = javaHome;
  }

  public String getDisplayName()
  {
    return _displayName;
  }

  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  public int getStartTimeout()
  {
    return _startTimeout;
  }

  public void setStartTimeout(int startTimeoutMilliseconds)
  {
    _startTimeout = startTimeoutMilliseconds;
  }

  public int getStopTimeout()
  {
    return _stopTimeout;
  }

  public void setStopTimeout(int stopTimeoutMilliseconds)
  {
    _stopTimeout = stopTimeoutMilliseconds;
  }

  private void requiredFile(String name, File file)
    throws IllegalStateException
  {
    if (file == null)
      throw new IllegalStateException(L.l("''{0}'' is required", name));

    if (!file.exists())
      throw new IllegalStateException(L.l("''{0}'' does not exist", file));
  }

  public void init()
    throws IllegalStateException
  {
    synchronized(_lock) {
      if (isInit())
        return;

      _lifecycle = LIFECYCLE_INITIALIZING;

      _properties.validate();

      requiredFile("java-home", _javaHome);

      File javaExe;

      javaExe = new File(_javaHome, "bin/java");

      if (!javaExe.exists())
        javaExe = new File(_javaHome, "bin/java.exe");

      if (!javaExe.exists())
        throw new IllegalStateException(L.l("Cannot find java exe in ''{0}''", _javaHome));

      _javaExe = javaExe;

      File resinJar = new File(getResinHome(), "lib/resin.jar");

      if (!resinJar.exists())
        throw new IllegalStateException(L.l("Cannot find lib/resin.jar in ''{0}''", getResinHome()));

      _resinJar = resinJar;

      _lifecycle = LIFECYCLE_INIT;
    }
  }

  public int getDebugPort()
  {
    return _debugPort;
  }

  /**
   * Default is 11999.
   */
  public void setDebugPort(int debugPort)
  {
    if (isActive())
      throw new IllegalStateException();

    _debugPort = debugPort;
  }


  public void start()
    throws IllegalStateException, IOException
  {
    init();

    synchronized(_lock) {
      if (isActive())
        stopImpl(false);

      _isDebug = false;

      startImpl();
    }
  }

  public void startDebug()
    throws IllegalStateException, IOException
  {
    init();

    synchronized(_lock) {
      if (isActive())
        stopImpl(false);

      _isDebug = true;

      startImpl();
    }
  }

  public boolean isDebug()
  {
    return _isDebug;
  }

  private void startImpl()
    throws IllegalStateException, IOException
  {
    _lifecycle = LIFECYCLE_STARTING;

    if (!isPortFree(getServerPort()))
      throw new IllegalStateException(L.l("Cannot start Resin, server-port {0} is already in use", getServerPort()));

    if (_isDebug && !isPortFree(_debugPort))
        throw new IllegalStateException(L.l("Cannot start Resin, debug-port {0} is already in use", _debugPort));

    StringBuilder args = new StringBuilder();

    args.append("-jar ");
    args.append('"');
    args.append(_resinJar.getAbsolutePath());
    args.append('"');

    args.append(' ');
    args.append("-conf ");
    args.append('"');
    args.append(getResinConf().getAbsolutePath());
    args.append('"');

    if (getServerId() != null && getServerId().length() > 0) {
      args.append(' ');
      args.append("-server ");
      args.append('"');
      args.append(getResinConf().getAbsolutePath());
      args.append('"');
    }

    if (_isDebug)
      throw new IllegalStateException("debug mode not implemented");

    // open the ServerLog
    synchronized (this) {
      if (_console != null) {
        _console.takeFocus();
      }
      else {
        _console = new Console(_id);
      }
    }

    NbProcessDescriptor processDescriptor
      = new NbProcessDescriptor(_javaExe.getAbsolutePath(),
                                args.toString(),
                                getDisplayName());

    _console.println(L.l("Starting Resin process {0} {1}",
                         processDescriptor.getProcessName(),
                         processDescriptor.getArguments()));

    _console.flush();

    _process = processDescriptor.exec(null, null, true, getResinHome());

    _console.println();

    _console.start(new InputStreamReader(_process.getInputStream()),
                   new InputStreamReader(_process.getErrorStream()));

    new Thread("resin-" + _id + "-process-monitor")
    {
      public void run()
      {
        try {
          _process.waitFor();
          Thread.sleep(2000);
        }
        catch (InterruptedException e) {
        }
        finally {
          handleProcessDied();
        }
      }
    }.start();

    // wait for server port to become active

    boolean isResponsive = false;

    for (int i = _stopTimeout; i > 0; i-= TIMEOUT_TICK) {
      if (isResponsive()) {
        isResponsive = true;
        break;
      }

      try {
        Thread.sleep(TIMEOUT_TICK);
      }
      catch (InterruptedException ex) {
        if (log.isLoggable(Level.WARNING))
          log.log(Level.WARNING, ex);
      }
    }

    if (!isResponsive) {
      String msg = L.l("Resin process failed to respond on server-port {0}", getServerPort());

      log.log(Level.WARNING, msg);

      try {
        stopImpl(false);
      }
      catch (Exception ex) {
        log.log(Level.WARNING, ex);
      }

      throw new IOException(msg);
    }

    _lifecycle = LIFECYCLE_ACTIVE;
  }

  public boolean isResponsive()
  {
    // XXX: could be more robust, i.e. actually get a response from the server
    return !isPortFree(getServerPort());
  }

  public Console getConsole()
  {
    return _console;
  }

  private static boolean isPortFree(int port)
  {
    ServerSocket ss = null;

    try {
      ss = new ServerSocket(port);
      return true;
    }
    catch (IOException ioe) {
      return false;
    }
    finally {
      if (ss != null) {
        try {
          ss.close();
        }
        catch (IOException ex) {
        }
      }
    }
  }

  public String getHttpUrl()
  {
    return null;
  }

  /**
   * Return true if the process is running
   */
  public boolean isProcessRunning()
  {
    Process process = _process;

    if (process != null) {
      try {
        process.exitValue();

        return false;
      }
      catch (IllegalThreadStateException e) {
        return true;
      }
    }

    return false;
  }

  /**
   * Return true if the server is responding
   */
  public boolean isResponding()
  {
    // XXX: could be more robust, contact the server and make sure there is a response
    return !isPortFree(getServerPort());
  }

  public Process getJavaProcess()
  {
    return _process;
  }

  private void handleProcessDied()
  {
    stopImpl(false);
  }

  public void stop()
  {
    synchronized(_lock) {

      if (_lifecycle != LIFECYCLE_ACTIVE)
        return;

      stopImpl(true);
    }
  }

  private void stopImpl(boolean isGraceful)
  {
    _lifecycle = LIFECYCLE_STOPPING;

    Process process = _process;
    _process = null;

    Console console = _console;
    _console = null;

    _isDebug = false;

    try {
      // XXX: graceful shutdown, send message to server,
      // then use isPortFree in a while loop that times out

      /*
      if (isGraceful) {
      try {
        printConsoleLine(L.l("Stopping Resin process ..."));
      }
      catch (Exception ex) {
        // no-op
      }

      for (int i = STOP_TIMEOUT; !isPortFree(getServerPort()) && i > 0; i-= TICK) {
        try {
          Thread.sleep(TICK);
        }
        catch (InterruptedException ex) {
          if (log.isLoggable(Level.WARNING))
            log.log(Level.WARNING, e), ex);

        }
      }
      }
      */
    }
    finally {
      try {
        if (process != null)
          process.destroy();
      }
      finally {
        try {
          console.println(L.l("Resin process destroyed"));
          console.flush();
        }
        catch (Exception ex) {
          // no-op
        }

        if (console != null)
          console.destroy();
      }
    }

    _lifecycle = LIFECYCLE_STOPPED;
  }

  public void destroy()
  {
    synchronized(_lock) {
      _lifecycle = LIFECYCLE_DESTROYING;

      try {
        try {
          stop();
        }
        catch (Exception ex) {
          log.log(Level.WARNING, ex);

          try {
            stopImpl(false);
          }
          catch (Exception ex2) {
            log.log(Level.WARNING, ex2);
          }
        }
      }
      finally {
        _lifecycle = LIFECYCLE_DESTROYED;
      }
    }
  }
}
