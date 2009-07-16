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
import javax.ejb.ScheduleExpression;
import javax.ejb.Schedules;

import com.caucho.ejb.scheduler.Scheduler;
import com.caucho.java.JavaWriter;
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
        _targetMethod = implementationMethod.getMethod();
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
        Scheduler.schedule(_targetBean, _targetMethod, new ScheduleExpression()
            .second(_schedule.second()).minute(_schedule.minute()).hour(
                _schedule.hour()).dayOfWeek(_schedule.dayOfWeek()).dayOfMonth(
                _schedule.dayOfMonth()).month(_schedule.month()).year(
                _schedule.year()), _schedule.info(), _schedule.persistent());
      } else {
        for (Schedule schedule : _schedules.value()) {
          Scheduler.schedule(_targetBean, _targetMethod,
              new ScheduleExpression().second(schedule.second()).minute(
                  schedule.minute()).hour(schedule.hour()).dayOfWeek(
                  schedule.dayOfWeek()).dayOfMonth(schedule.dayOfMonth())
                  .month(schedule.month()).year(schedule.year()), schedule
                  .info(), schedule.persistent());
        }
      }
    }

    _next.generatePrologue(out, map);
  }
}