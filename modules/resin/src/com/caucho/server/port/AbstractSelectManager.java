/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.port;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.vfs.*;

import com.caucho.config.ConfigException;

/**
 * Represents a protocol connection.
 */
abstract public class AbstractSelectManager {
  private static final L10N L = new L10N(AbstractSelectManager.class);
  private static final Logger log = Log.open(AbstractSelectManager.class);

  /**
   * Sets the timeout.
   */
  public void setSelectTimeout(long period)
  {
  }

  /**
   * Sets the max.
   */
  public void setSelectMax(int max)
  {
  }
  
  /**
   * Starts the manager.
   */
  abstract public void start();
  
  /**
   * Adds a keepalive connection.
   *
   * @param conn the connection to register as keepalive
   */
  abstract public void keepalive(TcpConnection conn);

  /**
   * Returns the select count.
   */
  public int getSelectCount()
  {
    return 0;
  }

  /**
   * Returns the number of available keepalives.
   */
  public int getFreeKeepalive()
  {
    return Integer.MAX_VALUE / 2;
  }

  /**
   * Stops the manager.
   */
  public boolean stop()
  {
    return true;
  }

  /**
   * Closing the manager.
   */
  public void close()
  {
    stop();
  }
}
