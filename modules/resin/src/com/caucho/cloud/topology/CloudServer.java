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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorStream;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.Period;
import com.caucho.lifecycle.StartLifecycleException;
import com.caucho.management.server.ClusterServerMXBean;
import com.caucho.network.balance.ClientSocketFactory;
import com.caucho.network.server.NetworkServer;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterPod;
import com.caucho.server.cluster.ClusterServerAdmin;
import com.caucho.server.cluster.Machine;
import com.caucho.server.cluster.Server;
import com.caucho.server.cluster.ClusterPod.Owner;
import com.caucho.server.resin.Resin;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * Defines a cloud server, a single Resin instance.
 * 
 * Each server has the following:
 * <ul>
 * <li>A unique id across the entire domain.
 * <li>A containing pod (contained within the server).
 * <li>A unique index within the pod. Servers 0-2 are special servers called
 * "triad" servers.
 * <li>An IP address and port.
 * </ul>
 *
 * Servers are organized into pods of up to 64 servers, contained in a cluster.
 * 
 * All the clusters are contained in a domain.
 */
public class CloudServer {
  private static final L10N L = new L10N(CloudServer.class);
  private static final Logger log
    = Logger.getLogger(CloudServer.class.getName());

  private static final int DECODE[];

  private final String _id;

  private final CloudPod _pod;
  private final int _index;
  
  private final boolean _isStatic;

  // unique identifier for the server within the cluster
  private String _uniqueClusterId;
  // unique identifier for the server within all Resin clusters
  private String _uniqueDomainId;
  
  private String _address = "127.0.0.1";
  private int _port = -1;
  private boolean _isSSL;

  public CloudServer(String id,
                     CloudPod pod, 
                     int index,
                     boolean isStatic)
  {
    _id = id;
    
    _pod = pod;
    _index = index;
    
    _isStatic = isStatic;
    
    if (index < 0 || index >= 64)
      throw new IllegalArgumentException(L.l("'{0}' is an invalid server index because it must be between 0 and 64",
                                            index));
    
    if (! isStatic && index == 0)
      throw new IllegalArgumentException(L.l("The first server must be static."));
    
    if (id == null)
      throw new NullPointerException();
    if (pod == null)
      throw new NullPointerException();

    // _clusterPort = new ClusterPort(this);
    // _ports.add(_clusterPort);

    StringBuilder sb = new StringBuilder();

    sb.append(convert(getIndex()));
    sb.append(convert(getPod().getIndex()));
    sb.append(convert(getPod().getIndex() / 64));

    _uniqueClusterId = sb.toString();

    String clusterId = pod.getCluster().getId();
    if (clusterId.equals(""))
      clusterId = "default";

    _uniqueDomainId = _uniqueClusterId + "." + clusterId.replace('.', '_');
  }

  /**
   * Gets the unique server identifier.
   */
  public final String getId()
  {
    return _id;
  }

  public final String getDebugId()
  {
    if ("".equals(_id))
      return "default";
    else
      return _id;
  }

  /**
   * Returns the index within the pod.
   */
  public final int getIndex()
  {
    return _index;
  }

  /**
   * Returns the server's id within the cluster
   */
  public final String getIdWithinCluster()
  {
    return _uniqueClusterId;
  }

  /**
   * Returns the server's id within all Resin clusters
   */
  public final String getIdWithinDomain()
  {
    return _uniqueDomainId;
  }

  /**
   * True if this server is a triad.
   */
  public boolean isTriad()
  {
    return _index < 3;
  }

  /**
   * True for statically configured servers.
   */
  public boolean isStatic()
  {
    return true;
  }
  
  //
  // topology attributes
  //

  /**
   * Returns the pod
   */
  public CloudPod getPod()
  {
    return _pod;
  }

  /**
   * Returns the cluster.
   */
  public CloudCluster getCluster()
  {
    return getPod().getCluster();
  }

  /**
   * Returns the pod owner
   */
  public ClusterPod.Owner getTriadOwner()
  {
    return ClusterPod.getOwner(getIndex());
  }
  
  //
  // IP addresses
  //
  
  /**
   * Gets the address
   */
  public final String getAddress()
  {
    return _address;
  }
  
  /**
   * Gets the port
   */
  public final int getPort()
  {
    return _port;
  }
  
  public boolean isSSL()
  {
    return _isSSL;
  }

  private static char convert(long code)
  {
    code = code & 0x3f;

    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }

  public static int decode(int code)
  {
    return DECODE[code & 0x7f];
  }

  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
