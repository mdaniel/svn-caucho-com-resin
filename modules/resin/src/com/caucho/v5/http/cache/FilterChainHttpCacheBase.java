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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.cache;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.FilterChain;

import com.caucho.v5.http.protocol.RequestCache;
import com.caucho.v5.http.protocol.ResponseCache;

/**
 * Represents the final servlet in a filter chain.
 */
abstract public class FilterChainHttpCacheBase
  implements FilterChain
  {
  /**
   * fillFromCache is called when the client needs the entire result, and
   * the result is already in the cache.
   *
   * @param req the servlet request trying to get data from the cache
   * @param response the servlet response which will receive data
   * @param entry the cache entry to use
   */
  abstract public boolean fillFromCache(RequestCache req,
                                        ResponseCache response,
                                        EntryHttpCacheBase abstractEntry)
    throws IOException;

  /**
   * Starts the caching after the headers have been sent.
   *
   * @param req the servlet request
   * @param req the servlet response
   * @param keys the saved header keys
   * @param values the saved header values
   * @param contentType the response content type
   * @param charEncoding the response character encoding
   *
   * @return the output stream to store the cache value or null if
   *         uncacheable.
   */
  abstract public EntryHttpCacheBase startCaching(RequestCache req,
                                                  ResponseCache res,
                                                  ArrayList<String> keys,
                                                  ArrayList<String> values,
                                                  String contentType,
                                                  String charEncoding,
                                                  long contentLength);

  /**
   * Update the headers when the caching has finished.
   *
   * @param okay if true, the cache if valid
   */
  abstract public void finishCaching(ResponseCache res);

  /**
   * Cleanup the cache entry on a failed cache attempt.
   */
  abstract public void killCaching(ResponseCache res);
}
