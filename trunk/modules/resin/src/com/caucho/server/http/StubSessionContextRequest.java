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

package com.caucho.server.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.network.listen.SocketLinkDuplexListener;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.NullEnumeration;
import com.caucho.vfs.ReadStream;

/**
 * Used when there isn't any actual request object, e.g. for calling
 * run-at servlets.
 */
public class StubSessionContextRequest extends StubServletRequest
  implements ProtocolConnection
{
  private WebApp _webApp;
  private String _sessionId;
  private HttpSession _session;
  
  public StubSessionContextRequest(WebApp webApp, String sessionId)
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
  public String getProtocolRequestURL()
  {
    return null;
  }

  @Override
  public boolean handleRequest() throws IOException
  {
    return false;
  }

  @Override
  public boolean handleResume() throws IOException
  {
    return false;
  }

  @Override
  public void init()
  {
  }

  @Override
  public boolean isWaitForRead()
  {
    return false;
  }
  
  @Override
  public void onCloseConnection()
  {
  }

  @Override
  public void onStartConnection()
  {
  }

  /* (non-Javadoc)
   * @see com.caucho.network.listen.ProtocolConnection#onAttachThread()
   */
  @Override
  public void onAttachThread()
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.network.listen.ProtocolConnection#onDetachThread()
   */
  @Override
  public void onDetachThread()
  {
    // TODO Auto-generated method stub
    
  }
}
  
