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

package com.caucho.v5.http.dispatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.ServletException;

import com.caucho.v5.util.L10N;

import io.baratine.inject.InjectManager;

/**
 * Manages the servlets.
 */
public class FilterManager {
  static final Logger log = Logger.getLogger(FilterManager.class.getName());

  static final L10N L = new L10N(FilterManager.class);

  private HashMap<String,FilterConfigImpl> _filters
    = new HashMap<String,FilterConfigImpl>();

  private HashMap<String,Filter> _instances
    = new HashMap<String,Filter>();

  private Map<String, Set<String>> _urlPatterns
    = new HashMap<String, Set<String>>();

  private Map<String, Set<String>> _servletNames
    = new HashMap<String, Set<String>>();

  private InjectManager _injectManager;
  // private Bean _bean;

  /**
   * Adds a filter to the filter manager.
   */
  public void addFilter(FilterConfigImpl config)
  {
    if (config.getServletContext() == null)
      throw new NullPointerException();

    _filters.put(config.getFilterName(), config);
  }

  /**
   * Adds a filter to the filter manager.
   */
  public FilterConfigImpl getFilter(String filterName)
  {
    return _filters.get(filterName);
  }

  public HashMap<String,FilterConfigImpl> getFilters()
  {
    return _filters;
  }

  /**
   * Initialize filters that need starting at server start.
   */
  @PostConstruct
  public void init()
  {
    for (String name : _filters.keySet()) {
      try {
        createFilter(name);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public void addFilterMapping(FilterMapping filterMapping)
  {
    Set<String> patterns = filterMapping.getURLPatterns();
    if (patterns != null) {
      Set<String> urls = _urlPatterns.get(filterMapping.getName());

      if (urls == null) {
        urls = new LinkedHashSet<>();

        _urlPatterns.put(filterMapping.getName(), urls);
      }

      urls.addAll(patterns);
    }

    List<String> servletNames = filterMapping.getServletNames();
    if (servletNames != null && servletNames.size() > 0) {
      Set<String> names = _servletNames.get(filterMapping.getName());

      if (names == null) {
        names = new HashSet<>();

        _servletNames.put(filterMapping.getName(), names);
      }

      names.addAll(servletNames);
    }
  }

  public Set<String> getUrlPatternMappings(String filterName)
  {
    return _urlPatterns.get(filterName);
  }

  public Set<String> getServletNameMappings(String filterName)
  {
    return _servletNames.get(filterName);
  }

  /**
   * Instantiates a filter given its configuration.
   *
   * @param filterName the filter
   *
   * @return the initialized filter.
   */
  public Filter createFilter(String filterName)
    throws ServletException
  {
    FilterConfigImpl config = _filters.get(filterName);

    if (config == null) {
      throw new ServletException(L.l("`{0}' is not a known filter.  Filters must be defined by <filter> before being used.", filterName));
    }

    // Class<?> filterClass = config.getFilterClass();

    /* XXX:
    if (! config.isAvailable(CurrentTime.getCurrentTime()))
      throw config.getInitException();
    */

    synchronized (config) {
      try {
        Filter filter = _instances.get(filterName);

        if (filter != null) {
          return filter;
        }

        InjectManager cdiManager = getCdiManager();

        // ioc/0p2e
       // _filterInjectionTarget = beanManager.discoverInjectionTarget(filterClass);

        filter = config.create(cdiManager);

        _instances.put(filterName, filter);

        return filter;
      } catch (ServletException e) {
        // XXX: log(e.getMessage(), e);

        // XXX: config.setInitException(e);

        throw e;
      } catch (Throwable e) {
        // XXX: log(e.getMessage(), e);

        throw new ServletException(e);
      }
    }
  }

  private InjectManager getCdiManager()
  {
    if (_injectManager == null) {
      _injectManager = InjectManager.current();
    }

    return _injectManager;
  }

  public void destroy()
  {
    ArrayList<Map.Entry<String,Filter>> filterList = new ArrayList<>();

    if (_instances != null) {
      synchronized (_instances) {
        filterList.addAll(_instances.entrySet());
      }
    }

    for (Map.Entry<String,Filter> entry : filterList) {
      String name = entry.getKey();
      Filter filter = entry.getValue();

      try {
        FilterConfigImpl config = _filters.get(name);
        
        if (config != null) {
          config.destroy(filter);
        }
        /*
        if (_bean != null)
          _bean.destroy(filter, null);
          */
        
        filter.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
