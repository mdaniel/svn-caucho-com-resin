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
 * @author Reza Rahman
 */
package com.caucho.config.timer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Timer;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Producer;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Trigger;
import com.caucho.java.JavaWriter;
import com.caucho.config.timer.CronExpression;
import com.caucho.config.timer.CronTrigger;
import com.caucho.config.timer.EjbTimer;
import com.caucho.config.timer.TimerTask;
import com.caucho.config.timer.TimeoutInvoker;
import com.caucho.config.timer.Scheduler;
import com.caucho.util.L10N;

/**
 * Processes EJB declarative scheduling. The scheduling functionality can be
 * applied to bean types that are not EJBs.
 * 
 * @author Reza Rahman
 */
public class ScheduleIntrospector {
  private static final L10N L = new L10N(ScheduleIntrospector.class);

  public ScheduleIntrospector()
  {
  }

  /**
   * Introspects the method for locking attributes.
   */
  public ArrayList<TimerTask> introspect(TimeoutCaller caller,
                                         AnnotatedType<?> type)
  {
    ArrayList<TimerTask> timers = null;
    
    for (AnnotatedMethod method : type.getMethods()) {
      Schedules schedules = method.getAnnotation(Schedules.class);

      if (schedules != null) {
        if (timers == null)
          timers = new ArrayList<TimerTask>();

        for (Schedule schedule : schedules.value()) {
          addSchedule(timers, schedule, caller, method);
        }
      }

      Schedule schedule = method.getAnnotation(Schedule.class);

      if (schedule != null) {
        if (timers == null)
          timers = new ArrayList<TimerTask>();

        addSchedule(timers, schedule, caller, method);
      }
    }

    return timers;
  }

  private void addSchedule(ArrayList<TimerTask> timers,
                           Schedule schedule,
                           TimeoutCaller caller,
                           AnnotatedMethod method)
  {
    CronExpression cronExpression
      = new CronExpression(schedule.second(),
                           schedule.minute(),
                           schedule.hour(),
                           schedule.dayOfWeek(),
                           schedule.dayOfMonth(),
                           schedule.month(),
                           schedule.year());
        
    Trigger trigger = new CronTrigger(cronExpression, -1, -1);
    EjbTimer ejbTimer = new EjbTimer();

    TimeoutInvoker timeout
      = new MethodTimeoutInvoker(caller, method.getJavaMember());

    TimerTask timer
      = new TimerTask(timeout, ejbTimer, cronExpression, trigger,
                      schedule.info());

    ejbTimer.setScheduledTask(timer);

    timers.add(timer);
  }
}
