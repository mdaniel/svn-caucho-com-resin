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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.dispatch;

import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import javax.servlet.jsp.el.VariableResolver;

import com.caucho.util.*;

import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;
import com.caucho.config.types.RawString;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

/**
 * Configuration for a servlet regexp.
 */
public class ServletRegexp {
  private static final L10N L = new L10N(ServletRegexp.class);

  private String _urlRegexp;

  private String _servletName;
  private String _servletClassName;
  
  // The configuration program
  private BuilderProgramContainer _program = new BuilderProgramContainer();
  
  /**
   * Creates a new servlet regexp object.
   */
  public ServletRegexp()
  {
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
   * Sets the servlet name.
   */
  public void setServletName(RawString string)
  {
    _servletName = string.getValue();
  }

  /**
   * Sets the servlet name.
   */
  public String getServletName()
  {
    return _servletName;
  }

  /**
   * Sets the servlet class name.
   */
  public void setServletClass(RawString string)
  {
    _servletClassName = string.getValue();
  }

  /**
   * Gets the servlet class name.
   */
  public String getServletClass()
  {
    return _servletClassName;
  }

  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Returns the program.
   */
  public BuilderProgram getBuilderProgram()
  {
    return _program;
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

    String rawName = _servletName;
    String rawClassName = _servletClassName;

    if (rawName == null)
      rawName = rawClassName;

    try {
      String servletName = EL.evalString(rawName, mapEnv);

      if (manager.getServletConfig(servletName) != null)
	return servletName;
      
      String className = EL.evalString(rawClassName, mapEnv);

      ServletConfigImpl config = new ServletConfigImpl();

      config.setServletName(servletName);
      config.setServletClass(className);
      config.setServletContext(application);

      _program.configure(config);

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
    return "ServletRegexp[" + _urlRegexp + "]";
  }
}
