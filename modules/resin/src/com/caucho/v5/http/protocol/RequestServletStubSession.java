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

package com.caucho.v5.http.protocol;

import java.io.IOException;

import javax.servlet.http.HttpSession;

import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.network.port.ConnectionProtocol;
import com.caucho.v5.network.port.StateConnection;

/**
 * Used when there isn't any actual request object, e.g. for calling
 * run-at servlets.
 */
public class RequestServletStubSession extends RequestServletStub
  implements ConnectionProtocol
{
  private WebApp _webApp;
  private String _sessionId;
  private HttpSession _session;
  
  public RequestServletStubSession(WebApp webApp, String sessionId)
  {
    _webApp = webApp;
    _sessionId = sessionId;
  }
  
  public HttpSession getSession(boolean create)
  {
    if (_session == null)
      _session = _webApp.getSessionManager().getSession(_sessionId);
    
    return _session;
  }
  @Override
  public String url()
  {
    return null;
  }

  @Override
  public StateConnection service() throws IOException
  {
    return StateConnection.CLOSE;
  }

  /*
  @Override
  public boolean handleResume() throws IOException
  {
    return false;
  }
  */
}
  
