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

package com.caucho.network.balance;


/**
 * A pool of connections to a server.
 */
public interface ClientSocketFactoryApi
{
  /**
   * Returns the user-readable id of the target server.
   */
  public String getId();

  /**
   * Returns the debug id.
   */
  public String getDebugId();

  /**
   * Returns the hostname of the target server.
   */
  public String getAddress();
  /**
   * Gets the port of the target server.
   */
  public int getPort();
  
  /**
   * Returns true if the server is active.
   */
  public boolean isActive();

  /**
   * Returns true if the server is dead.
   */
  public boolean isDead();

  /**
   * Enable the client
   */
  public void enable();

  /**
   * Disable the client
   */
  public void disable();

  /**
   * Returns the lifecycle state.
   */
  public String getState();

  /**
   * Returns true if the server can open a connection.
   */
  public boolean canOpenWarmOrRecycle();

  /**
   * Returns true if the server can open a connection.
   */
  public boolean canOpenWarm();

  /**
   * Return true if active.
   */
  public boolean isEnabled();

  public void toBusy();

  public void toFail();

  /**
   * Called when the socket read/write fails.
   */
  public void failSocket();

  /**
   * Called when the socket read/write fails.
   */
  public void failConnect();

  /**
   * Called when the server responds with "busy", e.g. HTTP 503
   */
  public void busy();

  /**
   * Called when the server has a successful response
   */
  public void success();

  /**
   * Enable the client.
   */
  public void start();

  /**
   * Disable the client.
   */
  public void stop();

  /**
   * Session only
   */
  public void enableSessionOnly();

  /**
   * Open a stream to the target server.
   *
   * @return the socket's read/write pair.
   */
  public ClientSocket openWarm();

  /**
   * Open a stream to the target server object persistence.
   *
   * @return the socket's read/write pair.
   */
  public ClientSocket openIfLive();

  /**
   * Open a stream to the target server for a session.
   *
   * @return the socket's read/write pair.
   */
  public ClientSocket openSticky();

  /**
   * Open a stream to the target server for the load balancer.
   *
   * @return the socket's read/write pair.
   */
  public ClientSocket open();

  /**
   * We now know that the server is live, e.g. if a sibling has
   * contacted us.
   */
  public void wake();

  /**
   * Notify that a start has occurred.
   */
  public void notifyStart();

  /**
   * Notify that a stop has occurred.
   */
  public void notifyStop();

  /**
   * Clears the recycled connections, e.g. on detection of backend
   * server going down.
   */
  public void clearRecycle();

  /**
   * Close the client
   */
  public void close();
}
