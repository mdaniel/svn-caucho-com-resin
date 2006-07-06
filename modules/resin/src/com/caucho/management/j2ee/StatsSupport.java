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

import com.caucho.util.Alarm;

import javax.management.j2ee.statistics.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.TreeSet;

public class StatsSupport
  implements Stats
{
  private final J2EEManagedObject _j2eeManagedObject;

  public StatsSupport(J2EEManagedObject j2eeManagedObject)
  {
    _j2eeManagedObject = j2eeManagedObject;
  }

  public String []getStatisticNames()
  {
    TreeSet<String> names = new TreeSet<String>();

    for (Method method : getClass().getMethods()) {
      if (Statistic.class.isAssignableFrom(method.getReturnType())) {
        String name = method.getName();

        if (name.startsWith("get"))
          names.add(name.substring(3));
      }
    }

    return names.toArray(new String[names.size()]);
  }

  public Statistic getStatistic(String name)
  {
    try {
      Method method = getClass().getMethod("get" + name);

      return (Statistic) method.invoke(this, (Object[]) null);
    }
    catch (NoSuchMethodException e) {
      return null;
    }
    catch (IllegalAccessException e) {
      return null;
    }
    catch (InvocationTargetException e) {
      return null;
    }
  }

  public Statistic []getStatistics()
  {
    ArrayList<Statistic> statistics = new ArrayList<Statistic>();

    for (Method method : getClass().getMethods()) {
      if (Statistic.class.isAssignableFrom(method.getReturnType())) {
        try {
          statistics.add((Statistic) method.invoke(this, (Object[]) null));
        }
        catch (IllegalAccessException e) {
          continue;
        }
        catch (InvocationTargetException e) {
          continue;
        }
      }
    }

    return statistics.toArray(new Statistic[statistics.size()]);
  }

  class StatisticSupport
    implements Statistic
  {
    public final String _name;

    public StatisticSupport(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public String getUnit()
    {
      return "UNKNOWN";
    }

    public String getDescription()
    {
      return "";
    }

    public long getStartTime()
    {
      return _j2eeManagedObject.getStartTime();
    }

    public long getLastSampleTime()
    {
      return Alarm.getCurrentTime();
    }
  }

  class TimeStatisticImpl
    extends StatisticSupport
    implements TimeStatistic
  {
    private long _count;
    private long _maxTime;
    private long _minTime;
    private long _totalTime;

    public TimeStatisticImpl(String name,
                             long count,
                             long maxTime,
                             long minTime,
                             long totalTime)
    {
      super(name);

      _count = count;
      _maxTime = maxTime;
      _minTime = minTime;
      _totalTime = totalTime;
    }

    public String getUnit()
    {
      return "MILLISECOND";
    }

    public long getCount()
    {
      return _count;
    }

    public long getMaxTime()
    {
      return _maxTime;
    }

    public long getMinTime()
    {
      return _minTime;
    }

    public long getTotalTime()
    {
      return _totalTime;
    }
  }

  class RangeStatisticImpl
    extends StatisticSupport
    implements RangeStatistic
  {
    private long _highWaterMark;
    private long _lowWaterMark;
    private long _current;

    public RangeStatisticImpl(String name,
                              long highWaterMark,
                              long lowWaterMark,
                              long current)
    {
      super(name);

      _highWaterMark = highWaterMark;
      _lowWaterMark = lowWaterMark;
      _current = current;
    }

    public long getHighWaterMark()
    {
      return _highWaterMark;
    }

    public long getLowWaterMark()
    {
      return _lowWaterMark;
    }

    public long getCurrent()
    {
      return _current;
    }
  }

  class BoundaryStatisticImpl
    extends StatisticSupport
    implements BoundaryStatistic
  {
    private long _upperBound;
    private long _lowerBound;

    public BoundaryStatisticImpl(String name, long upperBound, long lowerBound)
    {
      super(name);
      _upperBound = upperBound;
      _lowerBound = lowerBound;
    }

    public long getUpperBound()
    {
      return _upperBound;
    }

    public long getLowerBound()
    {
      return _lowerBound;
    }
  }

  class CountStatisticImpl
    extends StatisticSupport
    implements CountStatistic
  {
    private long _count;

    public CountStatisticImpl(String name, long count)
    {
      super(name);
      _count = count;
    }

    public long getCount()
    {
      return _count;
    }
  }

  class BoundedRangeStatisticImpl
    extends StatisticSupport
    implements BoundedRangeStatistic
  {
    private long _upperBound;
    private long _lowerBound;
    private long _highWaterMark;
    private long _lowWaterMark;
    private long _current;

    public BoundedRangeStatisticImpl(String name,
                                     long upperBound,
                                     long lowerBound,
                                     long highWaterMark,
                                     long lowWaterMark,
                                     long current)
    {
      super(name);

      _upperBound = upperBound;
      _lowerBound = lowerBound;
      _highWaterMark = highWaterMark;
      _lowWaterMark = lowWaterMark;
      _current = current;
    }

    public long getUpperBound()
    {
      return _upperBound;
    }

    public long getLowerBound()
    {
      return _lowerBound;
    }

    public long getHighWaterMark()
    {
      return _highWaterMark;
    }

    public long getLowWaterMark()
    {
      return _lowWaterMark;
    }

    public long getCurrent()
    {
      return _current;
    }
  }

  class UnimplementedTimeStatistic
    extends TimeStatisticImpl
  {
    public UnimplementedTimeStatistic(String name)
    {
      super(name, -1, -1, -1, -1);
    }
  }

  class UnimplementedRangeStatistic
    extends RangeStatisticImpl
  {
    public UnimplementedRangeStatistic(String name)
    {
      super(name, -1, -1, -1);
    }
  }

  class UnimplementedBoundaryStatistic
    extends BoundaryStatisticImpl
  {
    public UnimplementedBoundaryStatistic(String name)
    {
      super(name, -1, -1);
    }
  }

  class UnimplementedCountStatistic
    extends CountStatisticImpl
  {
    public UnimplementedCountStatistic(String name)
    {
      super(name, -1);
    }
  }

  class UnimplementedBoundedRangeStatistic
    extends BoundedRangeStatisticImpl
  {
    public UnimplementedBoundedRangeStatistic(String name)
    {
      super(name, -1, -1, -1, -1, -1);
    }
  }
}
