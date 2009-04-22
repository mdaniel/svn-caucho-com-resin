/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.webapp;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.servlet.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bare-bones servlet context implementation.
 */
public class ServletContextCompat {
  /**
   * Adds a runtime servlet.
   *
   * @Since Servlet 3.0
   */
  public void addServlet(String servletName,
			 String description,
			 String className,
			 Map<String,String> initParam,
			 int loadOnStartup,
			 boolean isAsyncSupported)
  {
  }

  /**
   * Adds a runtime servlet mapping
   *
   * @Since Servlet 3.0
   */
  public void addServletMapping(String servletName,
			       String []urlPatterns)
  {
  }

  /**
   * Adds a filter.
   *
   * @Since Servlet 3.0
   */
  public void addFilter(String filterName,
			String description,
			String className,
			Map<String,String> initParam,
			boolean isAsyncSupported)
  {
  }

  /**
   * Adds a filter mapping.
   *
   * @Since Servlet 3.0
   */
  public void addFilterMappingForServletNames(String filterName,
					      EnumSet<DispatcherType> dispatcherTypes,
					      boolean isMatchAfter,
					      String ... servletNames)
  {
  }

  /**
   * Adds a filter mapping.
   *
   * @Since Servlet 3.0
   */
  public void addFilterMappingForUrlPatterns(String filterName,
					      EnumSet<DispatcherType> dispatcherTypes,
					      boolean isMatchAfter,
					      String ... urlPatterns)
  {
  }

  /**
   * The session cookie configuration
   *
   * @Since Servlet 3.0
   */
  public void setSessionCookieConfig(SessionCookieConfig cookieConfig)
  {
  }

  /**
   * Sets the session cookie configuration
   *
   * @Since Servlet 3.0
   */
  public SessionCookieConfig getSessionCookieConfig()
  {
    return null;
  }

  /**
   * The session tracking mode
   *
   * @Since Servlet 3.0
   */
  public void setSessionTrackingModes(EnumSet<SessionTrackingMode> modes)
  {
  }

  /**
   * The session tracking mode
   *
   * @Since Servlet 3.0
   */
  public EnumSet<SessionTrackingMode> getDefaultSessionTrackingModes()
  {
    return null;
  }

  /**
   * The session tracking mode
   *
   * @Since Servlet 3.0
   */
  public EnumSet<SessionTrackingMode> getEffectiveSessionTrackingModes()
  {
    return null;
  }
}
