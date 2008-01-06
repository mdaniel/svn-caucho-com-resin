/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.resources;

import com.caucho.config.ConfigException;
import com.caucho.config.types.CronType;
import com.caucho.jca.AbstractResourceAdapter;
import com.caucho.log.Log;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The cron resources starts application Work tasks at cron-specified
 * intervals.
 */
public class CronResource extends AbstractResourceAdapter
  implements AlarmListener
{
  private static final L10N L = new L10N(CronResource.class);
  private static final Logger log = Log.open(CronResource.class);

  @Resource
  private Executor _threadPool;

  private ClassLoader _loader;
  
  private CronType _cron;
  
  private Runnable _work;
  
  private WorkManager _workManager;
  private Alarm _alarm;

  private volatile boolean _isActive;

  /**
   * Constructor.
   */
  public CronResource()
  {
    _loader = Thread.currentThread().getContextClassLoader();
  }

  /**
   * Sets the cron interval.
   */
  public void setCron(CronType cron)
  {
    _cron = cron;
  }

  /**
   * Sets the work task.
   */
  public void setWork(Runnable work)
  {
    _work = work;
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_cron == null)
      throw new ConfigException(L.l("CronResource needs a <cron> interval."));
    
    if (_work == null)
      throw new ConfigException(L.l("CronResource needs a <work> task."));
  }

  /**
   * Starting.
   */
  public void start(BootstrapContext ctx)
  {
    _workManager = ctx.getWorkManager();

    long now = Alarm.getCurrentTime();
    
    long nextTime = _cron.nextTime(now);

    _isActive = true;

    _alarm = new Alarm("cron-resource", this, nextTime - now);
  }

  /**
   * The runnable.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (! _isActive)
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_loader);
      
      log.fine("cron work scheduled: " + _work);

      if (_work instanceof Work)
	_workManager.scheduleWork((Work) _work);
      else
	_threadPool.execute(_work);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    long now = Alarm.getCurrentTime();
    long nextTime = _cron.nextTime(now);

    _alarm.queue(nextTime - now);
  }

  /**
   * Stopping.
   */
  public void stop()
  {
    _isActive = false;
    _alarm.dequeue();
  }
}
