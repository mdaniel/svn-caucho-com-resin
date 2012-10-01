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

package com.caucho.env.thread;

import com.caucho.env.thread2.AbstractTaskWorker2;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
abstract public class AbstractTaskWorker 
  extends AbstractTaskWorker2
  implements TaskWorker {
  private ThreadPool _threadPool;
  
  protected AbstractTaskWorker()
  {
    this(ThreadPool.getCurrent());
  }

  protected AbstractTaskWorker(ThreadPool threadPool)
  {
    this(Thread.currentThread().getContextClassLoader(), threadPool);
  }

  protected AbstractTaskWorker(ClassLoader classLoader,
                               ThreadPool threadPool)
  {
    super(classLoader);
    
    _threadPool = threadPool;
  }

  @Override
  protected void startWorkerThread()
  {
    _threadPool.schedulePriority(this);
  }

  @Override
  protected void unpark(Thread thread)
  {
    _threadPool.scheduleUnpark(thread);
  }
}
