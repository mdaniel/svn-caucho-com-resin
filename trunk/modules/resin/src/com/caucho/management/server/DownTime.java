/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.management.server;

import java.io.Serializable;

import com.caucho.util.QDate;

// will always have an end time; this is the time Resin started
// may not have a start time --  
//   ie if it was the 1st startup or data was not available
public class DownTime implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  private long _startTime = -1;
  private long _endTime = -1;
  
  private boolean _estimated = true;
  private boolean _dataAbsent = false;

  public DownTime()
  {
  }

  public DownTime(long endTime)
  {
    _endTime = endTime;
  }

  public DownTime(long startTime, long endTime)
  {
    _startTime = startTime;
    _endTime = endTime;
  }
  
  public long getStartTime()
  {
    return _startTime;
  }

  public void setStartTime(long startTime)
  {
    _startTime = startTime;
  }

  public long getEndTime()
  {
    return _endTime;
  }

  public void setEndTime(long endTime)
  {
    _endTime = endTime;
  }
  
  public void setEstimated(boolean estimated)
  {
    _estimated = estimated;
  }
  
  public boolean isEstimated()
  {
    return _estimated;
  }
  
  public void setDataAbsent(boolean dataAbsent)
  {
    _dataAbsent = dataAbsent;
  }
  
  public boolean isDataAbsent()
  {
    return _dataAbsent;
  }

  public long getET()
  {
    return _startTime == -1 ? -1 : (_endTime - _startTime);
  }
  
  public boolean hasStartTime()
  {
    return _startTime != -1;
  }
  
  @Override
  public String toString()
  {
    return String.format("%s[%s to %s, %s ms]%s",
                         this.getClass().getSimpleName(),
                         hasStartTime() ? QDate.formatLocal(_startTime) : "?",
                         QDate.formatLocal(_endTime),
                         hasStartTime() ? getET() : "?",
                         isEstimated() ? " est" : "");
  }
}
