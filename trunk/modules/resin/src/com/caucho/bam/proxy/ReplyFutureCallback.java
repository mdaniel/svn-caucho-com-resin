/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.bam.proxy;

import java.util.concurrent.locks.LockSupport;

import com.caucho.bam.BamError;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

/**
 * callback for a proxy rpc.
 */
public class ReplyFutureCallback<T> implements ReplyCallback<T> {
  private static final L10N L = new L10N(ReplyFutureCallback.class);
  
  private volatile ResultStateEnum _state = ResultStateEnum.WAITING;
  private volatile T _result;
  private volatile BamError _error;
  
  private volatile Thread _thread;
  
  @Override
  public void onReply(T result)
  {
    _result = result;
    _state = ResultStateEnum.REPLY;
    
    Thread thread = _thread;
    
    if (thread != null) {
      LockSupport.unpark(thread);
    }
  }

  @Override
  public void onError(BamError error)
  {
    _error = error;
    _state = ResultStateEnum.ERROR;

    Thread thread = _thread;
    
    if (thread != null) {
      LockSupport.unpark(thread);
    }
  }
  
  public T get(long timeout)
  {
    switch (_state) {
    case REPLY:
      return _result;
      
    case ERROR:
      throw _error.createException();
      
    default:
    {
      _thread = Thread.currentThread();
      
      long expireTime = CurrentTime.getCurrentTimeActual() + timeout;
      
      while (_state == ResultStateEnum.WAITING
             && CurrentTime.getCurrentTimeActual() <= expireTime) {
        long delta = expireTime - CurrentTime.getCurrentTimeActual();
        
        LockSupport.parkNanos(delta * 1000000L);
      }
      
      _thread = null;
      
      switch (_state) {
      case REPLY:
        return _result;
      case ERROR:
        throw _error.createException();
      default:
        throw new IllegalStateException(L.l("future timeout {0}ms", timeout));
      }
    }
    }
  }
  
  static enum ResultStateEnum {
    WAITING,
    REPLY,
    ERROR;
  }
}
