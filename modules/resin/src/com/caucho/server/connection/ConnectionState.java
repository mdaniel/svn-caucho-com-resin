/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.server.connection;

enum ConnectionState {
  INIT {                // allocated, ready to accept
    @Override
    ConnectionState toInit() 
    { 
      return INIT; 
    }

    @Override
    ConnectionState toAccept() 
    { 
      return ACCEPT; 
    }
  },

  ACCEPT {               // accepting
    @Override
    boolean isActive() { return true; }

    @Override
    ConnectionState toActive(TcpConnection conn) 
    { 
      return REQUEST_ACTIVE; 
    }
  },

  REQUEST_READ {         // after accept, but before any data is read
    @Override
    boolean isActive() { return true; }

    @Override
    boolean isRequestActive() { return true; }

    @Override
    boolean isAllowKeepalive() { return true; }

    @Override
    ConnectionState toActive(TcpConnection conn) 
    { 
      return REQUEST_ACTIVE; 
    }

    @Override
    ConnectionState toKeepalive(TcpConnection conn)
    {
      conn.getPort().keepaliveBegin();

      return REQUEST_KEEPALIVE;
    }
  },

  REQUEST_ACTIVE {       // processing a request
    @Override
    boolean isActive() { return true; }

    @Override
    boolean isRequestActive() { return true; }

    @Override
    boolean isAllowKeepalive() { return true; }

    @Override
    ConnectionState toPostActive()
    {
      return REQUEST_READ; 
    }

    @Override
    ConnectionState toKillKeepalive()
    {
      return REQUEST_NKA;
    }

    @Override
    ConnectionState toKeepalive(TcpConnection conn)
    {
      conn.getPort().keepaliveBegin();

      return REQUEST_KEEPALIVE;
    }

    @Override
    ConnectionState toComet()
    {
      return COMET;
    }

    @Override
    ConnectionState toDuplex()
    {
      return DUPLEX;
    }
  },

  REQUEST_NKA {          // processing a request, but keepalive forbidden
    @Override
    boolean isActive() { return true; }

    @Override
    ConnectionState toPostActive()
    {
      return this; 
    }

    @Override
    ConnectionState toComet()
    {
      return COMET_NKA;
    }

    @Override
    ConnectionState toDuplex()
    {
      return DUPLEX;
    }
  },

  REQUEST_KEEPALIVE {   // waiting for keepalive data
    @Override
    boolean isKeepalive() { return true; }

    @Override
    boolean isAllowKeepalive() { throw new IllegalStateException(); }

    @Override
    ConnectionState toActive(TcpConnection conn) 
    { 
      conn.getPort().keepaliveEnd(conn);

      return REQUEST_ACTIVE; 
    }

    @Override
    ConnectionState toKeepaliveSelect()
    {
      return REQUEST_KEEPALIVE_SELECT;
    }

    @Override
    ConnectionState toClosed(TcpConnection conn)
    {
      conn.getPort().keepaliveEnd(conn);

      return CLOSED;
    }
  },    

  REQUEST_KEEPALIVE_SELECT {   // waiting for keepalive data (select)
    @Override
    boolean isKeepalive() { return true; }

    @Override
    boolean isAllowKeepalive() { throw new IllegalStateException(); }

    @Override
    ConnectionState toActive(TcpConnection conn) 
    { 
      conn.getPort().keepaliveEnd(conn);

      return REQUEST_ACTIVE; 
    }

    @Override
    ConnectionState toClosed(TcpConnection conn)
    {
      conn.getPort().keepaliveEnd(conn);

      return CLOSED;
    }
  },    

  COMET {                // processing an active comet service
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometActive() { return true; }

    @Override
    boolean isCometSuspend() { return true; }

    @Override
    boolean isAllowKeepalive() { return true; }

    @Override
    ConnectionState toActive(TcpConnection conn) 
    { 
      return COMET;
    }

    @Override
    ConnectionState toPostActive()
    {
      return this; 
    }

    @Override
    ConnectionState toKillKeepalive()
    {
      return COMET_NKA;
    }

    @Override
    ConnectionState toCometSuspend()
    {
      return COMET_SUSPEND;
    }

    @Override
    ConnectionState toCometComplete()
    {
      return COMET_COMPLETE;
    }
  },

  COMET_SUSPEND {        // suspended waiting for a wake
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometSuspend() { return true; }

    @Override
    boolean isAllowKeepalive() { return true; }

    @Override
    ConnectionState toKillKeepalive()
    {
      return COMET_SUSPEND_NKA;
    }

    @Override
    ConnectionState toCometResume()
    {
      return COMET;
    }

    @Override
    ConnectionState toDestroy(TcpConnection conn)
    {
      throw new IllegalStateException();
    }
  },

  COMET_COMPLETE {       // complete or timeout
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometComplete() { return true; }

    @Override
    boolean isAllowKeepalive() { return true; }

    @Override
    ConnectionState toActive(TcpConnection conn) 
    { 
      return REQUEST_ACTIVE;
    }

    @Override
    ConnectionState toPostActive()
    {
      return this; 
    }

    @Override
    ConnectionState toKillKeepalive()
    {
      return COMET_COMPLETE_NKA;
    }

    @Override
    ConnectionState toKeepalive(TcpConnection conn)
    {
      conn.getPort().keepaliveBegin();

      return REQUEST_KEEPALIVE;
    }
  },

  COMET_NKA {            // processing an active comet service
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometActive() { return true; }

    @Override
    boolean isCometSuspend() { return true; }

    @Override
    ConnectionState toActive(TcpConnection conn) 
    { 
      return COMET_NKA;
    }

    @Override
    ConnectionState toPostActive()
    {
      return this; 
    }

    @Override
    ConnectionState toCometSuspend()
    {
      return COMET_SUSPEND_NKA;
    }

    @Override
    ConnectionState toCometComplete()
    {
      return COMET_COMPLETE_NKA;
    }
  },

