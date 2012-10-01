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

package com.caucho.amp.actor;

import java.util.concurrent.locks.LockSupport;

import com.caucho.amp.AmpQueryCallback;
import com.caucho.amp.stream.AmpError;
import com.caucho.bam.TimeoutException;
import com.caucho.util.CurrentTime;

/**
 * Handles the context for an actor, primarily including its
 * query map.
 */
public final class AmpQueryFuture implements AmpQueryCallback {
  private final long _timeout;

  private volatile Object _result;
  private volatile AmpError _error;
  private volatile ResultState _resultState = ResultState.UNSET;
  private volatile Thread _thread;

  public AmpQueryFuture(long timeout)
  {
    _timeout = timeout;
  }

  public final Object getResult()
  {
    return _result;
  }

  public final AmpError getError()
  {
    return _error;
  }

  public final Object get()
    throws TimeoutException
  {
    ResultState resultState = _resultState;
    
    switch (resultState) {
    case RESULT:
      return _result;
    case ERROR:
      throw new RuntimeException(String.valueOf(getError()));
      
    case UNSET:
      resultState = waitFor(_timeout);
      
      switch (resultState) {
      case UNSET:
        throw new TimeoutException(this + " query timeout");
      case ERROR:
        throw new RuntimeException(String.valueOf(getError()));
      case RESULT:
        return _result;
      }
      
    default:
      throw new IllegalStateException();
    }
  }

  private ResultState waitFor(long timeout)
  {
    _thread = Thread.currentThread();
    long now = CurrentTime.getCurrentTimeActual();
    long expires = now + timeout;

    while (_resultState == ResultState.UNSET
           && CurrentTime.getCurrentTimeActual() < expires) {
      try {
        Thread.interrupted();
        LockSupport.parkUntil(expires);
      } catch (Exception e) {
      }
    }
    
    _thread = null;

    return _resultState;
  }

  @Override
  public void onQueryResult(AmpActorRef to, 
                            AmpActorRef from, 
                            Object result)
  {
    _result = result;
    _resultState = ResultState.RESULT;

    Thread thread = _thread;
    if (thread != null)
      LockSupport.unpark(thread);
  }

  @Override
  public void onQueryError(AmpActorRef to,
                           AmpActorRef from,
                           AmpError error)
  {
    _error = error;
    _resultState = ResultState.ERROR;

    Thread thread = _thread;
    if (thread != null)
      LockSupport.unpark(thread);
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _result + "]");
  }
  
  private enum ResultState {
    UNSET,
    RESULT,
    ERROR;
  }
}
