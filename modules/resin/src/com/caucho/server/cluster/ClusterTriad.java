/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.cluster;

import com.caucho.util.L10N;
import java.util.logging.Logger;

/**
 * The ClusterTriad is a reliable triplicate for clustered data.
 * 
 * For small clusters, the triad may only have 1 or 2 servers, so
 * server B and server C may return null.
 */
public final class ClusterTriad
{
  private static final L10N L = new L10N(ClusterTriad.class);
  private static final Logger log
    = Logger.getLogger(ClusterTriad.class.getName());

  private final Cluster _cluster;
  
  private final ClusterServer _serverA;
  private final ClusterServer _serverB;
  private final ClusterServer _serverC;

  /**
   * Creates a new triad for the cluster.
   * 
   * @param cluster the owning cluster
   * @param serverA the triad's first server
   * @param serverB the triad's second server
   * @param serverC the triad's third server
   */
  ClusterTriad(Cluster cluster,
      	       ClusterServer serverA,
      	       ClusterServer serverB,
      	       ClusterServer serverC)
  {
    _cluster = cluster;
    
    _serverA = serverA;
    _serverB = serverB;
    _serverC = serverC;
    
    if (_serverA == null)
      throw new NullPointerException(L.l("ClusterTriad requires at least one server"));
    
    if (_serverB == null && _serverC != null)
      throw new NullPointerException(L.l("ClusterTriad server B may not be null if server C is not null."));
  }
  
  /**
   * Returns the triad's first server
   * 
   * @return the first server.
   */
  public ClusterServer getServerA()
  {
    return _serverA;
  }
  
  /**
   * Returns the triad's second server
   * 
   * @return the second server.
   */
  public ClusterServer getServerB()
  {
    return _serverB;
  }
  
  /**
   * Returns the triad's third server
   * 
   * @return the third server.
   */
  public ClusterServer getServerC()
  {
    return _serverC;
  }

  /**
   * Returns true for any of the triad servers.
   */
  public boolean isTriad(ClusterServer server)
  {
    return (_serverA == server
	    || _serverB == server
	    || _serverC == server);
  }
  
  /**
   * Returns the primary server given an ownership tag.
   */
  public ClusterServer getPrimary(Owner owner)
  {
    switch (owner) {
    case A_B:
    case A_C:
      return getServerA();
      
    case B_C:
    case B_A:
      if (getServerB() != null)
	return getServerB();
      else
	return getServerA();
      
    case C_A:
      if (getServerC() != null)
	return getServerC();
      else
	return getServerB();
      
    case C_B:
      if (getServerC() != null)
	return getServerC();
      else
	return getServerA();
      
    default:
	throw new IllegalStateException(L.l("'{0}' is an unknown owner", owner));
    }
  }
  
  /**
   * Returns the secondary server given an ownership tag.  If the
   * triad has only one server, return null.
   */
  public ClusterServer getSecondary(Owner owner)
  {
    if (getServerB() == null)
      return null;
     
    switch (owner) {
    case B_A:
    case C_A:
      return getServerA();
      
    case A_B:
      if (getServerB() != null)
	return getServerB();
      else
	return getServerC();
      
    case A_C:
      if (getServerC() != null)
	return getServerC();
      else
	return getServerB();
      
    case B_C:
      if (getServerC() != null)
	return getServerC();
      else
	return getServerA();
      
    case C_B:
      if (getServerB() != null)
	return getServerB();
      else
	return getServerA();
      
    default:
	throw new IllegalStateException(L.l("'{0}' is an unknown owner", owner));
     }
  }
  
  /**
   * Returns the tertiary server given an ownership tag.  If the server has
   * only 2 servers, return null.
   */
  public ClusterServer getTertiary(Owner owner)
  {
    if (getServerC() == null)
      return null;
    
    switch (owner) {
    case B_C:
    case C_B:
      return getServerA();
      
    case A_C:
    case C_A:
      return getServerB();
      
    case A_B:
    case B_A:
      return getServerC();
      
    default:
	throw new IllegalStateException(L.l("'{0}' is an unknown owner", owner));
    }
  }

  /**
   * Returns the best primary or secondary triad server.
   */
  public ClusterServer getActiveServer(Owner owner,
				       ClusterServer oldServer)
  {
    ClusterServer server;
    ServerPool pool;

    server = getPrimary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && pool.isActive() && server != oldServer)
      return server;

    server = getSecondary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && pool.isActive() && server != oldServer)
      return server;

    server = getTertiary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && pool.isActive() && server != oldServer)
      return server;

    // force the send

    server = getPrimary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    server = getSecondary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    server = getTertiary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    return null;
  }

  /**
   * Returns the owner for an index
   */
  public Owner getOwner(long index)
  {
    Owner []ownerList = Owner.class.getEnumConstants();
    
    return ownerList[(int) (index % ownerList.length & 0x7fffffff)];
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cluster.getId() + "]";
  }
  
  /**
   * For any object, assigns an owner and backup in the triad. 
   */
  public enum Owner {
    A_B,
    B_C,
    C_A,
    
    A_C,
    B_A,
    C_B
  };
}
