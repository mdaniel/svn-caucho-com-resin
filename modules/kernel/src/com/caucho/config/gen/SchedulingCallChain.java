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

import java.util.LinkedList;
import java.util.List;

import javax.ejb.Schedule;
import javax.ejb.ScheduleExpression;
import javax.ejb.Schedules;

import com.caucho.ejb.scheduler.Scheduler;
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

  public SchedulingCallChain(BusinessMethodGenerator businessMethod,
      EjbCallChain next)
  {
    super(next);
  }

  /**
   * Returns true if the business method needs scheduling.
   */
  @Override
  public boolean isEnhanced()
  {
    return false; // Will not need to generate code on the bean class itself.
  }

  /**
   * Introspects the method for locking attributes.
   */
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implementationMethod)
  {
    List<Schedule> _schedules = new LinkedList<Schedule>();

    Schedules schedulesAttribute = apiMethod.getAnnotation(Schedules.class);

    if ((schedulesAttribute == null) && (implementationMethod != null)) {
      schedulesAttribute = implementationMethod.getAnnotation(Schedules.class);
    }

    if (schedulesAttribute != null) {
      for (Schedule schedule : schedulesAttribute.value()) {
        _schedules.add(schedule);
      }
    }

    Schedule scheduleAttribute = apiMethod.getAnnotation(Schedule.class);

    if ((scheduleAttribute == null) && (implementationMethod != null)) {
      scheduleAttribute = implementationMethod.getAnnotation(Schedule.class);
    }

    if (scheduleAttribute != null) {
      _schedules.add(scheduleAttribute);
    }

    // TODO Should an alternate paradigm be used for this since this is not
    // really generating code?
    for (Schedule schedule : _schedules) {
      Scheduler.schedule(implementationMethod.getDeclaringClass()
          .getJavaClass(), implementationMethod.getMethod(),
          new ScheduleExpression().second(schedule.second()).minute(
              schedule.minute()).hour(schedule.hour()).dayOfWeek(
              schedule.dayOfWeek()).dayOfMonth(schedule.dayOfMonth()).month(
              schedule.month()).year(schedule.year()), schedule.info(),
          schedule.persistent());
    }
  }
}