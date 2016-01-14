/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.container;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.network.NetworkSystem;
import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.system.RootDirectorySystem;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.network.port.PortTcp;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * Configuration for the <cluster> and <server> tags.
 */
@Configurable
public class ServerContainerConfig implements EnvironmentBean
{
  private static final L10N L = new L10N(ServerContainerConfig.class);
  private static final Logger log = Logger.getLogger(ServerContainerConfig.class.getName());

  private final SystemManager _system;
  
  private final ServerBuilderOld _builder;

  private long _memoryFreeMin = 1024 * 1024;

  private int _threadMax = 4096;
  private int _threadExecutorTaskMax = -1;
  private int _threadIdleMin = -1;
  private int _threadIdleMax = -1;
  private long _threadIdleTimeout = -1;
  
  private ServerBartender _selfServer;
  
  private ContainerProgram _program = new ContainerProgram();

  /**
   * Creates a new servlet server.
   */
  ServerContainerConfig(ServerBuilderOld builder,
                        SystemManager system,
                        ServerBartender selfServer)
  {
    _builder = builder;
    _system = system;
    _selfServer = selfServer;
    
    ConfigContext.setProperty("server", new VarServer(selfServer), getClassLoader());
    ConfigContext.setProperty("cluster", new VarCluster(), getClassLoader());
  }

  public SystemManager getResinSystem()
  {
    return _system;
  }

  /**
   * Returns the classLoader
   */
  @Override
  public EnvironmentClassLoader getClassLoader()
  {
    return _system.getClassLoader();
  }

  /**
   * Returns the relax schema.
   */
  /*
  @Override
  public String getSchema()
  {
    return "com/caucho/server/resin/cluster.rnc";
  }
  */

