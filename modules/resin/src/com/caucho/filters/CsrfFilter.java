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

package com.caucho.filters;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.util.IntMap;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.util.RandomUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Support for CSRF prevention.
 */
public class CsrfFilter implements Filter {
  private static final L10N L = new L10N(CsrfFilter.class);
  private static final Logger log
    = Logger.getLogger(CsrfFilter.class.getName());
  
  public static final String PARAMETER = "cr_csrf";
  public static final String NONCE_MAP = "caucho.resin.csrf.nonce";
  
  private HashSet<String> _allowSet = new HashSet<String>();
  private int _lruSize = 8;
  
  /**
   * Adds an allowed URL, without the check
   */
  @Configurable
  public void addAllow(String entry)
  {
    _allowSet.add(entry);
  }
  
  @Override
  public void init(FilterConfig config)
  {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    
    HttpSession session = req.getSession();
    
    LruCache<String,String> nonceMap;
    
    nonceMap = (LruCache<String,String>) session.getAttribute(NONCE_MAP);
    
    if (nonceMap == null) {
      nonceMap = new LruCache<String,String>(_lruSize);
      
      synchronized (session) {
        if (session.getAttribute(NONCE_MAP) == null)
          session.setAttribute(NONCE_MAP, nonceMap);
        
        nonceMap = (LruCache<String,String>) session.getAttribute(NONCE_MAP);
      }
    }
    
    String url = req.getServletPath();
    
    if (req.getPathInfo() != null)
      url += req.getPathInfo();
    
    if (! _allowSet.contains(url)) {
      String nonceValue = req.getParameter(PARAMETER);
      
      if (nonceMap.get(nonceValue) == null) {
        res.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
    }
    
    String nonce = generateNonce();
    
    nonceMap.put(nonce, nonce);
    
    CsrfResponse csrfResponse = new CsrfResponse(res, nonce);
    try {
      nextFilter.doFilter(request, csrfResponse);
    } finally {
      csrfResponse.close();
    }
  }
  
  private String generateNonce()
  {
    long nonceValue = RandomUtil.getRandomLong();
    
    StringBuilder sb = new StringBuilder();
    
    fillBase64(sb, nonceValue);
    fillBase64(sb, nonceValue >> 6);
    fillBase64(sb, nonceValue >> 12);
    fillBase64(sb, nonceValue >> 18);
    fillBase64(sb, nonceValue >> 24);
    fillBase64(sb, nonceValue >> 30);
    fillBase64(sb, nonceValue >> 36);
    fillBase64(sb, nonceValue >> 42);
    fillBase64(sb, nonceValue >> 48);
    fillBase64(sb, nonceValue >> 54);
    fillBase64(sb, nonceValue >> 60);
    
    return sb.toString();
  }
  
  private void fillBase64(StringBuilder sb, long longValue)
  {
    int value = (int) (longValue & 0x3f);
    
    if (value < 26)
      sb.append((char) ('a' + value));
    else if (value < 52)
      sb.append((char) ('A' + value - 26));
    else if (value < 62)
      sb.append((char) ('0' + value - 52));
    else if (value == 62)
      sb.append('_');
    else
      sb.append('-');
  }
  

  /**
   * Any cleanup for the filter.
   */
  @Override
  public void destroy()
  {
  }
  
  static class CsrfResponse extends CauchoResponseWrapper {
    private String _nonce;
    
    CsrfResponse(HttpServletResponse response, String nonce)
    {
      super(response);
      
      _nonce = nonce;
    }
    
    @Override
    public String encodeUrl(String url)
    {
      return encodeURL(url);
    }
    
    @Override
    public String encodeURL(String url)
    {
      return super.encodeURL(rewriteUrl(url));
    }
    
    @Override
    public String encodeRedirectUrl(String url)
    {
      return encodeRedirectURL(url);
    }
    
    @Override
    public String encodeRedirectURL(String url)
    {
      return super.encodeRedirectURL(rewriteUrl(url));
    }
    
    String rewriteUrl(String url)
    {
      int q = url.indexOf('?');
      int a = url.indexOf('#');
      
      if (q < 0) {
        url = url + "?" + PARAMETER + "=" + _nonce; 
      }
      else if (a < 0) {
        url = url + "&" + PARAMETER + "=" + _nonce;
      }
      else {
        url = (url.substring(0, a) + "&" + PARAMETER + "=" + _nonce
               + url.substring(a));
      }
      
      return url;
    }
  }
}
