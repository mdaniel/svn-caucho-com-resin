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

package com.caucho.log;

import java.io.IOException;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.caucho.env.actor.AbstractActorProcessor;
import com.caucho.env.actor.ActorProcessor;
import com.caucho.env.actor.ValueActorQueue;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

/**
 * Configures a log handler
 */
abstract public class AbstractLogHandler extends Handler {
  private final ValueActorQueue<LogRecord> _logQueue
    = new ValueActorQueue<LogRecord>(256, new LogQueue());

  private Filter _filter;

  /**
   * Sets the filter.
   */
  @Override
  public void setFilter(Filter filter)
  {
    _filter = filter;
  }
  
  public Filter getFilter()
  {
    return _filter;
  }

  /**
   * Publishes the record.
   */
  @Override
  public final void publish(LogRecord record)
  {
    if (! isLoggable(record))
        return;
    
    Filter filter = getFilter();
    
    if (filter != null && ! filter.isLoggable(record))
      return;
    
    if (record == null) {
      System.out.println(this + ": no record");
      return;
    }
    
    //synchronized (this) {
      processPublish(record);
      processFlush();
    //}
    
    /*
    _logQueue.offer(record);
    
    if (CurrentTime.isTest()) {
      waitForEmpty();
    }
    */
  }
  
  private void waitForEmpty()
  {
    _logQueue.wake();
      
    for (int i = 0; i < 20 && ! _logQueue.isEmpty(); i++) {
      try {
        Thread.sleep(1);
      } catch (Exception e) {
      }
    }
  }
    
  abstract protected void processPublish(LogRecord record);
  
  abstract protected void processFlush();
  
  protected void printMessage(WriteStream os,
                              String message, 
                              Object []parameters)
    throws IOException
  {
    if (parameters == null || parameters.length == 0) {
      os.println(message);
    }
    else {
      os.println(L10N.fillMessage(message, parameters));
    }
  }

  /**
   * Flushes the buffer.
   */
  @Override
  public void flush()
  {
  }

  /**
   * Closes the handler.
   */
  @Override
  public void close()
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
  
  private class LogQueue extends AbstractActorProcessor<LogRecord>
  {
    @Override
    public String getThreadName()
    {
      Thread thread = Thread.currentThread();
      
      return getClass().getSimpleName() + "-" + thread.getId();
    }

    @Override
    public void process(LogRecord value) throws Exception
    {
      processPublish(value);
    }

    @Override
    public void onProcessComplete() throws Exception
    {
      processFlush();
    }
  }
}
