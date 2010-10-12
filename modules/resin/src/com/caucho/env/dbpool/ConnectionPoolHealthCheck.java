/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.env.dbpool;

import com.caucho.env.health.AbstractHealthCheck;
import com.caucho.env.health.AbstractHealthService;
import com.caucho.env.health.HealthStatus;

/**
 * Health check for the connection pool.
 */
public class ConnectionPoolHealthCheck extends AbstractHealthCheck
{
  private HealthStatus _status = HealthStatus.OK;
  
  private String _lastMessage = null;
  
  private String _nextMessage = null;
  
  @Override
  public HealthStatus checkHealth()
  {
    HealthStatus status = _status;
    
    _status = HealthStatus.OK;
    _lastMessage = _nextMessage;
    
    if (_lastMessage == null)
      _lastMessage = "ok";
    
    _nextMessage = null;
    
    return status;
  }
  
  @Override
  public String getHealthStatusMessage()
  {
    return _lastMessage;
  }

  /**
   * @param message
   */
  static void currentWarning(String message)
  {
    ConnectionPoolHealthCheck healthCheck = getCurrent();
    
    if (healthCheck != null)
      healthCheck.warning(message);
  }  
  
  private static ConnectionPoolHealthCheck getCurrent()
  {
    ConnectionPoolHealthCheck healthCheck
      = AbstractHealthService.getCurrentHealthCheck(ConnectionPoolHealthCheck.class);
      
    if (healthCheck == null) {
      healthCheck = new ConnectionPoolHealthCheck();
        
      healthCheck = AbstractHealthService.addCurrentHealthCheck(healthCheck);
    }
      
    return healthCheck;
  }
  
  void warning(String message)
  {
    if (_status == HealthStatus.OK) {
      _status = HealthStatus.WARNING;
      _nextMessage = message;
    }
    
    if (_nextMessage == null)
      _nextMessage = message;
  }

}
