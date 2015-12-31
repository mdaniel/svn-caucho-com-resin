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

package com.caucho.v5.http.webapp;

import io.baratine.service.Result;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.deploy.DeployHandle;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.dispatch.InvocationDecoder;
import com.caucho.v5.http.host.Host;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;

/**
 * Selets web-apps based on URL.
 */
public class WebAppRouter
{
  private static final L10N L = new L10N(WebApp.class);
  private static final Logger log
    = Logger.getLogger(WebAppRouter.class.getName());

  private HttpContainerServlet _httpContainer;
  private Host _host;

  private WebAppRouterService _routerService;

  private static final int URI_CACHE_SIZE = 8192;
  
  // LRU cache for the webApp lookup
  private LruCache<String,WebAppUriMap> _uriToAppCache
    = new LruCache<>(URI_CACHE_SIZE);
  
  private volatile HashMap<String,DeployHandle<WebApp>> _webAppMap
    = new HashMap<>();

  private long _startWaitTime = 10000L;
  private InvocationDecoder _invocationDecoder;

  /**
   * Creates the webApp with its environment loader.
   */
  public WebAppRouter(HttpContainerServlet httpContainer,
                      Host host)
  {
    Objects.requireNonNull(httpContainer);
    Objects.requireNonNull(host);

    _invocationDecoder = httpContainer.getInvocationDecoder();

    _httpContainer = httpContainer;
    _host = host;
    
    ServiceManagerAmp ampManager = AmpSystem.getCurrentManager();
    
    WebAppRouterServiceImpl routerServiceImpl = new WebAppRouterServiceImpl(this);

    _routerService = ampManager.service(routerServiceImpl)
                               .start()
                               .as(WebAppRouterService.class);
  }

  private InvocationDecoder getInvocationDecoder()
  {
    return _invocationDecoder;
  }

  /**
   * Returns the owning host.
   */
  public Host getHost()
  {
    return _host;
  }
  
  /**
   * Returns the URL for the container.
   */
  public String getURL()
  {
    return _host.getURL();
  }

  /**
   * Returns the URL for the container.
   */
  public String getId()
  {
    return getURL();
  }

  /**
   * Returns the host name for the container.
   */
  public String getHostName()
  {
    return "";
  }

  /**
   * Starts the container.
   */
  public void start()
  {
    try {
      _routerService.start(Result.ignore());
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  void setWebAppMap(HashMap<String, DeployHandle<WebApp>> webAppMap)
  {
    _webAppMap = webAppMap;
    
    clearCache();
  }

  /**
   * Clears the cache
   */
  private void clearCache()
  {
    _uriToAppCache = new LruCache<String,WebAppUriMap>(URI_CACHE_SIZE);
    
    _httpContainer.clearCache();
  }

  /**
   * Finds the web-app matching the current entry.
   */
  public WebAppUriMap find(String uri)
  {
    WebAppUriMap entry = _uriToAppCache.get(uri);

    if (entry != null) {
      return entry;
    }

    String cleanUri = uri;
    if (CauchoUtil.isCaseInsensitive()) {
      cleanUri = cleanUri.toLowerCase(Locale.ENGLISH);
    }

    // server/105w
    try {
      cleanUri = getInvocationDecoder().normalizeUri(cleanUri);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    entry = findImpl(cleanUri, _webAppMap);
    _uriToAppCache.put(uri, entry);

    return entry;
  }

  /**
   * Finds the web-app for the entry.
   */
  private WebAppUriMap findImpl(String uri, 
                                Map<String,DeployHandle<WebApp>> webAppMap)
  {
    WebAppUriMap entry = _uriToAppCache.get(uri);

    if (entry != null) {
      return entry ;
    }
    
    DeployHandle<WebApp> webAppHandle = webAppMap.get(uri);
    
    if (webAppHandle != null) {
      entry = new WebAppUriMap(uri, webAppHandle);
      
      _uriToAppCache.put(uri, entry);

      return entry;
    }
    
    int tail = uri.lastIndexOf('/');
    
    if (tail < 0) {
      return null;
    }
    
    String subURI = uri.substring(0, tail);
    
    entry = findImpl(subURI, webAppMap);
    
    if (entry != null) {
      _uriToAppCache.put(uri, entry);
    }

    return entry;
  }

  /**
   * Returns a list of the webApps.
   */
  public DeployHandle<WebApp> []getWebAppHandles()
  {
    return _routerService.getHandles();
  }

  /**
   * Closes the container.
   */
  public boolean shutdown(ShutdownModeAmp mode)
  {
    _routerService.shutdown(mode);

    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _host.getId() + "]";
  }
  
  static class WebAppUriMap
  {
    private final String _contextPath;
    private final DeployHandle<WebApp> _webAppHandle;
    
    WebAppUriMap(String contextPath,
                 DeployHandle<WebApp> webAppHandle)
    {
      _contextPath = contextPath;
      _webAppHandle = webAppHandle;
    }
    
    String getContextPath()
    {
      return _contextPath;
    }
    
    DeployHandle<WebApp> getHandle()
    {
      return _webAppHandle;
    }
  }
}
