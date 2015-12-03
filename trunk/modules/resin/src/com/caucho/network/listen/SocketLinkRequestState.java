/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.network.listen;

import java.util.concurrent.atomic.AtomicReference;

import com.caucho.inject.Module;

/**
 * State for an external request.
 */
@Module
enum SocketLinkRequestState {
  /**
   * The allocated, ready to accept state
   */
  INIT {
    @Override
    public boolean isAllowIdle()
    {
      return true;
    }
    
    @Override
    boolean toAccept(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(INIT, REQUEST)) {
        return true;
      }
      else {
        return stateRef.get().toAccept(stateRef);
      }
    }
  },
  
  /**
   * Handling a request
   */
  REQUEST {
    @Override
    public boolean isAllowIdle()
    {
      return true;
    }
    
    @Override
    boolean toIdle(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(REQUEST, INIT)) {
        return true;
      }
      else {
        return stateRef.get().toIdle(stateRef);
      }
    }
    
    @Override
    boolean toStartKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(REQUEST, KEEPALIVE_START)) {
        return true;
      }
      else {
        return stateRef.get().toStartKeepalive(stateRef);
      }
    }
    
    @Override
    boolean toAsyncStart(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(REQUEST, ASYNC_START)) {
        return true;
      }
      else {
        return stateRef.get().toAsyncStart(stateRef);
      }
    }
    
    /*
    @Override
    boolean toAsyncWake(AtomicReference<SocketLinkRequestState> stateRef)
    {
      // server/1lda
      return false;
    }
    */
  },
  
  /**
   * Keepalive select started, but original thread not done.
   */
  KEEPALIVE_START {
    @Override
    boolean toWakeKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(KEEPALIVE_START, KEEPALIVE_WAKE)) {
        return false;
      }
      else {
        return stateRef.get().toWakeKeepalive(stateRef);
      }
    }
    
    @Override
    boolean toSuspendKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(KEEPALIVE_START, KEEPALIVE_SUSPEND)) {
        return true;
      }
      else {
        return stateRef.get().toSuspendKeepalive(stateRef);
      }
    }
    
    @Override
    boolean toDestroy(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(this, DESTROY)) {
        return true;
      }
      else {
        return stateRef.get().toDestroy(stateRef);
      }
    }
  },
  
  /**
   * Keepalive select/detached
   */
  KEEPALIVE_WAKE {
    @Override
    boolean toSuspendKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(KEEPALIVE_WAKE, REQUEST)) {
        return false;
      }
      else {
        return stateRef.get().toSuspendKeepalive(stateRef);
      }
    }
    
    @Override
    boolean toDestroy(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(this, DESTROY))
        return true;
      else
        return stateRef.get().toDestroy(stateRef);
    }
  },
  
  /**
   * keepalive suspended
   */
  KEEPALIVE_SUSPEND {
    @Override
    boolean toWakeKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(KEEPALIVE_SUSPEND, REQUEST)) {
        return true;
      }
      else {
        return stateRef.get().toWakeKeepalive(stateRef);
      }
    }
    
    @Override
    boolean toDestroy(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(this, DESTROY))
        return true;
      else
        return stateRef.get().toDestroy(stateRef);
    }
  },
 
  ASYNC_START {
    @Override
    public boolean isAsyncStarted()
    {
      return true;
    }
    
    @Override
    boolean toAsyncSuspendThread(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(ASYNC_START, SUSPEND)) {
        return true;
      }
      else {
        return stateRef.get().toAsyncSuspendThread(stateRef);
      }
    }
    
    @Override
    boolean toAsyncSuspendRequest(AtomicReference<SocketLinkRequestState> stateRef)
    {
      return true;
    }
    
    @Override
    boolean toAsyncWake(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(ASYNC_START, ASYNC_WAKE)) {
        return false;
      }
      else {
        return stateRef.get().toAsyncWake(stateRef);
      }
    }
  },
  
  ASYNC_WAKE {
    @Override
    public boolean isAsyncWake()
    {
      return true;
    }
    
    @Override
    boolean toAsyncSuspendThread(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(ASYNC_WAKE, REQUEST)) {
        return false;
      }
      else {
        return stateRef.get().toAsyncSuspendThread(stateRef);
      }
    }
    
    @Override
    boolean toAsyncSuspendRequest(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(ASYNC_WAKE, REQUEST)) {
        return false;
      }
      else {
        return stateRef.get().toAsyncSuspendRequest(stateRef);
      }
    }
  },
  
  /**
   * Comet suspend
   */
  SUSPEND {
    @Override
    public boolean isAsyncStarted()
    {
      return true;
    }
    
    @Override
    boolean toAsyncWake(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(SUSPEND, REQUEST)) {
        return true;
      }
      else {
        return stateRef.get().toAsyncWake(stateRef);
      }
    }
    
    @Override
    boolean toDestroy(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(this, DESTROY))
        return true;
      else
        return stateRef.get().toDestroy(stateRef);
    }
  },
  
  /**
   * destroyed state
   */
  DESTROY {
    @Override
    public boolean isDestroyed()
    {
      return true;
    }
    
    @Override
    boolean toDestroy(AtomicReference<SocketLinkRequestState> stateRef)
    {
      return false;
    }
  };

  public boolean isAllowIdle()
  {
    return false;
  }
  
  public boolean isAsyncWake()
  {
    return false;
  }
  
  boolean toAccept(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toStartKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toSuspendKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toWakeKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toAsyncStart(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toAsyncSuspendThread(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toAsyncSuspendRequest(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toAsyncWake(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException("async dispatch is not valid outside of an async cycle.  Current state: " + toString());
  }

  boolean toIdle(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }
  
  boolean toDestroy(AtomicReference<SocketLinkRequestState> stateRef)
  {
    if (stateRef.compareAndSet(this, DESTROY))
      return false;
    else
      return stateRef.get().toDestroy(stateRef);
  }

  public boolean isDestroyed()
  {
    return false;
  }

  public boolean isAsyncStarted()
  {
    return false;
  }
}
