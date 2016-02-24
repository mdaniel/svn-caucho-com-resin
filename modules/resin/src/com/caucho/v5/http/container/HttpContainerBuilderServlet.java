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

package com.caucho.v5.http.container;

import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;

/**
 * Configuration for the <cluster> and <server> tags.
 */
@Configurable
public class HttpContainerBuilderServlet extends HttpContainerBuilder
{
  private static final L10N L = new L10N(HttpContainerBuilderServlet.class);

  private long _memoryFreeMin = 1024 * 1024;
  private long _permGenFreeMin = 1024 * 1024;

  private int _threadMax = 4096;
  private int _threadExecutorTaskMax = -1;
  private int _threadIdleMin = -1;
  private int _threadIdleMax = -1;
  private long _threadIdleTimeout = -1;
  
  private ContainerProgram _hostProgram = new ContainerProgram();
  private ContainerProgram _httpProgram = new ContainerProgram();

  private String _sessionCookie;

  private String _sslSessionCookie;

  private String _urlPrefix;

  private String _alternateSessionUrlPrefix;
  
  public HttpContainerBuilderServlet(ServerBartender selfServer, 
                                     String serverHeader)
  {
    super(selfServer, serverHeader);

    // Config.setProperty("server", new VarServerHttp(_selfServer), getClassLoader());
    // Config.setProperty("cluster", new VarCluster(), getClassLoader());
    
    //_httpContainer = createHttpContainer();
  }
  
  ContainerProgram getHttpProgram()
  {
    return _httpProgram;
  }
  
  ContainerProgram getHostProgram()
  {
    return _hostProgram;
  }

  /**
   * Development mode error pages.
   */
  @Configurable
  public void setDevelopmentModeErrorPage(ConfigProgram program)
  {
    _httpProgram.addProgram(program);
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
    _permGenFreeMin = min.getBytes();
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

  //
  // Configuration from <cluster>
  //

  /**
   * Sets the connection error page.
   */
  @Configurable
  public void setConnectionErrorPage(ConfigProgram program)
  {
    _httpProgram.addProgram(program);
  }

  /**
   * Sets the root directory.
   */
  @Configurable
  public void setRootDirectory(PathImpl path)
  {
  }

  /**
   * Sets the root directory.
   */
  @Configurable
  public void setRootDir(PathImpl path)
  {
    setRootDirectory(path);
  }

  /**
   * Sets the url-length-max
   */
  @Configurable
  public void setMaxUriLength(int max)
  {
    setUrlLengthMax(max);
  }

  /**
   * Adds a WebAppDefault.
   */
  @Configurable
  public void addWebAppDefault(ConfigProgram program)
  {
    _hostProgram.addProgram(program);
  }

  /**
   * Adds a HostDefault.
   */
  @Configurable
  public void addHostDefault(ConfigProgram program)
  {
    _hostProgram.addProgram(program);
  }

  /**
   * Adds a HostDeploy.
   */
  @Configurable
  public void addHostDeploy(ConfigProgram program)
  {
    _hostProgram.addProgram(program);
  }

  /**
   * Adds the host.
   */
  @Configurable
  public void addHost(ConfigProgram program)
  {
    _hostProgram.addProgram(program);
  }

  /**
   * Creates the proxy cache.
   */
  @Configurable
  public void addProxyCache(ConfigProgram program)
    throws ConfigException
  {
    _httpProgram.addProgram(program);
  }

  /**
   * backward compatibility for proxy cache
   */
  @Configurable
  public void addCache(ConfigProgram program)
    throws ConfigException
  {
    _httpProgram.addProgram(program);
  }

  /**
   * Sets the session cookie
   */
  @Configurable
  public void setSessionCookie(String sessionCookie)
  {
    _sessionCookie = sessionCookie;
  }

  /**
   * Sets the ssl session cookie
   */
  @Configurable
  public void setSSLSessionCookie(String cookie)
  {
    _sslSessionCookie = cookie;
  }

  /**
   * Sets the session url prefix.
   */
  @Configurable
  public void setSessionUrlPrefix(String urlPrefix)
  {
    _urlPrefix = urlPrefix;
  }

  /**
   * Sets the alternate session url prefix.
   */
  @Configurable
  public void setAlternateSessionUrlPrefix(String urlPrefix)
    throws ConfigException
  {
    _alternateSessionUrlPrefix = urlPrefix;
  }

  /**
   * Adds an error page
   */
  public void addErrorPage(ConfigProgram program)
  {
    _httpProgram.addProgram(program);
  }
  
  // 
  // compat
  //
  
  public void addLoadBalanceWeight(ConfigProgram program)
  {
  }

  public void setLoadBalanceWarmupTime(ConfigProgram program)
  {
  }

  /**
   * Initialization.
   */
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

    ThreadPool threadPool = ThreadPool.current();

    if (_threadMax > 0)
      threadPool.setThreadMax(_threadMax);

    if (_threadIdleMin > 0)
      threadPool.setIdleMin(_threadIdleMin);
    
    if (_threadIdleTimeout > 0)
      threadPool.setIdleTimeout(_threadIdleTimeout);

    threadPool.setExecutorTaskMax(_threadExecutorTaskMax);
  }

  @Override
  public HttpContainerServlet build()
  {
    init();
    
    return new HttpContainerServlet(this);
  }
}
