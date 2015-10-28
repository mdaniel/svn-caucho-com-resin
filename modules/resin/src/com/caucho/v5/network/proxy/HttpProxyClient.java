/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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
 * @author Paul Cowan
 */

package com.caucho.v5.network.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.protocol.RequestCaucho;
import com.caucho.v5.http.proxy.LoadBalanceManager;
import com.caucho.v5.network.balance.ClientSocket;
import com.caucho.v5.network.proxy.ProxyResult.ProxyStatus;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.TempBuffer;
import com.caucho.v5.vfs.WriteStream;

public class HttpProxyClient 
{
  private static final Logger log = 
    Logger.getLogger(HttpProxyClient.class.getName());
  private static final L10N L = new L10N(HttpProxyClient.class);
  
  private LoadBalanceManager _loadBalancer;
  
  public HttpProxyClient(LoadBalanceManager loadBalancer)
  {
    _loadBalancer = loadBalancer;
  }
  
  protected LoadBalanceManager getLoadBalancer()
  {
    return _loadBalancer;
  }
  
  public void handleRequest(HttpServletRequest req, 
                            HttpServletResponse res)
  {
    String sessionId = getSessionId(req);
    
    ClientSocket client = getLoadBalancer().openSticky(sessionId, req, null);
    if (client == null) {
      proxyFailure(req, res, null, "no backend servers available", true);
      return;
    }
    
    String uri = constructURI(req);
    long requestStartTime = System.currentTimeMillis();
      
    ProxyResult result = proxy(req, res, uri, sessionId, client);
    if (! result.isSuccess()) {
      proxyFailure(req, res, null, result.getFailureMessage(), true);
    }
  
    if (result.isKeepAlive()) {
      client.free(requestStartTime);
    } else {
      client.close();
    }
  }
  
  protected void proxyFailure(HttpServletRequest req,
                              HttpServletResponse res,
                              ClientSocket client,
                              String reason,
                              boolean send503)
  {
    log.warning(L.l("{0}: proxy {1} failed for {2}: {3}",
                    this, 
                    client != null ? client : "",
                    req.getRequestURI(),
                    reason));

    if (res.isCommitted())
      return;
    
    if (send503) {
      try {
        res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      } catch (IOException e) {
        log.log(Level.FINE, L.l("{0}: failed to send error {1}: {2}", 
                                this,
                                req.getRequestURI(),
                                e.getMessage()), e);
      }
    }
  }
  
