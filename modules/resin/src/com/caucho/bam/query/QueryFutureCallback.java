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

package com.caucho.bam.query;

import java.io.Serializable;
import java.util.concurrent.locks.LockSupport;

import com.caucho.bam.BamError;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

/**
 * QueryFutureCallback is used to wait
 * for query callbacks.
 */
public class QueryFutureCallback extends AbstractQueryCallback {
  private static final L10N L = new L10N(QueryFutureCallback.class);
  
  private volatile ResultStateEnum _state = ResultStateEnum.WAITING;
  private volatile Serializable _result;
  private volatile BamError _error;
  
  private volatile Thread _thread;

  @Override
  public void onQueryResult(String to, String from, Serializable result)
  {
    _result = result;
    _state = ResultStateEnum.REPLY;
    
    Thread thread = _thread;
    
    if (thread != null) {
      LockSupport.unpark(thread);
    }
  }

  @Override
  public void onQueryError(String to,
                           String from,
                           Serializable payload,
                           BamError error)
  {
    _error = error;
    _state = ResultStateEnum.ERROR;

    Thread thread = _thread;
    
    if (thread != null) {
      LockSupport.unpark(thread);
    }
  }
  
  public Serializable get(long timeout)
  {
    _thread = Thread.currentThread();

    try {
      switch (_state) {
      case REPLY:
        return _result;

      case ERROR:
        throw _error.createException();

      default:
      {
        long expireTime = CurrentTime.getCurrentTimeActual() + timeout;
        
        do {
          LockSupport.parkUntil(expireTime);

          switch (_state) {
          case REPLY:
            return _result;
          case ERROR:
            throw _error.createException();
          }
        } while (CurrentTime.getCurrentTimeActual() < expireTime);
        
        throw new IllegalStateException(L.l(this + " future timeout: " + timeout + "ms"));
      }
      }
    } finally {
      _thread = null;
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
  
  static enum ResultStateEnum {
    WAITING,
    REPLY,
    ERROR;
  }
}
