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

package com.caucho.v5.baratine;

import io.baratine.service.ServiceManager;

import java.util.concurrent.TimeUnit;

import com.caucho.v5.bartender.pod.PodBartender.PodType;
import com.caucho.v5.ramp.embed.ServerBaratineEmbedBuilder;
import com.caucho.v5.ramp.embed.ServerBaratineEmbedImpl;
import com.caucho.v5.vfs.PathImpl;

public interface ServiceServer
{
  ServicePod pod(String name);
  
  PodBuilder newPod(String name);
  
  ServiceManager client();
  
  void close();
  
  /**
   * Close without calling shutdown methods.
   * Primarily used to test crash and recovery.
   */
  void closeImmediate();
  
  static ServiceServer current()
  {
    return ServerBaratineEmbedImpl.getCurrent();
  }
  
  static Builder newServer()
  {
    return new ServerBaratineEmbedBuilder();
  }
  
  public interface PodBuilder
  {
    String getName();
    int getSize();
    PodType getType();
    Iterable<Server> getServers();
    Iterable<String> getApplicationPaths();
    
    PodBuilder off();
    PodBuilder solo();
    PodBuilder pair();
    PodBuilder triad();
    PodBuilder cluster();
    
    PodBuilder size(int size);
    
    PodBuilder server(String address, int port);
    
    PodBuilder journalMaxCount(int count);
    int getJournalMaxCount();
    
    PodBuilder journalDelay(long timeout, TimeUnit unit);
    long getJournalDelay();
    
    PodBuilder deploy(String path);
    PodBuilder classPath(PathImpl path);
    
    ServicePod build();
    
  }
  public interface Builder
  {
    Builder root(String path);
    Builder data(String path);
    
    Builder client(boolean isClient);
    Builder cluster(String cluster);
    Builder conf(String path);
    
    Builder port(int port);
    
    Builder removeData(boolean isRemove);
    Builder seed(String address, int port);
    
    Builder podName(String podName);
    
    ServiceServer build();
  }
  
  public interface Server
  {
    String getAddress();
    int getPort();
  }
}
