/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 */

package com.caucho.v5.admin.action;

import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.management.server.StatServiceValue;
import com.caucho.v5.server.admin.StatServiceValuesQueryReply;
import com.caucho.v5.util.L10N;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class GetStatsAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(GetStatsAction.class.getName());

  private static final L10N L = new L10N(GetStatsAction.class);

  public StatServiceValuesQueryReply execute(String []meters,
                                              Date from,
                                              Date to)
  {
    StatSystem stats = StatSystem.getCurrent();

    int id = HttpContainer.getCurrent().getServerIndex();

    String serverIndex;

    if (id <= 9)
      serverIndex = "0" + Integer.toString(id);
    else
      serverIndex = Integer.toString(id);
    
    String []meterNames = new String[meters.length];
    for (int i = 0; i < meters.length; i++) {
      String meter = meters[i];
      meterNames[i] = serverIndex + '|' + meter;
    }
    
    String []allNames = stats.getStatisticsNames();
    
    List<String> matchNames = new ArrayList<String>();

    for (String statName : allNames) {
      for (String meterName : meterNames) {
        if (statName.startsWith(meterName) && ! matchNames.contains(statName))
          matchNames.add(statName);
      }
    }

    if (matchNames.size() == 0)
      throw new IllegalArgumentException(L.l("unknown names {0}", Arrays.asList(
        meters)));

    List<StatServiceValue []> statValues = new ArrayList<StatServiceValue[]>();

    for (String match : matchNames) {
      statValues.add(stats.getStatisticsData(match,
                                             from.getTime(),
                                             to.getTime(),
                                             1));
    }

    String []names = new String[matchNames.size()];
    matchNames.toArray(names);

    StatServiceValue [][]data = new StatServiceValue[statValues.size()][];
    statValues.toArray(data);

    return new StatServiceValuesQueryReply(names, data);
  }
}
