/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.security;

import com.caucho.config.ConfigException;
import com.caucho.util.InetNetwork;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.rewrite.RequestPredicate;
import com.caucho.server.security.*;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Allow or deny requests based on the ip address of the client.
 *
 * <pre>
 * &lt;sec:Allow url-pattern="/admin/*"&gt;
 *   &lt;sec:IfNetwork name="192.168.17.0/24"/&gt;
 * &lt;/sec:Allow>
 * </pre>
 * 
 * <pre> 
 * &lt;sec:Deny>
 *   &lt;sec:IfNetwork>
 *     &lt;value>205.11.12.3&lt;/value>
 *     &lt;value>123.4.45.6&lt;/value>
 *     &lt;value>233.15.25.35&lt;/value>
 *     &lt;value>233.14.87.12&lt;/value>
 *   &lt;/sec:IfNetwork&gt;
 * &lt;/sec:Deny&gt;
 * </pre>
 */
public class IfNetwork implements RequestPredicate {
  private static final Logger log
    = Logger.getLogger(IfNetwork.class.getName());
  static L10N L = new L10N(IfNetwork.class);

  private ArrayList<InetNetwork> _networkList = new ArrayList<InetNetwork>();

  private int _cacheSize = 256;

  private LruCache<String,Boolean> _cache;

  /**
   * Size of the cache used to hold whether or not to allow a certain IP
   * address, default is 256.  The first time a request is received from an ip,
   * the allow and deny rules are checked to determine if the ip is allowed.
   * The result of this check is cached in a an LRU cache.  Subsequent requests
   * can do a cache lookup based on the ip instead of checking the rules.  This
   * is especially important if there are a large number of allow and/or deny
   * rules, and to protect against denial of service attacks.  
   */ 
  public void setCacheSize(int cacheSize)
  {
    _cacheSize = cacheSize;
  }

  /** 
   * Size of the cache used to hold whether or not to allow a certain IP
   * address.
   */ 
  public int getCacheSize()
  {
    return _cacheSize;
  }

  /**
   * Add an ip network to allow.  If allow is never used, (only deny is used),
   * then all are allowed except those in deny.
   */
  public void addValue(String network)
  {
    if (_networkList == null)
      _networkList = new ArrayList<InetNetwork>();

    _networkList.add(InetNetwork.create(network));
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    _cache = new LruCache<String,Boolean>(_cacheSize);
  }

  /**
   * Returns true if the user is authorized for the resource.
   */
  public boolean isMatch(HttpServletRequest request)
  {
    String remoteAddr = request.getRemoteAddr();
    long addr = 0;

    if (remoteAddr == null)
      return false;
    
    if (_cache != null) {
      Boolean cacheValue = _cache.get(remoteAddr);
      
      if (cacheValue != null)
	return cacheValue;
    }

    int len = remoteAddr.length();
    int ch;
    int i = 0;

    while (i < len && (ch = remoteAddr.charAt(i)) >= '0' && ch <= '9') {
      int digit = 0;
	
      for (; i < len && (ch = remoteAddr.charAt(i)) >= '0' && ch <= '9'; i++)
	digit = 10 * digit + ch - '0';

      addr = 256 * addr + digit;

      if (ch == '.')
	i++;
    }

    boolean isMatch = false;
    for (i = 0; i < _networkList.size(); i++) {
      InetNetwork net = _networkList.get(i);

      if (net.isMatch(addr)) {
	isMatch = true;
	break;
      }
    }

    // update cache

    if (_cache != null)
      _cache.put(remoteAddr, isMatch);

    return isMatch;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + _networkList;
  }
}
