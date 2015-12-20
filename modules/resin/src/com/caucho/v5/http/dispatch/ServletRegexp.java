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

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;

import com.caucho.v5.config.Config;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.types.RawString;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.util.L10N;

/**
 * Configuration for a servlet regexp.
 */
public class ServletRegexp {
  private static final L10N L = new L10N(ServletRegexp.class);

  private String _urlRegexp;

  private String _servletName;
  private String _servletClassName;
  
  // The configuration program
  private ContainerProgram _program = new ContainerProgram();
  
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
  public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Returns the program.
   */
  public ContainerProgram getBuilderProgram()
  {
    return _program;
  }

  /**
   * Initialize for a regexp.
   */
  public String initRegexp(WebApp webApp,
                           ServletMapper mapper,
                    ArrayList<String> vars)
    throws ServletException
  {
    // ELContext env = EL.getEnvironment();
    
    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("regexp", vars);

    //ELContext mapEnv = new ConfigELContext(new MapVariableResolver(map));

    String rawName = _servletName;
    String rawClassName = _servletClassName;

    if (rawName == null)
      rawName = rawClassName;

    try {
      String servletName = Config.evalString(rawName);

      /*
      if (manager.getServletConfig(servletName) != null)
        return servletName;
        */
      
      String className = Config.evalString(rawClassName);

      ServletBuilder config = new ServletBuilder();

      ServletMapping mapping = new ServletMapping();
      
      mapping.addURLRegexp(getURLRegexp());
      mapping.setServletName(getServletName());
      mapping.setServletClass(rawClassName);
      mapping.setServletContext(webApp);
      getBuilderProgram().configure(mapping);
      mapping.setStrictMapping(webApp.getBuilder().getStrictMapping());
      mapping.init(mapper);
      
      /*

      config.setServletName(servletName);
      config.setServletClass(className);
      config.setServletContext(application);

      _program.configure(config);

      config.init();

      manager.addServlet(config);
      */

      return servletName;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Returns a printable representation of the servlet config object.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _urlRegexp + "]";
  }
}
