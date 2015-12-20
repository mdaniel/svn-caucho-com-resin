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

import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.network.listen.ConnectionSocket;
import com.caucho.v5.vfs.ReadStream;

public interface RequestCaucho extends HttpServletRequest
{
  String getPageURI();
  String getPageContextPath();
  String getPageServletPath();
  String getPagePathInfo();
  String getPageQueryString();

  WebApp getWebApp();
  
  ReadStream getStream() throws IOException;
  int getRequestDepth(int depth);
  void setHeader(String key, String value);
  boolean getVaryCookies();
  void setVaryCookie(String cookie);
  boolean getHasCookie();

  void setSyntheticCacheHeader(boolean isTop);
  boolean isSyntheticCacheHeader();
  
  boolean isTop();

  boolean hasRequest();
  
  String getSessionId();
  void setSessionId(String sessionId);
  boolean isSessionIdFromCookie();
  HttpSession getMemorySession();
  Cookie getCookie(String name);
  void setHasCookie();
  void killKeepalive(String reason);
  boolean isSuspend(); // XXX: isComplete()?
  boolean isComet();
  boolean isDuplex();
  boolean isConnectionClosed();

  boolean isLoginRequested();
  void requestLogin();
  boolean login(boolean isFail);

  // public HashMap<String,String> setRoleMap(HashMap<String,String> roleMap);
  
  boolean isMultipartEnabled();

  ServletResponse getServletResponse();
  RequestHttpBase getAbstractHttpRequest();
  ConnectionSocket getSocketLink();
  void completeCache();
}
