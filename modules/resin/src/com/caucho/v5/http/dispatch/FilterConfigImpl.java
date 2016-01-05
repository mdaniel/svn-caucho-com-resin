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

package com.caucho.v5.http.dispatch;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.annotation.DisableConfig;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.types.InitParam;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.util.CauchoUtil;

import io.baratine.config.Configurable;
import io.baratine.inject.InjectManager;

/**
 * Configuration for a filter.
 */
public class FilterConfigImpl
  implements FilterConfig, FilterRegistration.Dynamic
{
  private String _filterName;
  private String _filterClassName;
  private Class<?> _filterClass;
  private String _displayName;
  private HashMap<String,String> _initParams = new HashMap<String,String>();

  private ContainerProgram _init;

  private WebApp _webApp;
  private ServletContext _servletContext;
  private FilterManager _filterManager;
  private Filter _filter;
  private boolean _isAsyncSupported;
  // private Bean<?> _bean;

  /**
   * Creates a new filter configuration object.
   */
  public FilterConfigImpl()
  {
  }
  
  public void setId(String id)
  {
  }

  /**
   * Sets the filter name.
   */
  public void setFilterName(String name)
  {
    _filterName = name;
  }

  /**
   * Gets the filter name.
   */
  @Override
  public String getFilterName()
  {
    return _filterName;
  }

  /**
   * Sets the filter class.
   */
  public void setFilterClass(String filterClassName)
    throws ConfigException, ClassNotFoundException
  {
    _filterClassName = filterClassName;
    
    _filterClass = CauchoUtil.loadClass(filterClassName);

    ConfigContext.validate(_filterClass, Filter.class);
  }

  @DisableConfig
  public void setFilterClass(Class<?> filterClass)
  {
    _filterClass = filterClass;

    ConfigContext.validate(_filterClass, Filter.class);
  }

  /**
   * Gets the filter name.
   */
  public Class<?> getFilterClass()
  {
    return _filterClass;
  }

  /**
   * Gets the filter name.
   */
  public String getFilterClassName()
  {
    return _filterClassName;
  }

  public Filter getFilter()
  {
    return _filter;
  }

  public void setFilter(Filter filter)
  {
    _filter = filter;
  }

  /**
   * Sets an init-param
   */
  public void setInitParam(String param, String value)
  {
    _initParams.put(param, value);
  }

  /**
   * Sets an init-param
   */
  public void setInitParam(InitParam initParam)
  {
    _initParams.putAll(initParam.getParameters());
  }

  /**
   * Gets the init params
   */
  public Map<String,String> getInitParamMap()
  {
    return _initParams;
  }

  /**
   * Gets the init params
   */
  @Override
  public String getInitParameter(String name)
  {
    return _initParams.get(name);
  }

  /**
   * Gets the init params
   */
  @Override
  public Enumeration<String> getInitParameterNames()
  {
    return Collections.enumeration(_initParams.keySet());
  }

  public void setWebApp(WebApp webApp)
  {
    _webApp = webApp;
  }

  /**
   * Returns the servlet context.
   */
  @Override
  public ServletContext getServletContext()
  {
    return _servletContext;
  }

  /**
   * Sets the servlet context.
   */
  public void setServletContext(ServletContext app)
  {
    _servletContext = app;
  }

  public FilterManager getFilterManager()
  {
    return _filterManager;
  }

  public void setFilterManager(FilterManager filterManager)
  {
    _filterManager = filterManager;
  }

  /**
   * Sets the init block
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Gets the init block
   */
  public ContainerProgram getInit()
  {
    return _init;
  }

  /**
   * Sets the display name
   */
  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  /**
   * Gets the display name
   */
  public String getDisplayName()
  {
    return _displayName;
  }

  @Override
  public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes,
                                        boolean isMatchAfter,
                                        String... servletNames)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    try {
      FilterMapping mapping = new FilterMapping();
      mapping.setServletContext(_webApp);

      mapping.setFilterName(_filterName);

      if (dispatcherTypes != null) {
        for (DispatcherType dispatcherType : dispatcherTypes) {
          mapping.addDispatcher(dispatcherType);
        }
      }

      for (String servletName : servletNames) {
          mapping.addServletName(servletName);
      }

      _webApp.getBuilder().addFilterMapping(mapping);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      //XXX: needs better exception handling
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public Collection<String> getServletNameMappings()
  {
    Set<String> names = _filterManager.getServletNameMappings(_filterName);

    if (names == null)
      return Collections.EMPTY_SET;

    return Collections.unmodifiableSet(names);
  }

  @Override
  public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes,
                                       boolean isMatchAfter,
                                       String... urlPatterns)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    try {
      FilterMapping mapping = new FilterMapping();
      mapping.setServletContext(_webApp);

      mapping.setFilterName(_filterName);

      if (dispatcherTypes != null) {
        for (DispatcherType dispatcherType : dispatcherTypes) {
          mapping.addDispatcher(dispatcherType);
        }
      }

      FilterMapping.URLPattern urlPattern = mapping.createUrlPattern();

      for (String pattern : urlPatterns) {
        urlPattern.addText(pattern);
      }

      urlPattern.init();

      _webApp.getBuilder().addFilterMapping(mapping);
    }
    catch (Exception e) {
      //XXX: needs better exception handling
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public Collection<String> getUrlPatternMappings()
  {
    Set<String> patterns = _filterManager.getUrlPatternMappings(_filterName);

    if (patterns == null)
      return Collections.EMPTY_SET;

    return Collections.unmodifiableSet(patterns);
  }

  @Override
  public String getName()
  {
    return _filterName;
  }

  @Override
  public String getClassName()
  {
    return _filterClassName;
  }

  @Override
  public boolean setInitParameter(String name, String value)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    if (_initParams.containsKey(name))
      return false;

    _initParams.put(name, value);
    
    return true;
  }

  public Set<String> setInitParameters(Map<String, String> initParameters)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();
    
    Set<String> conflicts = new HashSet<String>();

    for (Map.Entry<String, String> parameter : initParameters.entrySet()) {
      if (_initParams.containsKey(parameter.getKey()))
        conflicts.add(parameter.getKey());
      else
        _initParams.put(parameter.getKey(), parameter.getValue());
    }

    return conflicts;
  }

  @Override
  public Map<String, String> getInitParameters()
  {
    return _initParams;
  }

  @Override
  public void setAsyncSupported(boolean isAsyncSupported)
  {
    if (_webApp != null && ! _webApp.isInitializing())
      throw new IllegalStateException();
    
   _isAsyncSupported = isAsyncSupported;
  }

  public boolean isAsyncSupported()
  {
    return _isAsyncSupported;
  }

  /**
   * Sets the description
   */
  @Configurable
  public void setDescription(String description)
  {
  }

  /**
   * Sets the icon
   */
  @Configurable
  public void setIcon(String icon)
  {
  }

  public Filter create(InjectManager inject)
    throws ServletException
  {
    if (_filter != null) {
      ConfigContext.inject(_filter);
      _filter.init(this);
      return _filter;
    }
    
    //Bean<?> bean = getBean(cdiManager);

    //filter = config.getFilter();

    //CreationalContext env = cdiManager.createCreationalContext(null);

    //Filter filter = (Filter) bean.create(env);
    
    Filter filter = (Filter) inject.lookup(getFilterClass());

    // Initialize bean properties
    ContainerProgram init = getInit();

    if (init != null)
      init.inject(filter);

    filter.init(this);

    return filter;
  }

  public void destroy(Filter filter)
  {
    /*
    Bean bean = _bean;
    
    if (bean != null) {
      bean.destroy(filter, null);
    }
    */
  }

  /*
  Bean<?> getBean(CandiManager cdiManager)
  {
    if (_bean != null) {
      return _bean;
    }

    Set<Bean<?>>  beans = cdiManager.getBeans(getFilterClass());
    Bean<?> bean = cdiManager.resolve(beans);

    if (bean == null) {
      bean = cdiManager.createTransientBean(getFilterClass());
    }

    _bean = bean;

    return bean;
  }
  */

  /**
   * Returns a printable representation of the filter config object.
   */
  public String toString()
  {
    return getClass().getSimpleName() + "[name=" + _filterName + ",class=" + _filterClass + "]";
  }
}
