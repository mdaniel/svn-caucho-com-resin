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
 * @author Sam
 */


package com.caucho.server.webapp;

import com.caucho.network.listen.TcpSocketLink;
import com.caucho.network.listen.SocketLink;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.QSocket;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class StatisticsFilterChain implements FilterChain
{
  private final FilterChain _next;
  private WebApp _webApp;

  public StatisticsFilterChain(FilterChain next, WebApp webApp)
  {
    _next = next;
    _webApp = webApp;
  }

  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    //XXX: async, etc. support?
    if (request instanceof HttpServletRequestImpl) {
      HttpServletRequestImpl httpRequest = (HttpServletRequestImpl) request;

      SocketLink connection = httpRequest.getConnection();

      if (connection instanceof TcpSocketLink) {
        TcpSocketLink tcpConnection = (TcpSocketLink) connection;

        long time = CurrentTime.getExactTime();

        long readBytes = -1;
        long writeBytes = -1;
        
        QSocket socket = tcpConnection.getSocket();

        readBytes = socket.getTotalReadBytes();
        writeBytes = socket.getTotalWriteBytes();

        ClientDisconnectException clientDisconnectException = null;

        try {
          _next.doFilter(request, response);

          // server/272a/*
          if (httpRequest.getResponse() != null)
            httpRequest.getResponse().flushBuffer();
        } catch (ClientDisconnectException ex) {
          clientDisconnectException = ex;
        } finally {
          time = CurrentTime.getExactTime() - time;

          readBytes = socket.getTotalReadBytes() - readBytes;
          writeBytes = socket.getTotalWriteBytes() - writeBytes;

          _webApp.updateStatistics(time, (int) readBytes, (int) writeBytes, clientDisconnectException != null);
        }

        if (clientDisconnectException != null)
          throw clientDisconnectException;

        return;
      }
    }

    _next.doFilter(request, response);
  }
}
