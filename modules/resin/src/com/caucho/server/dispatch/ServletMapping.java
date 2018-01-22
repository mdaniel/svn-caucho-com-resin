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

package com.caucho.server.dispatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import javax.el.ELContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;

/**
 * Configuration for a servlet.
 */
public class ServletMapping extends ServletConfigImpl {
  private static final L10N L = new L10N(ServletMapping.class);

  private ArrayList<Mapping> _mappingList
    = new ArrayList<Mapping>();

  private boolean _isStrictMapping;
  private boolean _isRegexp;
  private boolean _ifAbsent;
  private boolean _isDefault;

  /**
   * Creates a new servlet mapping object.
   */
  public ServletMapping()
  {
  }

  public void create(WebServlet webServlet)
  {
    if (webServlet.value().length > 0 && webServlet.urlPatterns().length == 0) {
      for (String url : webServlet.value()) {
        addURLPattern(url); // XXX: support addURLRegexp?
      }
    }
    else if (webServlet.urlPatterns().length > 0 &&
             webServlet.value().length == 0) {
      for (String url : webServlet.urlPatterns()) {
        addURLPattern(url); // XXX: support addURLRegexp?
      }
    } 
    else {
      throw new ConfigException(L.l("Annotation @WebServlet at '{0}' must specify either value or urlPatterns",
                                    getServletClassName()));
    }

    for (WebInitParam initParam : webServlet.initParams()) {
      setInitParam(initParam.name(), initParam.value()); //omit description
    }

    setLoadOnStartup(webServlet.loadOnStartup());
    setAsyncSupported(webServlet.asyncSupported());
  }

  public void setIfAbsent(boolean ifAbsent)
  {
    _ifAbsent = ifAbsent;
  }

  /**
   * Sets the url pattern
   */
  public void addURLPattern(String pattern)
  {
    if (pattern.indexOf('\n') > -1) {
      throw new ConfigException(L.l("'url-pattern' cannot contain newline"));
    }

    _mappingList.add(new Mapping(pattern, null));

    // server/13f4
    if (getServletNameDefault() == null)
      setServletNameDefault(pattern);
  }

  /**
   * Sets the url regexp
   */
  public void addURLRegexp(String pattern)
  {
    _mappingList.add(new Mapping(null, pattern));
  }

  /**
   * True if strict mapping should be enabled.
   */
  public boolean isStrictMapping()
  {
    return _isStrictMapping;
  }

  /**
   * Set if strict mapping should be enabled.
   */
  public void setStrictMapping(boolean isStrictMapping)
  {
    _isStrictMapping = isStrictMapping;
  }

  /**
   * Set for default mapping that can be overridden by programmatic mapping.
   */
  public void setDefault(boolean isDefault)
  {
    _isDefault = isDefault;
  }

  /**
   * True for default mapping that can be overridden by programmatic mapping.
   */
  public boolean isDefault()
  {
    return _isDefault;
  }

  /**
   * initialize.
   */
  public void init(ServletMapper mapper)
    throws ServletException
  {
    boolean hasInit = false;

    if (getServletName() == null)
      setServletName(getServletNameDefault());
    
    if (getServletName() != null && getServletName().indexOf("${") >= 0)
      _isRegexp = true;
    
    if (getServletClassName() != null && getServletClassName().indexOf("${") >= 0)
      _isRegexp = true;
    
    boolean ifAbsent = _ifAbsent;

    for (int i = 0; i < _mappingList.size(); i++) {
      Mapping mapping = _mappingList.get(i);

      String urlPattern = mapping.getUrlPattern();
      String urlRegexp = mapping.getUrlRegexp();
      
      if (getServletName() == null
          && getServletClassName() != null
          && urlPattern != null) {
        setServletName(urlPattern);
      }

      if (urlPattern != null && ! hasInit) {
        hasInit = true;
        super.init();

        if (getServletClassName() != null)
          mapper.getServletManager().addServlet(this);
      }

      if (urlPattern != null) {
        if (mapper.addUrlMapping(urlPattern, getServletName(), this, ifAbsent)) {
          ifAbsent = false;
        }
      }
      else
        mapper.addUrlRegexp(urlRegexp, getServletName(), this);
    }

    /*
    if (_urlRegexp == null) {
      if (getServletName() == null && getServletClassName() != null) {
        // server/13f4
      }

    }
    */
  }
  
