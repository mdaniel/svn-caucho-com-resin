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

package com.caucho.server.cluster;

import java.io.*;
import java.net.*;

import com.caucho.util.L10N;
import com.caucho.vfs.*;

import com.caucho.config.ConfigException;

import com.caucho.server.port.Port;
import com.caucho.server.port.Protocol;

import com.caucho.server.http.SrunProtocol;
import com.caucho.server.hmux.HmuxProtocol;

/**
 * Represents a protocol connection.
 */
public class ClusterPort extends Port {
  private static L10N L = new L10N(ClusterPort.class);

  private int _index = -1;
  private boolean _isBackup;
  private String _protocolName = "hmux";
  private int _clientWeight = 100;

  /**
   * Sets the srun protocol (srun or hmux)
   */
  public void setProtocol(Protocol protocol)
    throws ConfigException
  {
    if (! protocol.getClass().equals(Protocol.class)) {
      super.setProtocol(protocol);
    }
    else if (protocol.getProtocolName().equals("srun"))
      _protocolName = "srun";
    else if (protocol.getProtocolName().equals("hmux"))
      _protocolName = "hmux";
    else
      throw new ConfigException(L.l("`{0}' is an unknown cluster protocol.  The protocol must be:\n hmux - new Resin P2P cluster protocol\n srun - old Resin protocol",
                                    protocol.getProtocolName()));
  }

  /**
   * Returns the protocol class.
   */
  public String getClusterProtocol()
  {
    return _protocolName;
  }

  /**
   * Sets the session index for the srun.
   */
  public void setIndex(int index)
  {
    _index = index - 1;
  }

  /**
   * Returns the session index for the srun.
   */
  public int getIndex()
  {
    return _index;
  }

  /**
   * Set true for a backup.
   */
  public void setBackup(boolean isBackup)
  {
    _isBackup = isBackup;
  }

  /**
   * Return true for a backup.
   */
  public boolean isBackup()
  {
    return _isBackup;
  }

  /**
   * Set the client weight.
   */
  public void setClientWeight(int weight)
  {
    _clientWeight = weight;
  }

  /**
   * Return the client weight.
   */
  public int getClientWeight()
  {
    return _clientWeight;
  }

  public void init()
    throws ConfigException
  {
    if (getProtocol() != null &&
	! getProtocol().getClass().equals(Protocol.class)) {
    }
    else if (getClusterProtocol().equals("srun")) {
      SrunProtocol protocol = new SrunProtocol();
      protocol.setParent(this);
      setProtocol(protocol);
    }
    else if (getClusterProtocol().equals("hmux")) {
      HmuxProtocol protocol = new HmuxProtocol();
      protocol.setParent(this);
      setProtocol(protocol);
    }
    else
      throw new ConfigException(L.l("`{0}' is an unknown protocol.",
                                    getClusterProtocol()));

    super.init();
  }

  public String toString()
  {
    if (getAddress() != null)
      return "ClusterPort[address=" + getAddress() + ",port=" + getPort() + "]";
    else
      return "ClusterPort[address=*,port=" + getPort() + "]";
  }
}
