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

package com.caucho.jsf.lifecycle;

import com.caucho.util.*;

import java.util.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.lifecycle.*;
import javax.faces.render.*;

import com.caucho.jsf.application.*;

/**
 * The default lifecycle implementation
 */
public class LifecycleImpl extends Lifecycle
{
  private static final L10N L = new L10N(LifecycleImpl.class);
  
  private ArrayList<PhaseListener> _phaseList = new ArrayList<PhaseListener>();
  private PhaseListener []_phaseListeners = new PhaseListener[0];
  
  public LifecycleImpl()
  {
  }

  public void addPhaseListener(PhaseListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    synchronized (_phaseList) {
      _phaseList.add(listener);
      _phaseListeners = new PhaseListener[_phaseList.size()];
      _phaseList.toArray(_phaseListeners);
    }
  }
  
  public PhaseListener []getPhaseListeners()
  {
    return _phaseListeners;
  }
  
  public void removePhaseListener(PhaseListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    synchronized (_phaseList) {
      _phaseList.remove(listener);
      _phaseListeners = new PhaseListener[_phaseList.size()];
      _phaseList.toArray(_phaseListeners);
    }
  }

  public void execute(FacesContext context)
    throws FacesException
  {
    if (context.getResponseComplete() || context.getRenderResponse())
      return;

    restoreView(context);
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;

    UIViewRoot viewRoot = context.getViewRoot();

    viewRoot.processDecodes(context);
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
    
    viewRoot.processValidators(context);
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
    
    viewRoot.processUpdates(context);
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
    
    viewRoot.processApplication(context);
  }

  private void restoreView(FacesContext context)
    throws FacesException
  {
    Application app = context.getApplication();

    if (app instanceof ApplicationImpl)
      ((ApplicationImpl) app).initRequest();
    
    ViewHandler view = app.getViewHandler();

    view.initView(context);

    UIViewRoot viewRoot = context.getViewRoot();
    System.out.println("ROOT: " + viewRoot);
    
    if (viewRoot != null) {
      ExternalContext extContext = context.getExternalContext();
      
      viewRoot.setLocale(extContext.getRequestLocale());

      // XXX: binding

      return;
    }

    String viewId = calculateViewId(context);

    String renderKitId = view.calculateRenderKitId(context);

    RenderKitFactory renderKitFactory
      = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
    
    RenderKit renderKit = renderKitFactory.getRenderKit(context, renderKitId);

    ResponseStateManager stateManager = renderKit.getResponseStateManager();

    if (stateManager.isPostback(context)) {
      viewRoot = view.restoreView(context,  viewId);

      if (viewRoot == null) {
	context.renderResponse();
      
	viewRoot = view.createView(context, viewId);

	context.setViewRoot(viewRoot);
	
	throw new ViewExpiredException(L.l("{0} is an expired view",
					 viewId));
      }
      
      context.setViewRoot(viewRoot);
    }
    else {
      context.renderResponse();
      
      viewRoot = view.createView(context, viewId);

      context.setViewRoot(viewRoot);
    }
  }

  private String calculateViewId(FacesContext context)
  {
    ExternalContext extContext = context.getExternalContext();

    String servletPath = extContext.getRequestServletPath();
    String pathInfo = extContext.getRequestPathInfo();

    String path;
    int dot;

    if (servletPath != null
	&& (dot = servletPath.lastIndexOf('.')) > 0
	&& servletPath.lastIndexOf('/') < dot) {
      // /test/foo.jsp

      return servletPath.substring(0, dot) + ".jsp";
    }
    else if (pathInfo != null) {
      dot = pathInfo.lastIndexOf('.');

      if (dot > 0)
	return pathInfo.substring(0, dot) + ".jsp";
      else
	return pathInfo + ".jsp";
    }
    else
      throw new FacesException("no view-id found");
  }
  
  public void render(FacesContext context)
    throws FacesException
  {
    if (context.getResponseComplete())
      return;

    Application app = context.getApplication();
    ViewHandler view = app.getViewHandler();

    try {
      view.renderView(context, context.getViewRoot());

      view.writeState(context); // XXX:
    } catch (java.io.IOException e) {
      throw new FacesException(e);
    }
  }

  public String toString()
  {
    return "DefaultLifecycleImpl[]";
  }
}
