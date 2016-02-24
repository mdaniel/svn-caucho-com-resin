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

import java.util.Objects;
import java.util.logging.Logger;

import com.caucho.v5.http.dispatch.FilterChainBuilder;
import com.caucho.v5.http.dispatch.FilterManager;
import com.caucho.v5.http.dispatch.FilterMapper;
import com.caucho.v5.http.dispatch.ServletManager;
import com.caucho.v5.http.dispatch.ServletMapper;
import com.caucho.v5.http.security.ConstraintManager;
import com.caucho.v5.util.L10N;

/**
 * dispatch server/filter configuration builder.
 */
public class WebAppBuilderDispatch
{
  private static final L10N L = new L10N(WebAppBuilderDispatch.class);
  
  private static final Logger log
    = Logger.getLogger(WebAppBuilderDispatch.class.getName());
  
  // The servlet manager
  private ServletManager _servletManager;
  // The servlet mapper
  private ServletMapper _servletMapper;
  // True the mapper should be strict
  private boolean _isStrictMapping;
  // True if the servlet init-param is allowed to use EL
  private boolean _servletAllowEL = false;
  // True if requestDispatcher forward is allowed after buffers flush
  private boolean _isAllowForwardAfterFlush = false;

  // The filter manager
  private FilterManager _filterManager;
  // The filter mapper
  private FilterMapper _filterMapper;
  // The filter mapper
  private FilterMapper _loginFilterMapper;
  // The dispatch filter mapper
  private FilterMapper _dispatchFilterMapper;
  // The include filter mapper
  private FilterMapper _includeFilterMapper;
  // The forward filter mapper
  private FilterMapper _forwardFilterMapper;
  // The error filter mapper
  private FilterMapper _errorFilterMapper;
  // True if includes are allowed to wrap a filter (forbidden by servlet spec)
  private boolean _dispatchWrapsFilters;

  // The security constraints
  private ConstraintManager _constraintManager;

  private FilterChainBuilder _securityBuilder;

  private WebAppResinBuilder _builder;
  
  /**
   * Builder Creates the webApp with its environment loader.
   */
  public WebAppBuilderDispatch(WebAppResinBuilder builder)
  {
    Objects.requireNonNull(builder);
    
    _builder = builder;
    
    _servletManager = builder.createServletManager();
    _servletMapper = new ServletMapper(null);
    _servletMapper.setServletManager(_servletManager);
    
    _filterManager = new FilterManager();
    _filterMapper = new FilterMapper();
    _filterMapper.setServletContext(getWebApp());
    _filterMapper.setFilterManager(_filterManager);

    _loginFilterMapper = new FilterMapper();
    _loginFilterMapper.setServletContext(getWebApp());
    _loginFilterMapper.setFilterManager(_filterManager);

    _includeFilterMapper = new FilterMapper();
    _includeFilterMapper.setServletContext(getWebApp());
    _includeFilterMapper.setFilterManager(_filterManager);

    _forwardFilterMapper = new FilterMapper();
    _forwardFilterMapper.setServletContext(getWebApp());
    _forwardFilterMapper.setFilterManager(_filterManager);

    _dispatchFilterMapper = new FilterMapper();
    _dispatchFilterMapper.setServletContext(getWebApp());
    _dispatchFilterMapper.setFilterManager(_filterManager);

    _errorFilterMapper = new FilterMapper();
    _errorFilterMapper.setServletContext(getWebApp());
    _errorFilterMapper.setFilterManager(_filterManager);
  }
  
  private WebAppResinBuilder getBuilder()
  {
    return _builder;
  }
  
  private WebAppResinBase getWebApp()
  {
    return getBuilder().getWebApp();
  }
  
  FilterManager getFilterManager()
  {
    return _filterManager;
  }
  
  FilterMapper getFilterMapper()
  {
    return _filterMapper;
  }
  
  FilterMapper getFilterMapperLogin()
  {
    return _loginFilterMapper;
  }
  
  FilterMapper getFilterMapperInclude()
  {
    return _includeFilterMapper;
  }
  
  FilterMapper getFilterMapperForward()
  {
    return _forwardFilterMapper;
  }
  
  FilterMapper getFilterMapperDispatch()
  {
    return _dispatchFilterMapper;
  }
  
  FilterMapper getFilterMapperError()
  {
    return _errorFilterMapper;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getWebApp().getId() + "]";
  }
}
