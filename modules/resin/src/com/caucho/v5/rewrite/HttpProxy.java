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

package com.caucho.v5.rewrite;

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
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.http.dispatch.ServletBuilder;
import com.caucho.v5.http.proxy.LoadBalanceBackend;
import com.caucho.v5.http.rewrite.DispatchRuleTargetBase;
import com.caucho.v5.http.webapp.WebAppResinBase;
import com.caucho.v5.servlets.HttpProxyServlet;

/**
 * Dispatches a request to a backend server using HTTP as the proxy
 * protocol.
 *
 * <pre>
 * &lt;web-app xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:HttpProxy regexp="^/remote">
 *     &lt;address>127.0.0.1:8080&lt;/address>
 *   &lt;/resin:HttpProxy>
 *
 * &lt;/web-app>
 * </pre>
 */
@Configurable
public class HttpProxy extends DispatchRuleTargetBase
{
  private HttpProxyServlet _proxyServlet;
  private ServletBuilder _servlet;

  public HttpProxy()
  {
    _proxyServlet = new HttpProxyServlet();

    _servlet = new ServletBuilder();

    _servlet.setServletName("resin-dispatch-lb");
    _servlet.setServlet(_proxyServlet);
  }

  /**
   * Adds a backend HTTP server address like "127.0.0.1:8081"
   *
   * @param address the backend address like "127.0.0.1:8081"
   */
  @Configurable
  public void addAddress(String address)
  {
    _proxyServlet.addAddress(address);
  }
  
  @Configurable
  public void setAddresses(String addresses)
  {
    for (String item : addresses.split("[\\s;]+")) {
      if ("".equals(item))
        continue;
      
      addAddress(item);
    }
  }

  @Configurable
  public void addHost(String host)
  {
    addAddress(host);
  }
  
  @Configurable
  public LoadBalanceBackend createBackend()
  {
    return new LoadBalanceBackend();
  }
  
  @Configurable
  public void addBackend(LoadBalanceBackend backend)
  {
    _proxyServlet.addBackend(backend);
  }

  /**
   * Sets the strategy for the load balancer
   */
  @Configurable
  public void setStrategy(String strategy)
  {
    _proxyServlet.setStrategy(strategy);
  }
  
  @Configurable
  public void setConnectTimeout(Period connectTimeout)
  {
    _proxyServlet.setConnectTimeout(connectTimeout);
  }
  
  @Configurable
  public void setConnectionMin(int connectionMin)
  {
    _proxyServlet.setConnectionMin(connectionMin);
  }

  @Configurable
  public void setSocketTimeout(Period socketTimeout)
  {
    _proxyServlet.setSocketTimeout(socketTimeout);
  }

  @Configurable
  public void setIdleTime(Period idleTime)
  {
    _proxyServlet.setIdleTime(idleTime);
  }

  @Configurable
  public void setRecoverTime(Period recoverTime)
  {
    _proxyServlet.setRecoverTime(recoverTime);
  }
  
  @Configurable
  public void setWarmupTime(Period warmupTime)
  {
    _proxyServlet.setWarmupTime(warmupTime);
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      _proxyServlet.init();

      WebAppResinBase webApp = WebAppResinBase.getCurrent();
      if (webApp != null)
        webApp.getBuilder().addServlet(_servlet);
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
      return new ProxyFilterChain(_servlet.createServletChain(),
                                  uri, queryString);
    } catch (ServletException e) {
      throw ConfigException.wrap(e);
    }
  }

  public static class ProxyFilterChain implements FilterChain {
    private final FilterChain _next;
    private final String _uri;
    private final String _queryString;

    ProxyFilterChain(FilterChain next, String uri, String queryString)
    {
      _next = next;

      _uri = uri;
      _queryString = queryString;
    }

    public void doFilter(ServletRequest req,
                         ServletResponse res)
      throws IOException, ServletException
    {
      _next.doFilter(new ProxyRequest(req, _uri, _queryString), res);
    }
  }

  public static class ProxyRequest extends HttpServletRequestWrapper {
    private String _uri;
    private String _queryString;

    ProxyRequest(ServletRequest req,
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
    public String getRequestURI()
    {
      return _uri;
    }

    /**
     * Returns the proxy query string
     */
    public String getQueryString()
    {
      return _queryString;
    }
  }
}
