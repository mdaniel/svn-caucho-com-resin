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

package com.caucho.v5.websocket.server;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.v5.http.protocol.RequestServlet;
import com.caucho.v5.http.protocol.ResponseServlet;
import com.caucho.v5.util.ModulePrivate;

/**
 * websocket server container
 */
@ModulePrivate
public class WebSocketServlet extends GenericServlet
{
  private WebSocketServletDispatch _webSocket;

  public WebSocketServlet(WebSocketServletDispatch webSocket)
  {
    _webSocket = webSocket;
  }

  @Override
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    RequestServlet req = (RequestServlet) request;
    ResponseServlet res = (ResponseServlet) response;
    
    _webSocket.service(req, res);
  }
}