  COMET_SUSPEND_NKA {    // suspended waiting for a wake
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometSuspend() { return true; }

    @Override
    ConnectionState toCometResume()
    {
      return COMET;
    }

    @Override
    ConnectionState toDestroy(TcpConnection conn)
    {
      throw new IllegalStateException();
    }
  },

  COMET_COMPLETE_NKA {   // complete or timeout
    @Override
    boolean isComet() { return true; }

    @Override
    boolean isCometComplete() { return true; }

    @Override
    ConnectionState toPostActive()
    {
      return this; 
    }
  },

  DUPLEX {               // converted to a duplex/websocket
    @Override
    boolean isDuplex() { return true; }

    @Override
    ConnectionState toKeepalive(TcpConnection conn)
    {
      conn.getPort().keepaliveBegin();

      return REQUEST_KEEPALIVE;
    }

    @Override
    ConnectionState toDuplexActive(TcpConnection conn)
    {
      return DUPLEX;
    }
  },

  DUPLEX_KEEPALIVE {     // waiting for duplex read data
    @Override
    boolean isDuplex() { return true; }

    @Override
    boolean isKeepalive() { return true; }

    @Override
    ConnectionState toDuplexActive(TcpConnection conn)
    {
      conn.getPort().keepaliveEnd(conn);

      return DUPLEX;
    }

    @Override
    ConnectionState toClosed(TcpConnection conn)
    {
      conn.getPort().keepaliveEnd(conn);

      return CLOSED;
    }
  },

  CLOSED {               // connection closed, ready for accept
    @Override
    boolean isClosed() { return true; }

    @Override
    boolean isAllowIdle() { return true; }

    @Override
    ConnectionState toAccept() 
    { 
      return ACCEPT; 
    }

    @Override
    ConnectionState toIdle()
    {
      return IDLE;
    }
  },

  IDLE {                // TcpConnection in free list 
    @Override
    boolean isIdle() { return true; }

    @Override
    ConnectionState toInit() 
    { 
      return INIT; 
    }

    @Override
    ConnectionState toDestroy(TcpConnection conn)
    {
      throw new IllegalStateException(this + " is an illegal destroy state");
    }
  },                    

  DESTROYED {            // connection destroyed
    @Override
    boolean isClosed() { return true; }

    @Override
    boolean isDestroyed() { return true; }

    @Override
    ConnectionState toIdle()
    {
      return this;
    }

    @Override
    ConnectionState toClosed(TcpConnection conn)
    {
      return this;
    }

    @Override
    ConnectionState toDestroy(TcpConnection conn)
    {
      return this;
    }
  };

  //
  // predicates
  //

  boolean isIdle()
  {
    return false;
  }
  
  boolean isComet()
  {
    return false;
  }

  boolean isCometActive()
  {
    return false;
  }

  boolean isCometSuspend()
  {
    return false;
  }

  boolean isCometComplete()
  {
    return false;
  }

  boolean isDuplex()
  {
    return false;
  }

  /**
   * True if the state is one of the keepalive states, either
   * a true keepalive-select or duplex.
   */
  boolean isKeepalive()
  {
    return false;
  }

  boolean isActive()
  {
    return false;
  }

  boolean isRequestActive()
  {
    return false;
  }

  boolean isClosed()
  {
    return false;
  }

  boolean isDestroyed()
  {
    return false;
  }

  boolean isAllowKeepalive()
  {
    return false;
  }

  boolean isAllowIdle()
  { 
    return false; 
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
    throw new IllegalStateException(this + " is an illegal init state");
  }

  /**
   * Change to the accept state.
   */
  ConnectionState toAccept()
  {
    throw new IllegalStateException(this + " is an illegal accept state");
  }

  ConnectionState toActive(TcpConnection conn)
  {
    /*
    case COMET_COMPLETE:
    case COMET_COMPLETE_NKA:
      return this;
      */

    throw new IllegalStateException(this + " is an illegal active state");
  }

  ConnectionState toPostActive()
  {
    throw new IllegalStateException(this + " is an illegal active state");
  }

  ConnectionState toKillKeepalive()
  {
    return this;
  }

  ConnectionState toKeepalive(TcpConnection conn)
  {
    throw new IllegalStateException(this + " is an illegal active state");
  }

  ConnectionState toKeepaliveSelect()
  {
    throw new IllegalStateException(this + " is an illegal keepalive state");
  }

  //
  // comet
  //

  ConnectionState toComet()
  {
    throw new IllegalStateException(this + " cannot switch to comet");
  }

  ConnectionState toCometResume()
  {
    throw new IllegalStateException(this + " cannot resume comet");
  }

  ConnectionState toCometSuspend()
  {
    throw new IllegalStateException(this + " cannot suspend comet");
  }
    
  ConnectionState toCometComplete()
  {
    throw new IllegalStateException(this + " cannot complete comet");
  }

  //
  // duplex/websocket
  //

  ConnectionState toDuplex()
  {
    throw new IllegalStateException(this + " cannot switch to duplex/websocket");
  }

  ConnectionState toDuplexActive(TcpConnection conn)
  {
    throw new IllegalStateException(this + " cannot switch to duplex/websocket");
  }

  //
  // idle/close
  //

  ConnectionState toIdle()
  {
    throw new IllegalStateException(this + " is an illegal idle state");
  }

  ConnectionState toClosed(TcpConnection conn)
  {
    return CLOSED;
  }

  ConnectionState toDestroy(TcpConnection conn)
  {
    toClosed(conn);

    conn.getPort().destroy(conn);

    return DESTROYED;
  }
}
