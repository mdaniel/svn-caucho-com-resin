/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.boot;

import java.util.*;

import com.caucho.config.*;
import com.caucho.util.*;

public class ClusterConfig {
  private static final L10N L = new L10N(ClusterConfig.class);
  
  private ResinConfig _resin;

  private ArrayList<Watchdog> _serverList
    = new ArrayList<Watchdog>();
  
  private String _id = "";

  ClusterConfig(ResinConfig resin)
  {
    _resin = resin;
  }

  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  public ResinConfig getResin()
  {
    return _resin;
  }

  public Watchdog createServer()
  {
    return new Watchdog(this);
  }

  public void addServer(Watchdog server)
    throws ConfigException
  {
    if (_resin.findServer(server.getId()) != null)
      throw new ConfigException(L.l("<server id='{0}'> is a duplicate server.  servers must have unique ids.",
				    server.getId()));
      
    _serverList.add(server);
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
  }

  /**
   * Finds a server.
   */
  public Watchdog findServer(String id)
  {
    for (int i = 0; i < _serverList.size(); i++) {
      Watchdog server = _serverList.get(i);

      if (id.equals(server.getId()))
	return server;
    }

    return null;
  }

  public String toString()
  {
    return "ClusterConfig[" + _id + "]";
  }
}
