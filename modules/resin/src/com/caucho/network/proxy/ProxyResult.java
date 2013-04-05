/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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
 * @author Paul Cowan
 */


package com.caucho.network.proxy;

public class ProxyResult
{
  public static enum ProxyStatus
  {
    OK,
    BUSY,
    FAIL
  }
  
  private ProxyStatus _status = ProxyStatus.FAIL;
  private boolean _keepAlive = false;
  private String _failureMessage;
  
  public ProxyResult(ProxyStatus status, boolean keepAlive)
  {
    _status = status;
    _keepAlive = keepAlive;
  }
  
  public ProxyResult(ProxyStatus status, 
                     boolean keepAlive, 
                     String failureMessage)
  {
    _status = status;
    _keepAlive = keepAlive;
    _failureMessage = failureMessage;
  }

  public boolean isSuccess()
  {
    return _status == ProxyStatus.OK;
  }

  public ProxyStatus getStatus()
  {
    return _status;
  }

  public void setStatus(ProxyStatus status)
  {
    _status = status;
  }

  public boolean isKeepAlive()
  {
    return _keepAlive;
  }

  public void setKeepAlive(boolean keepAlive)
  {
    _keepAlive = keepAlive;
  }

  public String getFailureMessage()
  {
    if (_failureMessage == null && _status == ProxyStatus.BUSY)
      return "busy";
    return _failureMessage;
  }

  public void setFailureMessage(String failureMessage)
  {
    _failureMessage = failureMessage;
  }

  public String toString()
  {
    return String.format("%s[status=%s,keepAlive=%s%s]", 
                         this.getClass().getSimpleName(),
                         _status,
                         _keepAlive,
                         _failureMessage != null ? "," + _failureMessage : "");
  }
}
