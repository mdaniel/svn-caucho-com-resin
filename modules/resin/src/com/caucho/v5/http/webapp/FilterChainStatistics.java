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
 * @author Sam
 */

package com.caucho.v5.http.webapp;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.v5.http.protocol.RequestServlet;
import com.caucho.v5.io.ClientDisconnectException;
import com.caucho.v5.io.SocketBar;
import com.caucho.v5.network.port.ConnectionTcp;
import com.caucho.v5.util.CurrentTime;

public class FilterChainStatistics implements FilterChain
{
  private final FilterChain _next;
  private WebAppResinBase _webApp;

  public FilterChainStatistics(FilterChain next, WebAppResinBase webApp)
  {
    _next = next;
    _webApp = webApp;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    //XXX: async, etc. support?
    if (request instanceof RequestServlet) {
      RequestServlet httpRequest = (RequestServlet) request;

      ConnectionTcp connection = httpRequest.getConnection();

      if (connection instanceof ConnectionTcp) {
        ConnectionTcp tcpConnection = (ConnectionTcp) connection;

        long time = CurrentTime.getExactTime();

        long readBytes = -1;
        long writeBytes = -1;
        
        SocketBar socket = tcpConnection.socket();

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
