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

package com.caucho.jsf.application;

import java.io.*;
import java.util.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.render.*;

import javax.servlet.*;

public class JspViewHandler extends ViewHandler
{
  @Override
  public Locale calculateLocale(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();
    
    return context.getApplication().getDefaultLocale();
  }

  @Override
  public String calculateCharacterEncoding(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();
    
    return "utf-8";
  }

  @Override
  public String calculateRenderKitId(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();
    
    return RenderKitFactory.HTML_BASIC_RENDER_KIT;
  }

  public UIViewRoot createView(FacesContext context,
			       String viewId)
  {
    if (context == null)
      throw new NullPointerException();
    
    if (viewId == null) {
      ExternalContext extContext = context.getExternalContext();

      String servletPath = extContext.getRequestServletPath();
      String pathInfo = extContext.getRequestPathInfo();

      String path;
      int dot;

      if (servletPath != null
	  && (dot = servletPath.lastIndexOf('.')) > 0
	  && servletPath.lastIndexOf('/') < dot) {
	// /test/foo.jsp

	viewId = servletPath.substring(0, dot) + ".jsp";
      }
      else if (pathInfo != null) {
	dot = pathInfo.lastIndexOf('.');

	if (dot > 0)
	  viewId = pathInfo.substring(0, dot) + ".jsp";
	else
	  viewId = pathInfo + ".jsp";
      }
      else
	viewId = "";
    }
    
    UIViewRoot viewRoot = new UIViewRoot();

    viewRoot.setViewId(viewId);
    viewRoot.setRenderKitId(calculateRenderKitId(context));

    return viewRoot;
  }

  public String getActionURL(FacesContext context,
			     String viewId)
  {
    if (context == null || viewId == null)
      throw new NullPointerException();

    return viewId;
  }

  public String getResourceURL(FacesContext context,
			       String path)
  {
    return path;
  }

  public void renderView(FacesContext context,
			 UIViewRoot viewToRender)
    throws IOException, FacesException
  {
    String viewId = viewToRender.getViewId();

    System.out.println("VIEW-ID: " + viewId);
    ExternalContext extContext = context.getExternalContext();

    ((javax.servlet.http.HttpServletResponse) extContext.getResponse()).setContentType("text/html");

    extContext.dispatch(viewId);

    /*
    UIViewRoot viewRoot = context.getViewRoot();

    if (viewRoot != null) {
      viewRoot.setRendered(true); // XXX:
      
      viewRoot.encodeAll(context);
    }
    */
  }

  public UIViewRoot restoreView(FacesContext context,
				String viewId)
    throws FacesException
  {
    return null;
  }

  public void writeState(FacesContext context)
    throws IOException
  {
  }

  public String toString()
  {
    return "JspViewHandler[]";
  }
}
