/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.management.server;

import java.beans.ConstructorProperties;

import javax.management.openmbean.CompositeData;

import com.caucho.v5.util.CurrentTime;

/**
 * Information about a tcp connection
 */
public class TcpConnectionInfo implements java.io.Serializable
{
  // the connection id
  private final int _id;
  // the thread id
  private final long _threadId;
  
  // the port id
  private final String _address;
  
  private final int _port;

  // the connection state
  private final String _state;
  // the request time
  private final long _requestStartTime;
  
  private String _remoteAddress;
  
  private String _url;

  /**
   * null-arg constructor for Hessian.
   */
  private TcpConnectionInfo()
  {
    _id = 0;
    _threadId = 0;
    _address = null;
    _port = 0;
    _state = null;
    _requestStartTime = 0;
    _remoteAddress = null;
    _url = null;
  }

  @ConstructorProperties({"id", "threadId", "address", "port",
                          "state", "requestStartTime"})
  public TcpConnectionInfo(int id,
                           long threadId,
                           String address,
                           int port,
                           String state,
                           long requestStartTime)
  {
    _id = id;
    _threadId = threadId;
    _address = address;
    _port = port;
    _state = state;
    _requestStartTime = requestStartTime;
  }
  
  public static TcpConnectionInfo from(CompositeData data)
  {
    return new TcpConnectionInfo((Integer) data.get("id"),
                                 (Long) data.get("threadId"),
                                 (String) data.get("address"),
                                 (int) data.get("port"),
                                 (String) data.get("state"),
                                 (Long) data.get("requestStartTime"));
  }

  public int getId()
  {
    return _id;
  }

  public long getThreadId()
  {
    return _threadId;
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  public int getPort()
  {
    return _port;
  }

  public String getPortName()
  {
    return (_address == null ? "*" : _address) + ":" + _port;
  }

  public String getState()
  {
    return _state;
  }
  
  public boolean hasRequest()
  {
    return _requestStartTime > 0;
  }

  public long getRequestStartTime()
  {
    return _requestStartTime;
  }
  
  public long getRequestActiveTime()
  {
    if (_requestStartTime > 0)
      return CurrentTime.getCurrentTime() - _requestStartTime;
    else
      return -1;
  }
  
  public void setRemoteAddress(String remoteAddress)
  {
    _remoteAddress = remoteAddress;
  }
  
  public String getRemoteAddress()
  {
    return _remoteAddress;
  }
  
  public void setUrl(String url)
  {
    _url = url;
  }
  
  public String getUrl()
  {
    return _url;
  }
  
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _id
            + "," + getPortName()
            + "]");
  }
}
