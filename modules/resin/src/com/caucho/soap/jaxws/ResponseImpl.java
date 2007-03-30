/*
* Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
* @author Emil Ong
*/

package com.caucho.soap.jaxws;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.ws.Response;

public class ResponseImpl<T> implements Response<T>  {
  private T _value;
  private Map<String,Object> _context;
  private boolean _done = false;
  private boolean _cancelled = false;

  /*
  public ResponseImpl(Class<T> type)
  {
  }*/
 
  public Map<String,Object> getContext()
  {
    return _context;
  }

  public void setContext(Map<String,? extends Object> context)
  {
    _context = (Map<String,Object>) context;
  }

  public T get()
    throws InterruptedException
  {
    synchronized(this) {
      while (! _done && ! _cancelled)
        wait();

      return _value;
    }
  }

  public T get(long timeout, TimeUnit unit)
    throws InterruptedException
  {
    synchronized(this) {
      if (! _done && ! _cancelled)
        wait(unit.toMillis(timeout));

      return _value;
    }
  }

  public void set(T value)
  {
    synchronized(this) {
      _done = true;
      _value = value;

      notify();
    }
  }

  public boolean cancel(boolean mayInterruptIfRunning)
  {
    synchronized(this) {
      _cancelled = true;

      notify();

      return true;
    }
  }

  public boolean isDone()
  {
    synchronized(this) {
      return _done;
    }
  }

  public boolean isCancelled()
  {
    synchronized(this) {
      return _cancelled;
    }
  }
}

