/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Sam
 */


package com.caucho.management.j2ee;

import com.caucho.ejb.cfg.EjbSessionBean;

import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.RangeStatistic;
import javax.management.j2ee.statistics.StatefulSessionBeanStats;

/**
 * Management interface for a stateful session bean.
 */
public class StatefulSessionBean
  extends SessionBean
  implements StatisticsProvider<StatefulSessionBeanStats>
{
  public StatefulSessionBean(EjbSessionBean ejbBean)
  {
    super(ejbBean);
  }

  public StatefulSessionBeanStats getStats()
  {
    return new StatefulSessionBeanStatsImpl(this);
  }

  // no attributes

  class StatefulSessionBeanStatsImpl
    extends StatsSupport
    implements StatefulSessionBeanStats
  {
    public StatefulSessionBeanStatsImpl(J2EEManagedObject j2eeManagedObject)
    {
      super(j2eeManagedObject);
    }

    public RangeStatistic getPassiveCount()
    {
      return new UnimplementedRangeStatistic("PassiveCount");
    }

    public RangeStatistic getMethodReadyCount()
    {
      return new UnimplementedRangeStatistic("MethodReadyCount");
    }

    public RangeStatistic getPooledCount()
    {
      return new UnimplementedRangeStatistic("PooledCount");
    }

    public CountStatistic getCreateCount()
    {
      return new UnimplementedCountStatistic("CreateCount");
    }

    public CountStatistic getRemoveCount()
    {
      return new UnimplementedCountStatistic("RemoveCount");
    }
  }
}
