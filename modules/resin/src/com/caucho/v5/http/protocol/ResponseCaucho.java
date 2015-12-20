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
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.cache.FilterChainHttpCacheBase;

public interface ResponseCaucho extends HttpServletResponse
{
  /* caucho methods */
  OutResponseBase getResponseStream();
  void setResponseStream(OutResponseBase os);

  boolean isCauchoResponseStream();
  String getHeader(String key);
  
  void setFooter(String key, String value);
  void addFooter(String key, String value);
  
  void writeHeaders(int length)
    throws IOException;
  
  void close() throws IOException;

  // to support the JSP getRemaining
  //  public int getRemaining();

  boolean getForbidForward();
  void setForbidForward(boolean forbid);

  String getStatusMessage();

  boolean hasError();
  void setHasError(boolean error);

  void setSessionId(String id);
  
  void killCache();
  void setNoCache(boolean killCache);
  void setPrivateCache(boolean isPrivate);
  
  void setCacheInvocation(FilterChainHttpCacheBase cacheFilterChain);
  boolean isCaching();

  boolean isNoCacheUnlessVary();
  
  void completeCache();

  ResponseHttpBase getAbstractHttpResponse();
  /**
   * Return wrapped response
   */
  ServletResponse getResponse();

  void setForwardEnclosed(boolean isForwardEnclosed);

  boolean isForwardEnclosed();
}
