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

import javax.management.j2ee.statistics.ServletStats;
import javax.management.j2ee.statistics.TimeStatistic;
import javax.servlet.ServletConfig;

public class Servlet
  extends J2EEManagedObject
  implements StatisticsProvider<ServletStats>
{
  private final ServletConfig _servletConfig;

  public Servlet(ServletConfig servletConfig)
  {
    _servletConfig = servletConfig;
  }

  protected String getName()
  {
    return _servletConfig.getServletName();
  }

  public ServletStats getStats()
  {
    return new ServletStatsImpl(this);
  }

  // no attributes

  class ServletStatsImpl
    extends StatsSupport
    implements ServletStats
  {
    public ServletStatsImpl(J2EEManagedObject j2eeManagedObject)
    {
      super(j2eeManagedObject);
    }

    public TimeStatistic getServiceTime()
    {
      return new UnimplementedTimeStatistic("ServiceTime");
    }
  }
}
