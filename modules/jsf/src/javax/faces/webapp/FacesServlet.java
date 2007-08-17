/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package javax.faces.webapp;

import java.io.*;
import java.lang.reflect.*;
import java.util.logging.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.lifecycle.*;

import javax.servlet.*;
import javax.servlet.http.*;

public final class FacesServlet implements Servlet
{
  private static final Logger log
    = Logger.getLogger(FacesServlet.class.getName());
  
  public static final String CONFIG_FILES_ATTR
    = "javax.faces.CONFIG_FILES";
  public static final String LIFECYCLE_ID_ATTR
    = "javax.faces.LIFECYCLE_ID";

  private ServletConfig _config;
  private ServletContext _webApp;

  private FacesContextFactory _facesContextFactory;
  private Lifecycle _lifecycle;
  
  public FacesServlet()
  {
  }

  public ServletConfig getServletConfig()
  {
    return _config;
  }

  public String getServletInfo()
  {
    return "javax.faces.webapp.FacesServlet";
  }

  public void init(ServletConfig config)
    throws ServletException
  {
    _config = config;
    _webApp = config.getServletContext();

    try {
      Class cl = Class.forName("com.caucho.jsf.webapp.FacesServletImpl");

      Servlet servlet = (Servlet) cl.newInstance();

      Method init = cl.getMethod("init", new Class[] { ServletConfig.class });

      init.invoke(servlet, config);
    } catch (ClassNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
	throw (RuntimeException) e.getCause();
      else if (e.getCause() instanceof ServletException)
	throw (ServletException) e.getCause();
      else
	throw new ServletException(e);
    } catch (Exception e) {
      throw new ServletException(e);
    }

    _facesContextFactory = (FacesContextFactory)
      FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);

    LifecycleFactory lifecycleFactory = (LifecycleFactory)
      FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);

    String name = null;

    name = config.getInitParameter("javax.faces.LIFECYCLE_ID");

    if (name == null)
      name = _webApp.getInitParameter("javax.faces.LIFECYCLE_ID");

    if (name == null)
      name = LifecycleFactory.DEFAULT_LIFECYCLE;

    _lifecycle = lifecycleFactory.getLifecycle(name);
  }

  public void service(ServletRequest request,
		      ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    FacesContext oldContext = FacesContext.getCurrentInstance();

    FacesContext context = null;
    
    try {
      context = _facesContextFactory.getFacesContext(_webApp,
						     req,
						     res,
						     _lifecycle);

      _lifecycle.execute(context);
      _lifecycle.render(context);
    } catch (FacesException e) {
      throw new ServletException(e);
    } finally {
      if (context != null)
	context.release();
      
      FacesContext.setCurrentInstance(oldContext);
    }
  }

  public void destroy()
  {
  }
}
