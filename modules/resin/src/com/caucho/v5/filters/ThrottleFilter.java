/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.filters;

import com.caucho.v5.config.types.Period;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.util.IntMap;
import com.caucho.v5.util.L10N;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Throttles the filter to only a limited number of requests.
 */
public class ThrottleFilter implements Filter {
  private static final L10N L = new L10N(ThrottleFilter.class);
  private static final Logger log
    = Logger.getLogger(ThrottleFilter.class.getName());
  
  private static final EnvironmentLocal<ThrottleFilter> _localRef
    = new  EnvironmentLocal<ThrottleFilter>();

  private IntMap _throttleCache = new IntMap();
  
  private ConcurrentHashMap<String,String> _poisonedIpMap
    = new ConcurrentHashMap<String,String>();
  
  private boolean _isPoisonedIp;

  private int _maxConcurrentRequests = 2;
  
  private int _maxTotalRequests = -1;
  private long _timeout = 120 * 1000L;
  private long _requestDelay = 0;
  private Semaphore _requestSemaphore;
  
  public ThrottleFilter()
  {
    _localRef.set(this);
  }
  
  public static ThrottleFilter getCurrent()
  {
    return _localRef.get();
  }

  /**
   * Sets the maximum number of concurrent requests for a single IP.
   */
  public void setMaxConcurrentRequests(int max)
  {
    _maxConcurrentRequests = max;
  }
  
  public void setMaxTotalRequests(int max)
  {
    _maxTotalRequests = max;
  }
  
  public void setTimeout(Period period)
  {
    _timeout = period.getPeriod();
  }
  
  public void setDelay(Period period)
  {
    _requestDelay = period.getPeriod();
  }
  
  public void addPoisonedIp(String address)
  {
    _isPoisonedIp = true;
    _poisonedIpMap.put(address, address);
  }

  @Override
  public void init(FilterConfig config)
    throws ServletException
  {
    if (_maxTotalRequests > 0)
      _requestSemaphore = new Semaphore(_maxTotalRequests);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    String ip = request.getRemoteAddr();
    
    if (_isPoisonedIp && _poisonedIpMap.get(ip) != null) {
      log.info(L.l("'{0}' is a poisoned address -- throttling.",
                   ip));
      
      if (response instanceof HttpServletResponse)
        ((HttpServletResponse) response).sendError(503);
      
      return;
    }
    
    boolean isOverflow;

    synchronized (this) {
      int count = _throttleCache.get(ip);

      if (count <= 0)
        count = 0;

      if (count < _maxConcurrentRequests) {
        isOverflow = false;
        _throttleCache.put(ip, count + 1);
      }
      else
        isOverflow = true;
    }

    if (isOverflow) {
      log.info(L.l("'{0}' has too many concurrent requests -- throttling.",
                   ip));

      if (response instanceof HttpServletResponse)
        ((HttpServletResponse) response).sendError(503);
      return;
    }

    try {
      if (_requestSemaphore != null) {
        boolean isAcquire = false;

        try {
          if (_requestSemaphore.tryAcquire(_timeout, TimeUnit.MILLISECONDS))
            isAcquire = true;
        } catch (InterruptedException e) {
        }

        if (! isAcquire) {
          if (response instanceof HttpServletResponse)
            ((HttpServletResponse) response).sendError(503);

          return;
        }
      }

      nextFilter.doFilter(request, response);
      
      if (_requestDelay > 0) {
        try {
          Thread.sleep(_requestDelay);
        } catch (Exception e) {
        }
      }
    } finally {
      synchronized (this) {
        int count = _throttleCache.get(ip);

        if (count <= 1)
          _throttleCache.remove(ip);
        else
          _throttleCache.put(ip, count - 1);
      }
      
      if (_requestSemaphore != null) {
        _requestSemaphore.release();
      }
    }
  }

  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }
}
