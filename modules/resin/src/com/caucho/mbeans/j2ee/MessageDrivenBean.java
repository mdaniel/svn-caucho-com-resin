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


package com.caucho.mbeans.j2ee;

import com.caucho.ejb.cfg.EjbMessageBean;

import javax.management.j2ee.statistics.MessageDrivenBeanStats;
import javax.management.j2ee.statistics.CountStatistic;

/**
 * Management interface for a message driven bean.
 */
public class MessageDrivenBean
  extends EJB
  implements StatisticsProvider<MessageDrivenBeanStats>
{
  public MessageDrivenBean(EjbMessageBean ejbMessageBean)
  {
    super(ejbMessageBean);
  }

  public MessageDrivenBeanStats getStats()
  {
    return new MessageDrivenBeanStatsImpl(this);
  }

  // no attributes

  class MessageDrivenBeanStatsImpl
    extends StatsSupport
    implements MessageDrivenBeanStats
  {
    public MessageDrivenBeanStatsImpl(J2EEManagedObject j2eeManagedObject)
    {
      super(j2eeManagedObject);
    }

    public CountStatistic getMessageCount()
    {
      return new UnimplementedCountStatistic("MessageCount");
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
