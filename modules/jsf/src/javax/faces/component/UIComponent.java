/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.el.*;

import javax.faces.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.render.*;

public abstract class UIComponent
  implements StateHolder, SystemEventListenerHolder
{
  private static final Logger log
    = Logger.getLogger(UIComponent.class.getName());

  private Map<Class<? extends SystemEvent>, ComponentSystemEventListener []> _componentEventListenerMap;

  public abstract Map<String,Object> getAttributes();

  @Deprecated
  public abstract ValueBinding getValueBinding(String name);

  @Deprecated
  public abstract void setValueBinding(String name, ValueBinding binding);

  /**
   * @Since 1.2
   */
  public javax.el.ValueExpression getValueExpression(String name)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @Since 1.2
   */
  public void setValueExpression(String name, ValueExpression binding)
  {
    throw new UnsupportedOperationException();
  }

  public abstract String getClientId(FacesContext context);

  /**
   * @Since 1.2
   */
  public String getContainerClientId(FacesContext context)
  {
    return getClientId(context);
  }

  public abstract String getFamily();

  public abstract String getId();

  public abstract void setId(String id);

  public abstract UIComponent getParent();

  public abstract void setParent(UIComponent parent);

  public abstract boolean isRendered();

  public abstract void setRendered(boolean rendered);

  public abstract String getRendererType();

  public abstract void setRendererType(String rendererType);

  public abstract boolean getRendersChildren();

  public abstract List<UIComponent> getChildren();

  public abstract int getChildCount();

  public abstract UIComponent findComponent(String expr);

  /**
   * @Since 1.2
   */
  public boolean invokeOnComponent(FacesContext context,
				   String clientId,
				   ContextCallback callback)
    throws FacesException
  {
    if (context == null || clientId == null || callback == null)
      throw new NullPointerException();

    if (clientId.equals(getClientId(context))) {
      try {
        callback.invokeContextCallback(context, this);

        return true;
      }
      catch (Exception e) {
        throw new FacesException(e);
      }
    }
    else {
      Iterator<UIComponent> iter = getFacetsAndChildren();

      while (iter.hasNext()) {
	UIComponent comp = iter.next();

	boolean result = comp.invokeOnComponent(context, clientId, callback);
	if (result)
	  return true;
      }

      return false;
    }
  }

  public abstract Map<String,UIComponent> getFacets();

  /**
   * @Since 1.2
   */
  public int getFacetCount()
  {
    Map<String,UIComponent> map = getFacets();

    if (map == null)
      return 0;
    else
      return getFacets().size();
  }

  public abstract UIComponent getFacet(String name);

  public abstract Iterator<UIComponent> getFacetsAndChildren();

  public abstract void broadcast(FacesEvent event)
    throws AbortProcessingException;

  public abstract void decode(FacesContext context);

  public abstract void encodeBegin(FacesContext context)
    throws IOException;

  public abstract void encodeChildren(FacesContext context)
    throws IOException;

  public abstract void encodeEnd(FacesContext context)
    throws IOException;

  /**
   * @Since 1.2
   */
  
  /**
   * Encodes all children
   */
  public void encodeAll(FacesContext context)
    throws IOException
  {
    if (context == null)
      throw new NullPointerException();

    if (! isRendered()) {
      return;
    }
    
    encodeBegin(context);

    if (getRendersChildren()) {
      encodeChildren(context);
    }
    else {
      int childCount = getChildCount();

      if (childCount > 0) {
	List<UIComponent> children = getChildren();

	for (int i = 0; i < childCount; i++) {
	  UIComponent child = children.get(i);

	  child.encodeAll(context);
	}
      }
    }
    
    encodeEnd(context);
  }

  protected abstract void addFacesListener(FacesListener listener);

  protected abstract FacesListener []getFacesListeners(Class cl);

  protected abstract void removeFacesListener(FacesListener listener);

  public abstract void queueEvent(FacesEvent event);

  public abstract void processRestoreState(FacesContext context,
					   Object state);

  public abstract void processDecodes(FacesContext context);

  public abstract void processValidators(FacesContext context);

  public abstract void processUpdates(FacesContext context);

  public abstract Object processSaveState(FacesContext context);

  protected abstract FacesContext getFacesContext();

  protected abstract Renderer getRenderer(FacesContext context);

  /**
   * @since 2.0
   */
  public void subscribeToEvent(FacesContext context,
                               Class<? extends SystemEvent> eventClass,
                               ComponentSystemEventListener componentListener)
  {

    if (context == null)
      throw new NullPointerException();

    if (eventClass == null)
      throw new NullPointerException();

    if (componentListener == null)
      throw new NullPointerException();

    if (_componentEventListenerMap == null)
      _componentEventListenerMap
        = new HashMap<Class<? extends SystemEvent>, ComponentSystemEventListener[]>();

    ComponentSystemEventListener []listeners = _componentEventListenerMap.get(
      eventClass);

    if (listeners == null) {
      listeners = new ComponentSystemEventListener[]{componentListener};
    }
    else {
      ComponentSystemEventListener []temp
        = new ComponentSystemEventListener [listeners.length + 1];

      System.arraycopy(listeners, 0, temp, 0, listeners.length);

      temp [temp.length - 1] = componentListener;

      listeners = temp;
    }

    _componentEventListenerMap.put(eventClass, listeners);
  }

  public void unsubscribeFromEvent(FacesContext context,
                                 Class<? extends SystemEvent> facesEventClass,
                                 ComponentSystemEventListener listener) {
    if (context == null)
      throw new NullPointerException();

    if (facesEventClass == null)
      throw new NullPointerException();

    if (listener == null)
      throw new NullPointerException();

    if (_componentEventListenerMap == null)
      return;

    ComponentSystemEventListener [] listeners
      = _componentEventListenerMap.get(facesEventClass);

    for (int i = 0; i < listeners.length; i++) {
      if (listener.equals(listeners[i])) {
        if (listeners.length == 1) {
          _componentEventListenerMap.remove(facesEventClass);
        }
        else
        {
          ComponentSystemEventListener []temp = new ComponentSystemEventListener [listeners.length - 1];

          System.arraycopy(listeners, 0, temp, 0, i);

          System.arraycopy(listeners, i + 1, temp, i, listeners.length - (i + 1));

          _componentEventListenerMap.put(facesEventClass, temp);
        }
        break;
      }
    }
  }

  public List<SystemEventListener> getListenersForEventClass(Class<? extends SystemEvent> facesEventClass)
  {
    if (facesEventClass == null)
      throw new NullPointerException();

    if (_componentEventListenerMap == null)
      return new ArrayList<SystemEventListener>(0);

    ComponentSystemEventListener [] listeners
      = _componentEventListenerMap.get(facesEventClass);

    if (listeners == null)
      return new ArrayList<SystemEventListener>(0);

    ArrayList<SystemEventListener> result
      = new ArrayList<SystemEventListener>(listeners.length);

    for (ComponentSystemEventListener listener : listeners) {
      SystemEventListener systemEventListener
        = new SystemEventListenerAdapter(listener, this.getClass());

      result.add(systemEventListener);
    }

    return result;
  }

  /**
   * @since 2.0
   */
  protected void pushComponentToEL(FacesContext context)
  {

    try {
      Map<Object, Object> attributes = context.getAttributes();

      UIComponent []components
        = (UIComponent[]) attributes.get("caucho.jsf.component.stack");

      if (components == null) {
        components = new UIComponent[]{this};
      }
      else {
        UIComponent []temp = new UIComponent[components.length + 1];

        System.arraycopy(components, 0, temp, 0, components.length);

        temp[temp.length - 1] = this;

        components = temp;
      }

      attributes.put("caucho.jsf.component.stack", components);

      attributes.put("component", this);

    }
    catch (UnsupportedOperationException e) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, e.getMessage(), e);
    }
  }

  /**
   * @since 2.0
   */
  protected void popComponentFromEL(FacesContext context)
  {
    try {
      Map<Object, Object> attributes = context.getAttributes();
      UIComponent[] components
        = (UIComponent[]) attributes.get("caucho.jsf.component.stack");

      if (components == null ||
          components.length == 0 ||
          components[components.length - 1] != this) {
        log.fine("UIComponent.popComponent expected to find self '" +
                 this +
                 "' on stack");
      }
      else {
        UIComponent[] temp = new UIComponent[components.length - 1];

        System.arraycopy(components, 0, temp, 0, temp.length);

        attributes.put("caucho.jsf.component.stack", temp);

        if (temp.length > 0)
          attributes.put("component", temp[temp.length - 1]);
        else
          attributes.remove("component");
      }

    }
    catch (UnsupportedOperationException e) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, e.getMessage(), e);
    }
  }

  /**
   * @since 2.0
   */
  public static UIComponent getCurrentComponent()
  {
    FacesContext context = FacesContext.getCurrentInstance();

    try {
      Map<Object, Object> attributes = context.getAttributes();

       return (UIComponent) attributes.get("component");
    }
    catch (UnsupportedOperationException e) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, e.getMessage(), e);
    }

    return null;
  }

}

class SystemEventListenerAdapter implements SystemEventListener {
  private ComponentSystemEventListener _listener;
  private Class _sourceClass;

  SystemEventListenerAdapter(ComponentSystemEventListener listener,
                             Class sourceClass)
  {
    _listener = listener;
    _sourceClass = sourceClass;
  }

  public boolean isListenerForSource(Object source)
  {
    return _sourceClass.isAssignableFrom(source.getClass());
  }

  public void processEvent(SystemEvent event)
    throws AbortProcessingException
  {
    _listener.processEvent((ComponentSystemEvent) event);
  }
}