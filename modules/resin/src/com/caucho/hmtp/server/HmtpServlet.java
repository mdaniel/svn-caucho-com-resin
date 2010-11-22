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

package com.caucho.hmtp.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import com.caucho.bam.Actor;
import com.caucho.hmtp.HmtpWebSocketListener;
import com.caucho.websocket.WebSocketListener;
import com.caucho.websocket.WebSocketServletRequest;

/**
 * HmtpWriteStream writes HMTP packets to an OutputStream.
 */
@SuppressWarnings("serial")
public class HmtpServlet extends HttpServlet
{
  private static final Logger log
    = Logger.getLogger(HmtpServlet.class.getName());

  private String _jid;

  public HmtpServlet()
  {
    _jid = getClass().getSimpleName();
  }
  
  public String getJid()
  {
    return _jid;
  }
  
  @Override
  public void service(ServletRequest request,
                      ServletResponse response)
    throws IOException, ServletException
  {
    WebSocketServletRequest req = (WebSocketServletRequest) request;
    
    WebSocketListener listener = createWebSocketListener();
    
    req.startWebSocket(listener);
  }
  
  protected WebSocketListener createWebSocketListener()
  {
    Actor actor = createActor();
    
    return new HmtpWebSocketListener(actor);
  }
  
  protected Actor createActor()
  {
    return null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
