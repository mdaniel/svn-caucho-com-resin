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

package com.caucho.jsf.webapp;

import java.io.*;
import java.lang.reflect.*;
import java.util.logging.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.lifecycle.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.config.*;
import com.caucho.jsf.application.*;
import com.caucho.jsf.cfg.*;
import com.caucho.vfs.*;

public class FacesServletImpl extends GenericServlet
{
  private static final Logger log
    = Logger.getLogger(FacesServletImpl.class.getName());

  private static final String FACES_SCHEMA
    = "com/caucho/jsf/cfg/jsf-12.rnc";

  public FacesServletImpl()
  {
  }

  public void init(ServletConfig config)
    throws ServletException
  {
    Path facesPath = Vfs.lookup("WEB-INF/faces-config.xml");

    if (facesPath.canRead()) {
      try {
	FacesConfig facesConfig = new FacesConfig();
	
	new Config().configure(facesConfig, facesPath, FACES_SCHEMA);

	ApplicationFactory appFactory = (ApplicationFactory)
	  FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
	
	ApplicationImpl app = new ApplicationImpl();
	appFactory.setApplication(app);

	for (ManagedBeanConfig bean : facesConfig.getManagedBeans()) {
	  app.addManagedBean(bean.getName(), bean);
	}
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new ServletException(e);
      }
    }
  }

  public void service(ServletRequest request,
		      ServletResponse response)
    throws IOException, ServletException
  {
    throw new UnsupportedOperationException();
  }

  public void destroy()
  {
  }
}
