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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.resin;

import com.caucho.config.ConfigException;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.server.cluster.ProtocolConfig;
import com.caucho.util.L10N;

/**
 * Represents a protocol connection.
 */
public class SrunPort extends SocketLinkListener {
  private static L10N L = new L10N(SrunPort.class);

  private int _index = -1;
  private String _group = "";
  private boolean _isBackup;
  private String _protocolName = "hmux";

  /**
   * Sets the srun protocol (srun or hmux)
   */
  public void setProtocol(ProtocolConfig protocol)
    throws ConfigException
  {
    if (protocol.getId().equals("srun"))
      _protocolName = "srun";
    else if (protocol.getId().equals("hmux"))
      _protocolName = "hmux";
    else
      throw new ConfigException(L.l("`{0}' is an unknown srun protocol.  The protocol must be:\n hmux - new Resin P2P srun protocol\n srun - old Resin protocol",
                                    protocol));
  }

  /**
   * Returns the protocol class.
   */
  public String getSrunProtocol()
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

  // backwards compat
  public void setSrunIndex(int index)
  {
    setIndex(index);
  }

  public int getSrunIndex()
  {
    return getIndex();
  }

  public void setSrunGroup(String group)
  {
    setGroup(group);
  }

  public String getSrunGroup()
  {
    return getGroup();
  }

  /**
   * Sets the session group for the srun.
   */
  public void setGroup(String group)
  {
    _group = group;
  }

  /**
   * Gets the session group for the srun.
   */
  public String getGroup()
  {
    return _group;
  }
}
