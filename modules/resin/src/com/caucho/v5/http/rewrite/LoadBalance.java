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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.rewrite;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.InlineConfig;
import com.caucho.v5.http.dispatch.ServletBuilder;
import com.caucho.v5.http.proxy.HashGeneratorStickyRequest;
import com.caucho.v5.http.proxy.ProxyLoadBalanceServlet;
import com.caucho.v5.http.rewrite.DispatchRuleTargetBase;
import com.caucho.v5.http.webapp.WebApp;

import io.baratine.config.Configurable;

/**
 * Dispatches a request to the load balancer.
 *
 * <pre>
 * &lt;web-app xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:LoadBalance regexp="^/remote" cluster="a"/>
 *
 * &lt;/web-app>
 * </pre>
 */
@Configurable
public class LoadBalance extends DispatchRuleTargetBase
{
  private WebApp _webApp = WebApp.getCurrent();

  private ProxyLoadBalanceServlet _loadBalanceServlet;
  private ServletBuilder _servlet;

  public LoadBalance()
  {
    _loadBalanceServlet = new ProxyLoadBalanceServlet();
  }

  /**
   * Sets the target cluster for the load balancer
   */
  @Configurable
  public void setCluster(String cluster)
  {
    _loadBalanceServlet.setCluster(cluster);
  }

  /**
   * Sets the strategy for the load balancer
   */
  @Configurable
  public void setStrategy(String strategy)
  {
    _loadBalanceServlet.setStrategy(strategy);
  }
  
  @Configurable
  public void setStickySessions(boolean isSticky)
  {
    _loadBalanceServlet.setStickySessions(isSticky);
  }
  
  @Configurable
  public void setSessionCookie(String sessionId)
  {
    _loadBalanceServlet.setSessionCookie(sessionId);
  }
  
  @Configurable
  public void add(HashGeneratorStickyRequest gen)
  {
    _loadBalanceServlet.setStickyGenerator(gen);
  }
  
  @Override
  protected String rewriteDefault(String uri, String queryString)
  {
    if (_webApp == null)
      return uri;
    
    String contextPath = _webApp.getContextPath();

    if (! contextPath.equals("/")) {
      // server/1kw2
      return contextPath + uri;
    }
    else {
      return uri;
    }
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      _webApp = WebApp.getCurrent();

      _servlet = new ServletBuilder();

      _servlet.setServletContext(_webApp);
      _servlet.setServletName("resin-dispatch-lb");
      _servlet.setServlet(_loadBalanceServlet);

      _loadBalanceServlet.init(_servlet);

      if (_webApp != null)
        _webApp.getBuilder().addServlet(_servlet);
    }
    catch (ServletException ex) {
      throw ConfigException.wrap(ex);
    }
  }

  @Override
  public FilterChain createDispatch(DispatcherType type,
                                    String uri,
                                    String queryString,
                                    String target,
                                    FilterChain next)
  {
    try {
      return new LoadBalanceFilterChain(_servlet.createServletChain(),
                                        uri, queryString, target);
    } catch (ServletException e) {
      throw ConfigException.wrap(e);
    }
  }

  public static class LoadBalanceFilterChain implements FilterChain {
    private final FilterChain _next;
    private final String _uri;
    private final String _queryString;
    private final String _target;

    LoadBalanceFilterChain(FilterChain next,
                           String uri,
                           String queryString,
                           String target)
    {
      _next = next;

      _uri = uri;
      _queryString = queryString;
      _target = target;
    }

    @Override
    public void doFilter(ServletRequest req,
                         ServletResponse res)
      throws IOException, ServletException
    {
      if (_target != null)
        _next.doFilter(new LoadBalanceRequest(req, _target, _queryString), res);
      else
        _next.doFilter(req, res);
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _next + "]";
    }
  }

  public static class LoadBalanceRequest extends HttpServletRequestWrapper {
    private String _uri;
    private String _queryString;

    LoadBalanceRequest(ServletRequest req,
                       String uri,
                       String queryString)
    {
      super((HttpServletRequest) req);

      _uri = uri;
      _queryString = queryString;
    }

    /**
     * Returns the proxy uri
     */
    @Override
    public String getRequestURI()
    {
      return _uri;
    }

    /**
     * Returns the proxy query string
     */
    @Override
    public String getQueryString()
    {
      return _queryString;
    }
  }
}
