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
package com.caucho.config.gen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Timer;

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
public class SchedulingCallChain extends AbstractCallChain {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(SchedulingCallChain.class);

  private EjbCallChain _next;

  private Schedule _schedule;
  private Schedules _schedules;

  @SuppressWarnings("unchecked")
  private Class _targetBean;
  private Method _targetMethod;

  public SchedulingCallChain(BusinessMethodGenerator businessMethod,
      EjbCallChain next)
  {
    super(next);

    _next = next;
  }

  /**
   * Returns true if the business method needs scheduling.
   */
  @Override
  public boolean isEnhanced()
  {
    return ((_schedule != null) || (_schedules != null));
  }

  /**
   * Introspects the method for locking attributes.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implementationMethod)
  {
    _schedule = apiMethod.getAnnotation(Schedule.class);

    if ((_schedule == null) && (implementationMethod != null)) {
      _schedule = implementationMethod.getAnnotation(Schedule.class);
    }

    if (_schedule != null) {
      _schedules = apiMethod.getAnnotation(Schedules.class);

      if ((_schedules == null) && (implementationMethod != null)) {
        _schedules = implementationMethod.getAnnotation(Schedules.class);
      }
    }

    if ((_schedule != null) || (_schedules != null)) {
      // TODO What happens when implementation method is null? Can it be null in
      // this case?
      if (implementationMethod != null) {
        _targetBean = implementationMethod.getDeclaringClass().getJavaClass();

        Method method = implementationMethod.getMethod();
        Class[] parameters = method.getParameterTypes();

        if (parameters.length != 0) {
          if (!(parameters.length == 1)
              || !(Timer.class.isAssignableFrom(parameters[0]))
              || !(parameters[0].isAssignableFrom(Timer.class))) {
            throw new ConfigException(L.l(
                "{0}.{1} is not a valid timeout method.",
                _targetBean.getName(), method.getName()));
          }
        }

        _targetMethod = method;
      }
    }
  }

  /**
   * Generates the class prologue.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void generatePrologue(JavaWriter out, HashMap map) throws IOException
  {
    if (((_schedule != null) || (_schedules != null))
        && (map.get("caucho.ejb.scheduling") == null)) {
      map.put("caucho.ejb.scheduling", "done");

      // TODO Should an alternate paradigm be used for this since this is not
      // really generating code?
      if (_schedule != null) {
        addSchedule(_schedule);
      } else {
        for (Schedule schedule : _schedules.value()) {
          addSchedule(schedule);
        }
      }

    }

    _next.generatePrologue(out, map);
  }

  private void addSchedule(final Schedule schedule)
  {
    CronExpression cronExpression = new CronExpression(schedule.second(),
        schedule.minute(), schedule.hour(), schedule.dayOfWeek(), schedule
            .dayOfMonth(), schedule.month(), schedule.year());
    Trigger trigger = new CronTrigger(cronExpression, -1, -1);
    EjbTimer timer = new EjbTimer();

    // TODO What really needs to be passed in is a unique reference to the bean
    // (maybe a JCDI Bean definition?), not just the bean class. Is there an
    // easy way to do that?
    TimeoutInvoker timeout = null;

    TimerTask scheduledTask = new TimerTask(timeout,
        timer, cronExpression, trigger, -1, -1, schedule.info());

    timer.setScheduledTask(scheduledTask);

    // TODO This should probably be an injection of the scheduler by JCDI.
    Scheduler.addTimerTask(scheduledTask);
  }
}
