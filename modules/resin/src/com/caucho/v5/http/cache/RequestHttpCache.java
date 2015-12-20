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

package com.caucho.v5.http.cache;

import com.caucho.v5.http.protocol.RequestCaucho;
import com.caucho.v5.http.protocol.RequestCauchoWrapper;
import com.caucho.v5.http.protocol.RequestFacade;

public class RequestHttpCache extends RequestCauchoWrapper
{
  private EntryHttpCache _entry;
  
  RequestHttpCache(RequestFacade req,
                   EntryHttpCache entry)
  {
    super((RequestCaucho) req);
  
    _entry = entry;
    
    /*
    String oldIfMatch = req.getHeader("If-None-Match");
    String oldIfModifiedSince = req.getHeader("If-Modified-Since");

    if (oldIfMatch == null && oldIfModifiedSince == null) {
      if (entry._etag != null)
        req.setHeader("If-None-Match", entry._etag);

      if (entry._lastModified != null)
        req.setHeader("If-Modified-Since", entry._lastModified);
    }

      // Point the response to the cache entry, so if the servlet returns
      // not modified, we can use the cache
      res.setMatchCacheEntry(entry);
     */
  }
  
  EntryHttpCache getProxyCacheEntry()
  {
    return _entry;
  }
  
  @Override
  public String getHeader(String header)
  {
    if ("If-None-Match".equalsIgnoreCase(header)) {
      if (_entry.getEtag() != null) {
        return _entry.getEtag();
      }
    }
    else if ("If-Modified-Since".equalsIgnoreCase(header)) {
      if (_entry.getLastModified() != null) {
        return _entry.getLastModified();
      }
    }
    
    return super.getHeader(header);
    
  }
}