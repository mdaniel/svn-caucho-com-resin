/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.util.*;

import java.util.logging.Level;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import javax.servlet.jsp.el.VariableResolver;

import com.caucho.util.*;

import com.caucho.config.types.InitProgram;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.server.webapp.Application;

/**
 * Configuration for a servlet.
 */
public class ServletMapping extends ServletConfigImpl {
  private static final L10N L = new L10N(ServletMapping.class);

  private String _urlPattern;
  private String _urlRegexp;
  private boolean _isStrictMapping;
  
  /**
   * Creates a new servlet mapping object.
   */
  public ServletMapping()
  {
  }

  /**
   * Sets the url pattern
   */
  public void setURLPattern(String pattern)
  {
    _urlPattern = pattern;
  }

  /**
   * Gets the url pattern
   */
  public String getURLPattern()
  {
    return _urlPattern;
  }

  /**
   * Sets the url regexp
   */
  public void setURLRegexp(String pattern)
  {
    _urlRegexp = pattern;
  }

  /**
   * Gets the url regexp
   */
  public String getURLRegexp()
  {
    return _urlRegexp;
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
   * initialize.
   */
  public void init()
    throws ServletException
  {
    if (_urlRegexp == null) {
      if (getServletName() == null && getServletClassName() != null) {
	// server/13f4
	setServletName(getURLPattern());
      }

      super.init();
    }
  }

  /**
   * Initialize for a regexp.
   */
  String initRegexp(ServletContext application,
		    ServletManager manager,
		    ArrayList<String> vars)
    throws ServletException
  {
    VariableResolver env = EL.getEnvironment();
    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("regexp", vars);
    MapVariableResolver mapEnv = new MapVariableResolver(map, env);

    String rawName = getServletName();
    String rawClassName = getServletClassName();

    if (rawName == null)
      rawName = rawClassName;

    if (rawClassName == null)
      rawClassName = rawName;

    try {
      String servletName = EL.evalString(rawName, mapEnv);

      if (manager.getServletConfig(servletName) != null)
	return servletName;
      
      String className = EL.evalString(rawClassName, mapEnv);

      try {
	Application app = (Application) getServletContext();

	Class cl = Class.forName(className, false, app.getClassLoader());
      } catch (ClassNotFoundException e) {
	log.log(Level.WARNING, e.toString(), e);

	return null;
      }

      ServletConfigImpl config = new ServletConfigImpl();

      config.setServletName(servletName);
      config.setServletClass(className);
      config.setServletContext(application);

      InitProgram program = getInit();
      if (program != null)
	program.init(config);

      config.init();

      manager.addServlet(config);

      return servletName;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Throwable e) {
      throw new ServletException(e);
    }
  }

  /**
   * Returns a printable representation of the servlet config object.
   */
  public String toString()
  {
    return "ServletMapping[pattern=" + _urlPattern + ",name=" + getServletName() + "]";
  }
}