  @Configurable
  public Object createJdbcStore()
    throws ConfigException
  {
    return null;
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addJavaExe(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addJvmArgLine(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addJvmClasspath(String args)
  {
  }

  /**
   * Sets the stage id
   */
  @Configurable
  public void setStage(String stage)
  {
    /*
    if (stage == null || "".equals(stage)) {
      stage = "production";
    }
    
    _httpContainer.setStage(stage);
    */
  }

  /**
   * The Resin system classloader
   */
  @Configurable
  public void setSystemClassLoader(String loader)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogLog(ConfigProgram args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogPassword(String args)
  {
  }

  /**
   * Arguments on boot
   */
  @Configurable
  public void addWatchdogPort(int port)
  {
  }
  
  @Configurable
  public void setDynamicServerEnable(boolean isEnable)
  {
    
  }

  /**
   * Sets the minimum free memory after a GC
   */
  @Configurable
  public void setMemoryFreeMin(Bytes min)
  {
    _memoryFreeMin = min.getBytes();
  }

  /**
   * Sets the minimum free memory after a GC
   */
  public long getMemoryFreeMin()
  {
    return _memoryFreeMin;
  }

  /**
   * Sets the minimum free memory after a GC
   */
  @Configurable
  public void setPermGenFreeMin(Bytes min)
  {
  }

  /**
   * Sets the maximum thread-based keepalive
   */
  @Configurable
  public void setThreadMax(int max)
  {
    if (max < 0)
      throw new ConfigException(L.l("<thread-max> ({0}) must be greater than zero.",
                                    max));

    _threadMax = max;
  }

  /**
   * Sets the maximum executor (background) thread.
   */
  @Configurable
  public void setThreadExecutorTaskMax(int max)
  {
    _threadExecutorTaskMax = max;
  }

  /**
   * Sets the minimum number of idle threads in the thread pool.
   */
  @Configurable
  public void setThreadIdleMin(int min)
  {
    _threadIdleMin = min;
  }

  /**
   * Sets the maximum number of idle threads in the thread pool.
   */
  @Configurable
  public void setThreadIdleMax(int max)
  {
    _threadIdleMax = max;
  }
  
  @Configurable
  public void setThreadIdleTimeout(Period timeout)
  {
    _threadIdleTimeout = timeout.getPeriod();
  }

  /**
   * @param waitTime
   */
  public void setShutdownWaitMax(Period waitTime)
  {
    // TODO Auto-generated method stub
  }

  public void addSelectManager(SelectManagerCompat selectManager)
  {

  }
  
  public void addLoadBalanceWeight(ConfigProgram program)
  {
  }

  public void setLoadBalanceWarmupTime(ConfigProgram program)
  {
  }
  
  public void addServerDefault(ConfigProgram program)
  {
    
  }
  
  public void addContentProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  public ConfigProgram getProgram()
  {
    return _program;
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
  {
    if (_threadIdleMax > 0
        && _threadMax > 0
        && _threadMax < _threadIdleMax)
      throw new ConfigException(L.l("<thread-idle-max> ({0}) must be less than <thread-max> ({1})",
                                    _threadIdleMax, _threadMax));

    if (_threadIdleMin > 0
        && _threadIdleMax > 0
        && _threadIdleMax < _threadIdleMin)
      throw new ConfigException(L.l("<thread-idle-min> ({0}) must be less than <thread-idle-max> ({1})",
                                    _threadIdleMin, _threadIdleMax));

    if (_threadMax > 0
        && _threadExecutorTaskMax > 0
        && _threadMax < _threadExecutorTaskMax)
      throw new ConfigException(L.l("<thread-executor-task-max> ({0}) must be less than <thread-max> ({1})",
                                    _threadExecutorTaskMax, _threadMax));

    ThreadPool threadPool = ThreadPool.getThreadPool();

    if (_threadMax > 0)
      threadPool.setThreadMax(_threadMax);

    if (_threadIdleMin > 0)
      threadPool.setIdleMin(_threadIdleMin);
    
    if (_threadIdleTimeout > 0)
      threadPool.setIdleTimeout(_threadIdleTimeout);

    threadPool.setExecutorTaskMax(_threadExecutorTaskMax);
    
    /*
    if (_keepaliveSelectEnable) {
      try {
        Class<?> cl = Class.forName("com.caucho.server.connection.JniSelectManager");
        Method method = cl.getMethod("create", new Class[0]);

        initSelectManager((AbstractSelectManager) method.invoke(null, null));
      } catch (ClassNotFoundException e) {
        log.warning(L.l("'select-manager' requires Resin Professional.  See http://www.caucho.com for information and licensing."));
      } catch (Throwable e) {
        log.warning(L.l("Cannot enable select-manager {0}", e.toString()));

        log.log(Level.FINER, e.toString());
      }

      if (getSelectManager() != null) {
        if (_keepaliveSelectMax > 0)
          getSelectManager().setSelectMax(_keepaliveSelectMax);
      }
    }
    */
  }

  /**
   * EL variables
   */
  public class VarCluster {
    /**
     * Returns the resin.id
     */
    public String getId()
    {
      return _builder.getClusterId();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public PathImpl getRoot()
    {
      return getRootDirectory();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public PathImpl getRootDir()
    {
      return getRootDirectory();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public PathImpl getRootDirectory()
    {
      return _builder.getRootDirectory();
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + getId() + "]";
    }
  }

  public class VarServer {
    private final ServerBartender _server;

    public VarServer(ServerBartender server)
    {
      _server = server;
    }

    public String getId()
    {
      return _server.getDisplayName();
    }

    public String getName()
    {
      return _server.getDisplayName();
    }
    
    public String getProgram()
    {
      System.out.println("PGM: " + _builder.getProgramName());
      return _builder.getProgramName();
    }

    private int getPort(PortTcp port)
    {
      if (port == null)
        return 0;

      return port.port();
    }

    private String getAddress(PortTcp port)
    {
      if (port == null)
        return null;

      String address = port.address();

      if (address == null || address.length() == 0)
        address = "INADDR_ANY";

      return address;
    }

    private PortTcp getFirstPort(String protocol, boolean isSSL)
    {
      NetworkSystem listenService = NetworkSystem.current(); 
      
      for (PortTcp port : listenService.getPorts()) {
        if (protocol.equals(port.protocolName()) && (port.isSSL() == isSSL))
          return port;
      }

      return null;
    }

    public String getAddress()
    {
      return _selfServer.getAddress();
    }

    public int getPort()
    {
      return _selfServer.port();
    }

    public String getHttpAddress()
    {
      return getAddress(getFirstPort("http", false));
    }

    public int getHttpPort()
    {
      return getPort(getFirstPort("http", false));
    }


    public String getHttpsAddress()
    {
      return getAddress(getFirstPort("http", true));
    }

    public int getHttpsPort()
    {
      return getPort(getFirstPort("http", true));
    }

    /**
     * Returns the home directory of the server.
     */
    public PathImpl getHome()
    {
      ServerBaseOld server =  ServerBaseOld.current();

      return server == null ? VfsOld.getPwd() : server.getHomeDirectory();
    }

    /**
     * Returns the root directory of the server.
     */
    public PathImpl getRoot()
    {
      RootDirectorySystem dirSystem = _system.getSystem(RootDirectorySystem.class);
      
      return dirSystem.getRootDirectory();

      // return server == null ? Vfs.getPwd() : server.getRootDirectory();
    }

    /**
     * Returns the data directory of the server.
     */
    public PathImpl getData()
    {
      RootDirectorySystem dirSystem = _system.getSystem(RootDirectorySystem.class);
      
      // ServerBase server =  ServerBase.getCurrent();
      
      return dirSystem.getDataDirectory();
      
      //return server == null ? Vfs.getPwd() : server.getDataDirectory();
    }
  }

  public static class SelectManagerCompat {
    private boolean _isEnable = true;

    public void setEnable(boolean isEnable)
    {
      _isEnable = isEnable;
    }

    public boolean isEnable()
    {
      return _isEnable;
    }
  }

  /**
   * @param isEnable
   */
  public void setSendfileEnable(boolean isEnable)
  {
    // TODO Auto-generated method stub
    
  }

  /**
   * @param bytes
   */
  public void setSendfileMinLength(Bytes bytes)
  {
    // TODO Auto-generated method stub
    
  }
}
