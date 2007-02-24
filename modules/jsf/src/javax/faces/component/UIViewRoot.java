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

package javax.faces.component;

import java.util.*;

import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.event.*;

public class UIViewRoot extends UIComponentBase
{
  public static final String COMPONENT_FAMILY = "javax.faces.ViewRoot";
  public static final String COMPONENT_TYPE = "javax.faces.ViewRoot";
  public static final String UNIQUE_ID_PREFIX = "_id";

  private String _renderKitId = "HTML_BASIC";

  private String _viewId;
  private int _unique;

  private Locale _locale;

  private ArrayList<PhaseListener> _phaseListeners
    = new  ArrayList<PhaseListener>();
  
  private ArrayList<FacesEvent> _eventList;

  public UIViewRoot()
  {
  }

  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }
  
  public String getRenderKitId()
  {
    return _renderKitId;
  }
  
  public void setRenderKitId(String renderKitId)
  {
    _renderKitId = renderKitId;
  }
  
  public String getViewId()
  {
    return _viewId;
  }
  
  public void setViewId(String value)
  {
    _viewId = value;
  }

  public void setLocale(Locale locale)
  {
    _locale = locale;
  }

  public Locale getLocale()
  {
    return _locale;
  }

  public void addPhaseListener(PhaseListener listener)
  {
    _phaseListeners.add(listener);
  }

  public void removePhaseListener(PhaseListener listener)
  {
    _phaseListeners.remove(listener);
  }

  public String createUniqueId()
  {
    return UNIQUE_ID_PREFIX + _unique++;
  }

  /**
   * Process the application.
   */
  public void processApplication(FacesContext context)
  {
    if (context == null)
      throw new NullPointerException();

    broadcastEvents(PhaseId.INVOKE_APPLICATION);
  }

  /**
   * Process the decodes.
   */
  public void processDecodes(FacesContext context)
  {
    super.processDecodes(context);

    broadcastEvents(PhaseId.APPLY_REQUEST_VALUES);
  }

  /**
   * Process the updates.
   */
  public void processUpdates(FacesContext context)
  {
    super.processUpdates(context);

    broadcastEvents(PhaseId.UPDATE_MODEL_VALUES);
  }

  /**
   * Process the validators.
   */
  @Override
  public void processValidators(FacesContext context)
  {
    super.processValidators(context);

    broadcastEvents(PhaseId.PROCESS_VALIDATIONS);
  }

  @Override
  public void queueEvent(FacesEvent event)
  {
    if (_eventList == null)
      _eventList = new ArrayList<FacesEvent>();

    _eventList.add(event);
  }

  private void broadcastEvents(PhaseId phaseId)
  {
    if (_eventList != null) {
      for (int i = 0; i < _eventList.size(); i++) {
	FacesEvent event = _eventList.get(i);

	if (phaseId.equals(event.getPhaseId())) {
	  event.getComponent().broadcast(event);
	  _eventList.remove(i);
	  i--;
	}
      }
    }
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    return new Object[] {
      super.saveState(context),
      _viewId,
      _renderKitId,
      _locale
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _viewId = (String) state[1];
    _renderKitId = (String) state[2];
    _locale = (Locale) state[3];
  }
  
  public String toString()
  {
    return getClass().getName() + "[" + getViewId() + "]";
  }
}
