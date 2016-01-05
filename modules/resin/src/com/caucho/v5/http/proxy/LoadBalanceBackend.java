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
 * @author Paul Cowan
 */

package com.caucho.v5.http.proxy;

import com.caucho.v5.config.types.Period;

import io.baratine.config.Configurable;

@Configurable
public class LoadBalanceBackend
{
  private String _address;
  
  private long _connectTimeout = -1;
  private int _connectionMin = -1;
  private long _socketTimeout = -1;
  private long _idleTime = -1;
  private long _recoverTime = -1;
  private long _busyRecoverTime = -1;
  private long _warmupTime = -1;
  private int _weight = -1;
  
  public LoadBalanceBackend()
  {
    
  }
  
  public LoadBalanceBackend(String address)
  {
    _address = address;
  }

  public String getAddress()
  {
    return _address;
  }

  @Configurable
  public void setAddress(String address)
  {
    _address = address;
  }

  public long getConnectTimeout()
  {
    return _connectTimeout;
  }

  @Configurable
  public void setConnectTimeout(Period connectTimeout)
  {
    setConnectTimeoutMs(connectTimeout.getPeriod());
  }

  @Configurable
  public void setConnectTimeoutMs(long connectTimeout)
  {
    _connectTimeout = connectTimeout;
  }
  
  public boolean hasConnectTimeout()
  {
    return _connectTimeout >= 0;
  }

  public int getConnectionMin()
  {
    return _connectionMin;
  }

  @Configurable
  public void setConnectionMin(int connectionMin)
  {
    _connectionMin = connectionMin;
  }
 
  public boolean hasConnectionMin()
  {
    return _connectionMin >= 0;
  }

  public long getSocketTimeout()
  {
    return _socketTimeout;
  }

  @Configurable
  public void setSocketTimeout(Period socketTimeout)
  {
    setSocketTimeoutMs(socketTimeout.getPeriod());
  }

  @Configurable
  public void setSocketTimeoutMs(long socketTimeout)
  {
    _socketTimeout = socketTimeout;
  }

  public boolean hasSocketTimeout()
  {
    return _socketTimeout >= 0;
  }

  public long getIdleTime()
  {
    return _idleTime;
  }

  @Configurable
  public void setIdleTime(Period idleTime)
  {
    setIdleTimeMs(idleTime.getPeriod());
  }

  @Configurable
  public void setIdleTimeMs(long idleTime)
  {
    _idleTime = idleTime;
  }

  public boolean hasIdleTime()
  {
    return _idleTime >= 0;
  }

  public long getRecoverTime()
  {
    return _recoverTime;
  }

  @Configurable
  public void setRecoverTime(Period recoverTime)
  {
    setRecoverTimeMs(recoverTime.getPeriod());
  }

  @Configurable
  public void setRecoverTimeMs(long recoverTime)
  {
    _recoverTime = recoverTime;
  }

  public boolean hasRecoverTime()
  {
    return _recoverTime >= 0;
  }

  @Configurable
  public void setBusyRecoverTime(Period recoverTime)
  {
    setBusyRecoverTimeMs(recoverTime.getPeriod());
  }

  @Configurable
  public void setBusyRecoverTimeMs(long recoverTime)
  {
    _busyRecoverTime = recoverTime;
  }

  public boolean hasBusyRecoverTime()
  {
    return _busyRecoverTime >= 0;
  }

  public long getBusyRecoverTime()
  {
    return _busyRecoverTime;
  }
  
  public long getWarmupTime()
  {
    return _warmupTime;
  }

  @Configurable
  public void setWarmupTime(Period warmupTime)
  {
    setWarmupTimeMs(warmupTime.getPeriod());
  }

  @Configurable
  public void setWarmupTimeMs(long warmupTime)
  {
    _warmupTime = warmupTime;
  }

  public boolean hasWarmupTime()
  {
    return _warmupTime >= 0;
  }
  
  public int getWeight()
  {
    return _weight;
  }

  @Configurable
  public void setWeight(int weight)
  {
    _weight = weight;
  }
  
  public boolean hasWeight()
  {
    return _weight >= 0;
  }
  
  public String toString()
  {
    return this.getClass().getSimpleName() + "[" + _address + "]";
  }
}