  protected ProxyResult proxy(HttpServletRequest req,
                              HttpServletResponse res,
                              String uri,
                              String sessionId,
                              ClientSocket client)
  {
    ReadStream clientIn = client.getInputStream();
    WriteStream clientOut = client.getOutputStream();
    
    try {
      clientOut.print(req.getMethod());
      clientOut.print(' ');
      clientOut.print(uri);
      clientOut.print(" HTTP/1.1\r\n");

      String host = req.getHeader("Host");
      if (host == null)
        host = req.getServerName() + ":" + req.getServerPort();

      clientOut.print("Host: ");
      clientOut.print(host);
      clientOut.print("\r\n");

      clientOut.print("X-Forwarded-For: ");
      clientOut.print(req.getRemoteAddr());
      clientOut.print("\r\n");

      Enumeration<String> e = req.getHeaderNames();
      while (e.hasMoreElements()) {
        String name = e.nextElement();

        if (name.equalsIgnoreCase("Connection"))
          continue;
        else if (name.equalsIgnoreCase("Host"))
          continue;

        Enumeration<String> e1 = req.getHeaders(name);
        while (e1.hasMoreElements()) {
          String value = e1.nextElement();

          clientOut.print(name);
          clientOut.print(": ");
          clientOut.print(value);
          clientOut.print("\r\n");
        }
      }

      final int contentLength = req.getContentLength();

      InputStream is = req.getInputStream();

      TempBuffer tempBuffer = TempBuffer.allocate();
      byte []buffer = tempBuffer.getBuffer();

      boolean isFirst = true;

      if (contentLength >= 0) {
        isFirst = false;
        clientOut.print("\r\n");
      }

      int len;
      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        if (isFirst) {
          clientOut.print("Transfer-Encoding: chunked\r\n");
        }

        if (contentLength < 0) {
          clientOut.print("\r\n");
          clientOut.print(Integer.toHexString(len));
          clientOut.print("\r\n");
        }

        clientOut.write(buffer, 0, len);

        isFirst = false;
      }

      if (isFirst) {
        clientOut.print("Content-Length: 0\r\n\r\n");
      }
      else if (contentLength < 0) {
        clientOut.print("\r\n0\r\n");
      }

      TempBuffer.free(tempBuffer);

      clientOut.flush();
      
      return parseResults(clientIn, req, res);
    } catch (Exception e) {
      return new ProxyResult(ProxyStatus.FAIL, false, e.toString());
    }
  }
  
  protected String constructURI(HttpServletRequest req)
  {
    String uri;
    
    if (req.isRequestedSessionIdFromURL()) {
      uri =  (req.getRequestURI() + ";jsessionid=" +
              req.getRequestedSessionId());
    } else {
      uri = req.getRequestURI();
    }

    String queryString = null;

    if (req instanceof RequestCaucho) {
      queryString = ((RequestCaucho)req).getPageQueryString();
    } else {
      queryString
        = (String) req.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING);

      if (queryString == null)
        queryString = req.getQueryString();
    }

    if (queryString != null)
      uri += '?' + queryString;
    
    return uri;
  }
  
  protected String getSessionId(HttpServletRequest req)
  {
    return req.getRequestedSessionId();
  }

  private ProxyResult parseResults(ReadStream is,
                                   HttpServletRequest req,
                                   HttpServletResponse res)
    throws IOException
  {
    String line = parseStatus(is);
    if (line.length() == 0)
      return new ProxyResult(ProxyStatus.FAIL, false, "read failure");

    boolean isKeepalive = true;

    if (! line.startsWith("HTTP/1.1"))
      isKeepalive = false;

    int statusCode = parseStatusCode(line);

    String location = null;

    boolean isChunked = false;
    int contentLength = -1;

    while (true) {
      line = is.readLine();
      if (line == null)
        break;

      int p = line.indexOf(':');
      if (p < 0)
        break;

      String name = line.substring(0, p);
      String value = line.substring(p + 1).trim();

      if (name.equalsIgnoreCase("transfer-encoding")) {
        isChunked = true;
      } else if (name.equalsIgnoreCase("content-length")) {
        contentLength = Integer.parseInt(value);
      } else if (name.equalsIgnoreCase("location")) {
        location = value;
      } else if (name.equalsIgnoreCase("connection")) {
        if ("close".equalsIgnoreCase(value))
          isKeepalive = false;
      } else {
        // XXX: split header
        res.addHeader(name, value);
      }
    }

    /* server/1965
    if (location == null) {
    }
    else if (location.startsWith(hostURL)) {
      location = location.substring(hostURL.length());

      String prefix;
      if (req.isSecure()) {
        if (req.getServerPort() != 443)
          prefix = ("https://" + req.getServerName() +
                    ":" + req.getServerPort());
        else
          prefix = ("https://" + req.getServerName());
      }
      else {
        if (req.getServerPort() != 80)
          prefix = ("http://" + req.getServerName() +
                    ":" + req.getServerPort());
        else
          prefix = ("http://" + req.getServerName());
      }

      if (! location.startsWith("/"))
        location = prefix + "/" + location;
      else
        location = prefix + location;
    }
    */

    if (location != null)
      res.setHeader("Location", location);
    
    ProxyStatus resultStatus = ProxyStatus.OK;

    if (statusCode == 302 && location != null)
      res.sendRedirect(location);
    else if (statusCode != 200)
      res.setStatus(statusCode);
    
    if (statusCode == 503) {
      resultStatus = ProxyStatus.BUSY;
      isKeepalive = false;
    }

    OutputStream os = res.getOutputStream();

    if (isChunked)
      writeChunkedData(os, is);
    else if (contentLength > 0) {
      res.setContentLength(contentLength);
      writeContentLength(os, is, contentLength);
    }
    
    return new ProxyResult(resultStatus, isKeepalive);
  }

  private String parseStatus(ReadStream is)
    throws IOException
  {
    int ch;

    for (ch = is.read(); Character.isWhitespace(ch); ch = is.read()) {
    }

    StringBuilder sb = new StringBuilder();
    for (; ch >= 0 && ch != '\n'; ch = is.read()) {
      if (ch != '\r')
        sb.append((char) ch);
    }

    return sb.toString();
  }

  private int parseStatusCode(String line)
  {
    int len = line.length();

    int i = 0;
    int ch;

    for (; i < len && (ch = line.charAt(i)) != ' '; i++) {
    }

    for (; i < len && (ch = line.charAt(i)) == ' '; i++) {
    }

    int statusCode = 0;

    for (; i < len && '0' <= (ch = line.charAt(i)) && ch <= '9'; i++) {
      statusCode = 10 * statusCode + ch - '0';
    }

    if (statusCode == 0)
      return 400;
    else
      return statusCode;
  }

  private void writeChunkedData(OutputStream os, ReadStream is)
    throws IOException
  {
    int ch;

    while (true) {
      for (ch = is.read(); Character.isWhitespace(ch); ch = is.read()) {
      }

      int len = 0;
      for (; ch >= 0; ch = is.read()) {
        if ('0' <= ch && ch <= '9')
          len = 16 * len + ch - '0';
        else if ('a' <= ch && ch <= 'f')
          len = 16 * len + ch - 'a' + 10;
        else if ('A' <= ch && ch <= 'F')
          len = 16 * len + ch - 'A' + 10;
        else
          break;
      }

      if (ch == '\r')
        ch = is.read();

      if (ch != '\n')
        throw new IllegalStateException(L.l("unexpected chunking at '{0}'",
                                            (char) ch));

      if (len == 0)
        break;

      is.writeToStream(os, len);
    }

    ch = is.read();
    if (ch == '\r')
      ch = is.read();

    // XXX: footer
  }

  private void writeContentLength(OutputStream os, ReadStream is, int length)
    throws IOException
  {
    is.writeToStream(os, length);
  }
  
  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
}
