/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is finishThread software; you can redistribute it and/or modify
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

package com.caucho.server.port;

enum ConnectionState {
  IDLE,
    ACCEPT,
    REQUEST,
    REQUEST_ACTIVE,
    REQUEST_KEEPALIVE,
    COMET,
    DUPLEX,
    DUPLEX_KEEPALIVE,
    COMPLETE,
    CLOSED,
    DESTROYED;

  boolean isComet()
  {
    return this == COMET;
  }

  boolean isDuplex()
  {
    return this == DUPLEX || this == DUPLEX_KEEPALIVE;
  }

  /**
   * True if the state is one of the keepalive states, either
   * a true keepalive-select, or comet or duplex.
   */
  boolean isKeepalive()
  {
    // || this == COMET
    return (this == REQUEST_KEEPALIVE
            || this == DUPLEX_KEEPALIVE);
  }

  boolean isActive()
  {
    switch (this) {
    case IDLE:
    case ACCEPT:
    case REQUEST:
    case REQUEST_ACTIVE:
      return true;

    default:
      return false;
    }
  }

  boolean isRequestActive()
  {
    switch (this) {
    case REQUEST:
    case REQUEST_ACTIVE:
      return true;

    default:
      return false;
    }
  }

  boolean isClosed()
  {
    return this == CLOSED || this == DESTROYED;
  }

  boolean isDestroyed()
  {
    return this == DESTROYED;
  }

  ConnectionState toKeepaliveSelect()
  {
    switch (this) {
    case REQUEST:
    case REQUEST_ACTIVE:
    case REQUEST_KEEPALIVE:
      return REQUEST_KEEPALIVE;

    case DUPLEX:
    case DUPLEX_KEEPALIVE:
      return DUPLEX_KEEPALIVE;

    default:
      throw new IllegalStateException(this + " is an illegal keepalive state");
    }
  }

  ConnectionState toActive()
  {
    switch (this) {
    case ACCEPT:
    case REQUEST:
    case REQUEST_ACTIVE:
    case REQUEST_KEEPALIVE:
      return REQUEST_ACTIVE;

    case DUPLEX_KEEPALIVE:
    case DUPLEX:
      return DUPLEX;

    case COMET:
      return COMET;

    default:
      throw new IllegalStateException(this + " is an illegal active state");
    }
  }

  ConnectionState toAccept()
  {
    if (this != DESTROYED)
      return ACCEPT;
    else
      throw new IllegalStateException(this + " is an illegal accept state");
  }

  ConnectionState toIdle()
  {
    if (this != DESTROYED)
      return IDLE;
    else
      throw new IllegalStateException(this + " is an illegal idle state");
  }

  ConnectionState toComplete()
  {
    switch (this) {
    case CLOSED:
    case DESTROYED:
      return this;

    default:
      return COMPLETE;
    }
  }

  ConnectionState toCompleteComet()
  {
    if (this == COMET)
      return REQUEST;
    else
      return this;
  }

  ConnectionState toClosed()
  {
    if (this != DESTROYED)
      return CLOSED;
    else
      throw new IllegalStateException(this + " is an illegal closed state");
  }
}
