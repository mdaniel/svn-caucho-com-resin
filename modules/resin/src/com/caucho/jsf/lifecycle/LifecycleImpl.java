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
import java.util.logging.*;

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
  private static final Logger log
    = Logger.getLogger(LifecycleImpl.class.getName());
  
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

    beforePhase(context, PhaseId.RESTORE_VIEW);

    try {
      restoreView(context);
    } finally {
      afterPhase(context, PhaseId.RESTORE_VIEW);
    }

    if (context.getResponseComplete() || context.getRenderResponse())
      return;

    UIViewRoot viewRoot = context.getViewRoot();

    beforePhase(context, PhaseId.APPLY_REQUEST_VALUES);
    
    try {
      viewRoot.processDecodes(context);
    } finally {
      afterPhase(context, PhaseId.APPLY_REQUEST_VALUES);
    }

    //
    // Process Validations (processValidators)
    //
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
    
    beforePhase(context, PhaseId.PROCESS_VALIDATIONS);

    try {
      viewRoot.processValidators(context);
    } finally {
      afterPhase(context, PhaseId.PROCESS_VALIDATIONS);
    }

    //
    // Update Model Values (processUpdates)
    //
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
    
    beforePhase(context, PhaseId.UPDATE_MODEL_VALUES);

    try {
      viewRoot.processUpdates(context);
    } finally {
      afterPhase(context, PhaseId.UPDATE_MODEL_VALUES);
    }

    //
    // Invoke Application (processApplication)
    //
    
    if (context.getResponseComplete() || context.getRenderResponse())
      return;
    
    beforePhase(context, PhaseId.INVOKE_APPLICATION);

    try {
      viewRoot.processApplication(context);
    } finally {
      afterPhase(context, PhaseId.INVOKE_APPLICATION);
    }
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
	
	log.info(L.l("{0} is an expired view", viewId));
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
    return JspViewHandler.createViewId(context);
  }
  
  public void render(FacesContext context)
    throws FacesException
  {
    if (context.getResponseComplete())
      return;

    beforePhase(context, PhaseId.RENDER_RESPONSE);
    
    Application app = context.getApplication();
    ViewHandler view = app.getViewHandler();

    try {
      view.renderView(context, context.getViewRoot());
    } catch (java.io.IOException e) {
      throw new FacesException(e);
    }
    
    afterPhase(context, PhaseId.RENDER_RESPONSE);
  }

  private void beforePhase(FacesContext context, PhaseId phase)
  {
    for (int i = 0; i < _phaseListeners.length; i++) {
      PhaseListener listener = _phaseListeners[i];
      PhaseId id = listener.getPhaseId();

      if (phase == id || id == PhaseId.ANY_PHASE) {
	PhaseEvent event = new PhaseEvent(context, phase, this);

	listener.beforePhase(event);
      }
    }
  }

  private void afterPhase(FacesContext context, PhaseId phase)
  {
    for (int i = _phaseListeners.length - 1; i >= 0; i--) {
      PhaseListener listener = _phaseListeners[i];
      PhaseId id = listener.getPhaseId();

      if (phase == id || id == PhaseId.ANY_PHASE) {
	PhaseEvent event = new PhaseEvent(context, phase, this);
	
	listener.afterPhase(event);
      }
    }
  }

  public String toString()
  {
    return "DefaultLifecycleImpl[]";
  }
}