  Class<?> getServletClass(ArrayList<String> vars)
  {
    if (vars.size() > 1 || _isRegexp) {
      return initRegexpClass(vars);
    }
    else {
      return getServletClass();
    }
  }

  /**
   * Initialize for a regexp.
   */
  String initRegexpName(ArrayList<String> vars)
  {
    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("regexp", vars);

    ELContext mapEnv = new ConfigELContext(new MapVariableResolver(map));

    String rawName = getServletName();
    String rawClassName = getServletClassName();

    if (rawName == null)
      rawName = rawClassName;

    if (rawClassName == null)
      rawClassName = rawName;

    try {
      return EL.evalString(rawName, mapEnv);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Initialize for a regexp.
   */
  Class<?> initRegexpClass(ArrayList<String> vars)
  {
    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("regexp", vars);

    ELContext mapEnv = new ConfigELContext(new MapVariableResolver(map));

    String rawName = getServletName();
    String rawClassName = getServletClassName();

    if (rawName == null)
      rawName = rawClassName;

    if (rawClassName == null)
      rawClassName = rawName;

    try {
      String className = EL.evalString(rawClassName, mapEnv);

      try {
        WebApp app = (WebApp) getServletContext();

        return Class.forName(className, false, app.getClassLoader());
      } catch (ClassNotFoundException e) {
        log.log(Level.WARNING, e.toString(), e);

        return null;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Initialize for a regexp.
   */
  ServletMapping initRegexpConfig(ArrayList<String> vars)
  {
    if (vars.size() == 0 || ! _isRegexp)
      return null;
    
    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("regexp", vars);

    ELContext mapEnv = new ConfigELContext(new MapVariableResolver(map));

    String rawName = getServletName();
    String rawClassName = getServletClassName();

    if (rawName == null)
      rawName = rawClassName;

    if (rawClassName == null)
      rawClassName = rawName;

    try {
      String servletName = EL.evalString(rawName, mapEnv);
      
      String className = EL.evalString(rawClassName, mapEnv);
      
      ServletMapping config = new ServletMapping();
      config.setServletContext(getServletContext());
      config.setServletName(servletName);
      config.setServletClass(className);
      
      config.copyFrom(this);
      
      config.init();
      
      return config;
      
      /*
      try {
        WebApp app = (WebApp) getServletContext();

        return Class.forName(className, false, app.getClassLoader());
      } catch (ClassNotFoundException e) {
        log.log(Level.WARNING, e.toString(), e);

        return null;
      }
      */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns a printable representation of the servlet config object.
   */
  public String toString()
  {
    StringBuilder builder = new StringBuilder();

    builder.append("ServletMapping[");

    for (int i = 0; i < _mappingList.size(); i++) {
      Mapping mapping = _mappingList.get(i);

      if (mapping.getUrlPattern() != null) {
        builder.append("url-pattern=");
        builder.append(mapping.getUrlPattern());
        builder.append(", ");
      }
      else if (mapping.getUrlRegexp() != null) {
        builder.append("url-regexp=");
        builder.append(mapping.getUrlRegexp());
        builder.append(", ");
      }
    }

    builder.append("name=");
    builder.append(getServletName());

    if (getServletClassName() != null) {
      builder.append(", class=");
      builder.append(getServletClassName());
    }

    builder.append("]");

    return builder.toString();
  }

  static class Mapping {
    private final String _urlPattern;
    private final String _urlRegexp;

    Mapping(String urlPattern, String urlRegexp)
    {
      _urlPattern = urlPattern;
      _urlRegexp = urlRegexp;
    }

    String getUrlPattern()
    {
      return _urlPattern;
    }

    String getUrlRegexp()
    {
      return _urlRegexp;
    }

    public String toString()
    {
      return "ServletMapping[" + _urlPattern + ", " + _urlRegexp + "]";
    }
  }
}
