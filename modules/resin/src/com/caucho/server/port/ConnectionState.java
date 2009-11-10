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

public enum ConnectionState {
  IDLE,                 // TcpConnection in free list
  INIT,                 // allocated, ready to accept
  ACCEPT,               // accepting
  REQUEST_READ,         // after accept, but before any data is read
  REQUEST_ACTIVE,       // processing a request
  REQUEST_NKA,          // processing a request, but keepalive forbidden
  REQUEST_KEEPALIVE,    // waiting for keepalive data
  
  COMET,                // processing an active comet service
  COMET_SUSPEND,        // suspended waiting for a wake
  COMET_COMPLETE,       // complete or timeout
  COMET_NKA,            // processing an active comet service
  COMET_SUSPEND_NKA,    // suspended waiting for a wake
  COMET_COMPLETE_NKA,   // complete or timeout
  
  DUPLEX,               // converted to a duplex/websocket
  DUPLEX_KEEPALIVE,     // waiting for duplex read data
  CLOSED,               // connection closed, ready for accept
  DESTROYED;            // connection destroyed

  //
  // predicates
  //

  boolean isComet()
  {
    switch (this) {
    case COMET:
    case COMET_SUSPEND:
    case COMET_NKA:
    case COMET_SUSPEND_NKA:
      return true;
    default:
      return false;
    }
  }

  boolean isCometActive()
  {
    return this == COMET || this == COMET_NKA;
  }

  public boolean isCometSuspend()
  {
    return this == COMET_SUSPEND || this == COMET_SUSPEND_NKA;
  }

  public boolean isCometComplete()
  {
    return this == COMET_COMPLETE || this == COMET_COMPLETE_NKA;
  }

  public boolean isDuplex()
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
    case ACCEPT:
    case REQUEST_READ:
    case REQUEST_ACTIVE:
    case REQUEST_NKA:
      return true;

    default:
      return false;
    }
  }

  boolean isRequestActive()
  {
    switch (this) {
    case REQUEST_READ:
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

  boolean isAllowKeepalive()
  {
    switch (this) {
    case REQUEST_READ:
    case REQUEST_ACTIVE:
    case REQUEST_KEEPALIVE:
      
    case COMET:
    case COMET_SUSPEND:
    case COMET_COMPLETE:
      return true;
      
    default:
      return false;
    }
  }

  //
  // state changes
  //

  /**
   * Convert from the idle (pooled) or closed state to the initial state
   * before accepting a connection.
   */
  ConnectionState toInit()
  {
    if (this == IDLE || this == CLOSED)
      return INIT;
    else
      throw new IllegalStateException(this + " is an illegal init state");
  }

  /**
   * Change to the accept state.
   */
  ConnectionState toAccept()
  {
    switch (this) {
    case INIT:
    case CLOSED:
      return ACCEPT;
    default:
      throw new IllegalStateException(this + " is an illegal accept state");
    }
  }

  ConnectionState toActive()
  {
    switch (this) {
    case ACCEPT:
    case REQUEST_READ:
    case REQUEST_ACTIVE:
    case REQUEST_KEEPALIVE:
      return REQUEST_ACTIVE;
      
    case REQUEST_NKA:
      return REQUEST_NKA;

      /*
    case DUPLEX_KEEPALIVE:
    case DUPLEX:
      return DUPLEX;
      */

    case COMET:
    case COMET_SUSPEND:
      return COMET;

    case COMET_NKA:
    case COMET_SUSPEND_NKA:
      return COMET_NKA;

    default:
      throw new IllegalStateException(this + " is an illegal active state");
    }
  }

  ConnectionState toKillKeepalive()
  {
    switch (this) {
    case REQUEST_ACTIVE:
      return REQUEST_NKA;
    case COMET:
      return COMET_NKA;
    case COMET_SUSPEND:
      return COMET_SUSPEND_NKA;
    case COMET_COMPLETE:
      return COMET_COMPLETE_NKA;
    default:
      return this;
    }
  }

  ConnectionState toKeepalive()
  {
    switch (this) {
    case REQUEST_READ:
    case REQUEST_ACTIVE:
    case REQUEST_KEEPALIVE:
      return REQUEST_KEEPALIVE;

    case DUPLEX:
      return DUPLEX_KEEPALIVE;

    default:
      return this;
    }
  }

  ConnectionState toKeepaliveSelect()
  {
    switch (this) {
    case ACCEPT:  // XXX: unsure if this should be allowed
    case REQUEST_READ:
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

  //
  // comet
  //

  ConnectionState toComet()
  {
    switch (this) {
    case REQUEST_READ:
    case REQUEST_ACTIVE:
    case REQUEST_KEEPALIVE:
      return COMET;
      
    case REQUEST_NKA:
      return COMET_NKA;

    default:
      throw new IllegalStateException(this + " cannot switch to comet");
    }
  }

  ConnectionState toCometComplete()
  {
    switch (this) {
    case COMET:
    case COMET_SUSPEND:
    case COMET_COMPLETE:
      return COMET_COMPLETE;
      
    case COMET_NKA:
    case COMET_SUSPEND_NKA:
    case COMET_COMPLETE_NKA:
      return COMET_COMPLETE_NKA;
      
    case CLOSED:
    case DESTROYED:
      return this;

    default:
      throw new IllegalStateException(this + " cannot complete comet");
    }
  }

  //
  // duplex/websocket
  //

  ConnectionState toDuplex()
  {
    switch (this) {
    case REQUEST_READ:
    case REQUEST_ACTIVE:
    case REQUEST_KEEPALIVE:
    case REQUEST_NKA:
      return DUPLEX;

    default:
      throw new IllegalStateException(this + " cannot switch to duplex/websocket");
    }
  }

  ConnectionState toDuplexActive()
  {
    switch (this) {
    case DUPLEX:
    case DUPLEX_KEEPALIVE:
      return DUPLEX;

    default:
      throw new IllegalStateException(this + " cannot switch to duplex/websocket");
    }
  }

  //
  // idle/close
  //

  ConnectionState toIdle()
  {
    if (this == CLOSED)
      return IDLE;
    else
      throw new IllegalStateException(this + " is an illegal idle state");
  }

  ConnectionState toClosed()
  {
    if (this != DESTROYED)
      return CLOSED;
    else
      return this;
  }
}
