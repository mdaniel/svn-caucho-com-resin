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
 * @author Scott Ferguson
 */

package com.caucho.cloud.topology;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.caucho.util.L10N;

/**
 * The CloudPod controls up to 64 CloudServers.
 * 
 * The first three servers are the triad servers.
 */
public class CloudPod
{
  private static final L10N L = new L10N(CloudPod.class);
  private static final Logger log
    = Logger.getLogger(CloudPod.class.getName());

  private final String _id;
  
  private final CloudCluster _cluster;

  private final int _index;
  
  private final CloudServer []_servers = new CloudServer[64];
  
  private final CopyOnWriteArrayList<CloudServerListener> _listeners
    = new CopyOnWriteArrayList<CloudServerListener>();
  
  private int _maxIndex;

  /**
   * Creates a new triad for the cluster.
   * 
   * @param cluster the owning cluster
   * @param index the triad index
   */
  public CloudPod(CloudCluster cluster,
                  String id,
                  int index)
  {
    _cluster = cluster;
    _index = index;
    
    if (index < 0 || index >= 64 * 64)
      throw new IllegalArgumentException(L.l("'{0}' is an invalid pod index because it's not between 0 and 64.",
                                             64 * 64));
    
    if (cluster == null)
      throw new NullPointerException();

    if (id != null)
      _id = id;
    else if (index == 0)
      _id = "main";
    else
      _id = String.valueOf(index);
  }

  /**
   * Returns the pod id.
   */
  public final String getId()
  {
    return _id;
  }

  /**
   * Returns the pod index.
   */
  public final int getIndex()
  {
    return _index;
  }

  /**
   * Returns the triad's cluster
   */
  public final CloudCluster getCluster()
  {
    return _cluster;
  }

  public CloudServer []getServerList()
  {
    return _servers;
  }
  /**
   * Finds the first server with the given server-id.
   */
  public CloudServer findServer(String id)
  {
    for (int i = 0; i < _maxIndex; i++) {
      CloudServer server = _servers[i];
      
      if (server != null && server.getId().equals(id))
        return server;
    }

    return null;
  }

  /**
   * Returns the pod with the given index.
   */
  public CloudServer findServer(int index)
  {
    return _servers[index];
  }

  /**
   * Finds the first server with the given address and port.
   */
  public CloudServer findServer(String address, int port)
  {
    for (int i = 0; i < _maxIndex; i++) {
      CloudServer server = _servers[i];
      
      if (server != null 
          && server.getAddress().equals(address)
          && server.getPort() == port) {
        return server;
      }
    }

    return null;
  }
  
  //
  // listeners
  //
  
  /**
   * Adds a listener to detect server add and removed.
   */
  public void addListener(CloudServerListener listener)
  {
    if (! _listeners.contains(listener))
      _listeners.add(listener);
  }
  
  /**
   * Removes a listener to detect server add and removed.
   */
  public void removeListener(CloudServerListener listener)
  {
    _listeners.remove(listener);
  }
  
  //
  // Add/remove servers
  //
  
  /**
   * Creates a new static server
   */
  public CloudServer createStaticServer(String id,
                                        String address,
                                        int port,
                                        boolean isSecure)
  {
    return createServer(id, address, port, isSecure, true);
  }
  
  /**
   * Creates a new dynamic server
   */
  public CloudServer createDynamicServer(String id,
                                         String address,
                                         int port,
                                         boolean isSecure)
  {
    return createServer(id, address, port, isSecure, false);
  }
  
  /**
   * Creates a new server
   */
  private CloudServer createServer(String id,
                                   String address,
                                   int port,
                                   boolean isSecure,
                                   boolean isStatic)
  {
    int index;
    CloudServer server;
    
    synchronized (this) {
      index = findFirstFreeIndex();
      
      if (index < 2)
        server = new TriadServer(id, this, index, isStatic);
      else
        server = new CloudServer(id, this, index, isStatic);
      
      _servers[index] = server;
      
      if (_maxIndex < index)
        _maxIndex = index;
    }
    
    return server;
  }
  
  private int findFirstFreeIndex()
  {
    for (int i = 0; i <= _maxIndex; i++) {
      if (_servers[i] == null)
        return i;
    }
    
    return _maxIndex;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + getIndex()
            + "," + getId()
            + ",cluster=" + _cluster.getId() + "]");
  }
}
