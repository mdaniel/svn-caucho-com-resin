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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Reza Rahman
 */
package com.caucho.ejb.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ScheduleExpression;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.CronType;
import com.caucho.config.types.Trigger;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;

/**
 * Resin scheduler.
 * 
 * @author Reza Rahman
 */
// TODO This should probably be a server/cluster-wide managed bean itself; I
// tried but I could not figure out how to make that happen. I
// am also not sure this is the right package/module for this.
public class Scheduler {
  private static final L10N L = new L10N(ScheduledTask.class);
  private static final Logger log = Logger.getLogger(Scheduler.class.getName());

  private static ClassLoader _loader = Thread.currentThread()
      .getContextClassLoader();
  private static InjectManager _injectionManager = InjectManager.create();

  /**
   * Schedules a task.
   * 
   * @param targetBean
   *          The target bean instance to invoke.
   * @param targetMethod
   *          The target method to invoke.
   * @param schedule
   *          The schedule.
   * @param info
   *          Any info to be passed to the invocation target.
   * @param persistent
   *          If the schedule is to be persisted.
   */
  @SuppressWarnings("unchecked")
  public static void schedule(final Class targetBean,
      final Method targetMethod, final ScheduleExpression schedule,
      final Object info, final boolean persistent)
  {
    new ScheduledTask(new Runnable() {
      @Override
      public void run()
      {
        try {
          targetMethod.invoke(_injectionManager.getReference(targetBean));
        } catch (IllegalArgumentException e) {
          log.log(Level.WARNING, L.l("Cannot invoke scheduled method."), e);
        } catch (IllegalAccessException e) {
          log.log(Level.WARNING, L.l("Cannot invoke scheduled method."), e);
        } catch (InvocationTargetException e) {
          log.log(Level.WARNING, L
              .l("The scheduled method threw an unexpected exception."), e);
        }
      }
    }, new CronType("*  *  *  *  *"));
  }

  /**
   * Scheduled task.
   * 
   * @author Reza Rahman
   */
  private static class ScheduledTask implements AlarmListener {
    private Runnable _task;
    private Trigger _trigger;

    /**
     * Constructs a new scheduled task.
     * 
     * @param task
     *          The task to execute.
     * @param trigger
     *          The trigger for the task.
     */
    public ScheduledTask(Runnable task, Trigger trigger)
    {
      _task = task;
      _trigger = trigger;

      long now = Alarm.getCurrentTime();

      long nextTime = _trigger.nextTime(now + 500);

      new Alarm("cron-resource", this, nextTime - now);
    }

    /**
     * The runnable to handle a triggered alarm.
     */
    public void handleAlarm(Alarm alarm)
    {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(_loader);

        ThreadPool.getCurrent().schedule(_task);

        long now = Alarm.getExactTime();
        long nextTime = _trigger.nextTime(now + 500);

        alarm.queue(nextTime - now);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }
  }
}