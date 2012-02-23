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

package com.caucho.server.httpcache;

import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.CauchoRequestWrapper;

public class ProxyCacheRequest extends CauchoRequestWrapper
{
  private ProxyCacheEntry _entry;
  
  ProxyCacheRequest(CauchoRequest req,
                    ProxyCacheEntry entry)
  {
    super(req);
  
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
  
  ProxyCacheEntry getProxyCacheEntry()
  {
    return _entry;
  }
  
  @Override
  public String getHeader(String header)
  {
    if ("If-None-Match".equalsIgnoreCase(header)) {
      if (_entry._etag != null)
        return _entry._etag;
    }
    else if ("If-Modified-Since".equalsIgnoreCase(header)) {
      if (_entry._lastModified != null)
        return _entry._lastModified;
    }
    
    return super.getHeader(header);
    
  }
}