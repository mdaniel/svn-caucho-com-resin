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

package com.caucho.v5.ramp.embed;

import io.baratine.service.ServiceManager;

import com.caucho.v5.amp.Amp;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.baratine.ServicePod;
import com.caucho.v5.baratine.ServiceServer;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.pod.PodContainer;
import com.caucho.v5.loader.ContextClassLoader;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.server.container.ServerBaseOld;
import com.caucho.v5.util.L10N;


public class ServerBaratineEmbedImpl 
  implements ServiceServer
{
  private static final L10N L = new L10N(ServerBaratineEmbedImpl.class);
  
  private ServerBaseOld _server;
  private boolean _isClient;
  private String _podName;
  
  private ServiceManagerAmp _client;
  
  public ServerBaratineEmbedImpl(ServerBaseOld server,
                                 boolean isClient,
                                 String podName)
  {
    _server = server;
    _isClient = isClient;
    _podName = podName;
  }
  
  ServerBaseOld getServer()
  {
    return _server;
  }
  
  @Override
  public ServicePod pod(String name)
  {
    /*
    ServiceServer server = _server.getPodContainer().getServerBaratine();
    
    ServicePod pod = server.pod(name);
    
    return pod; 
    //return new PodBaratineImpl(this, name);
     * */
    
    return null;
  }
  
  @Override
  public PodBuilder newPod(String name)
  {
    /*
    if (_isClient) {
      throw new IllegalStateException(L.l("pods cannot be created in client mode"));
    }
    
    ServiceServer server;
    
    server = _server.getPodContainer().getServerBaratine();

    return server.newPod(name);
    */
    
    return null;
  }
  
  @Override
  public ServiceManager client()
  {
    if (_client != null) {
      return _client;
    }
    
    HttpContainer container = _server.getHttp();
    EnvironmentClassLoader clientLoader
      = EnvironmentClassLoader.create(container.classLoader(), "client:");
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(clientLoader);

      _client = Amp.newManagerBuilder().name("client:").start();
      
      Amp.setContextManager(_client, clientLoader);
      
      _client.start();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
    
    return _client;
  }
  
  @Override
  public void close()
  {
    _server.close();
  }
  
  @Override
  public void closeImmediate()
  {
    _server.shutdown(ShutdownModeAmp.IMMEDIATE);
  }

  public static ServiceServer getCurrent()
  {
    PodContainer podContainer = PodContainer.getCurrent();
    
    //return podContainer.getServerBaratine();
    return null;
  }
  
  @Override
  public void finalize()
    throws Throwable
  {
    close();
    
    super.finalize();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _server.getServerSelf().getId() + "]";
  }
}
