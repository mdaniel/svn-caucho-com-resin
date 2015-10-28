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
 * @author Scott Ferguson
 */

package com.caucho.v5.transaction;

import com.caucho.v5.env.health.*;
import com.caucho.v5.health.check.AbstractHealthCheck;

/**
 * Implementation of the transaction manager.
 */
public class TransactionHealthCheckImpl extends AbstractHealthCheck
{
  private TransactionManagerImpl _tm;
  
  private long _lastCommitCount;
  private long _lastRollbackCount;
  private long _lastCommitFailCount;
  
  private boolean _initialized = false;
  
  public TransactionHealthCheckImpl()
  {
    _tm = TransactionManagerImpl.getInstance();
  }
  
  /**
   * Returns the health status for this health check.
   */
  @Override
  public HealthCheckResult checkHealth()
  {
    if (_tm == null)
      return new HealthCheckResult(HealthStatus.OK);
    
    long lastCommitCount = _lastCommitCount;
    long lastCommitFailCount = _lastCommitFailCount;
    long lastRollbackCount = _lastRollbackCount;
    
    long commitCount = _tm.getCommitCount();
    long commitFailCount = _tm.getCommitResourceFailCount();
    long rollbackCount = _tm.getRollbackCount();
    
    _lastCommitCount = commitCount;
    _lastCommitFailCount = commitFailCount;
    _lastRollbackCount = rollbackCount;
    
    if (! _initialized) {
      _initialized = true;
      return new HealthCheckResult(HealthStatus.OK);
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append("fail=").append(commitFailCount - lastCommitFailCount);
    sb.append(", commit=").append(commitCount - lastCommitCount);
    sb.append(", rollback=").append(rollbackCount - lastRollbackCount);
    
    String message = sb.toString();
    
    if (lastCommitFailCount == commitFailCount)
      return new HealthCheckResult(HealthStatus.OK, message);
    else
      return new HealthCheckResult(HealthStatus.WARNING, message);
  }
}
