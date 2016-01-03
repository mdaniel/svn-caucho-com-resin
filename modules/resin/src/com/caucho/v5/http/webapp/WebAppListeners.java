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

package com.caucho.v5.http.webapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.http.session.SessionManager;
import com.caucho.v5.util.ConcurrentArrayList;
import com.caucho.v5.util.L10N;

/**
 * servlet/filter listeners.
 */
class WebAppListeners
{
  private static final L10N L = new L10N(WebAppListeners.class);
  private static final Logger log
    = Logger.getLogger(WebAppListeners.class.getName());
  
  private final WebAppBuilder _builder;

  // List of all the listeners.
  private ArrayList<ListenerConfig<?>> _listeners
    = new ArrayList<ListenerConfig<?>>();

  //Listeners added by ServletContainerInitializers
  private ArrayList<ServletContextListener> _containerListeners
    = new ArrayList<>();

  // List of the ServletContextListeners from the configuration file
  private ArrayList<ServletContextListener> _webAppListeners
    = new ArrayList<>();

  // List of the ServletContextAttributeListeners from the configuration file
  private ArrayList<ServletContextAttributeListener> _attributeListeners
    = new ArrayList<>();

  // List of the ServletRequestListeners from the configuration file
  private ConcurrentArrayList<ServletRequestListener> _requestListeners
    = new ConcurrentArrayList<>(ServletRequestListener.class);

  // List of the ServletRequestAttributeListeners from the configuration file
  private ConcurrentArrayList<ServletRequestAttributeListener> _requestAttributeListeners
    = new ConcurrentArrayList<>(ServletRequestAttributeListener.class);

  private boolean _isStart;

  WebAppListeners(WebAppBuilder builder)
  {
    Objects.requireNonNull(builder);
    
    _builder = builder;
  }
  
  private WebApp getWebApp()
  {
    return _builder.getWebApp();
  }
  
  private SessionManager getSessionManager()
  {
    return getWebApp().getSessionManager();
  }

  public void start()
  {
    _isStart = true;
    
    ServletContextEvent event = new ServletContextEvent(getWebApp());

    for (ListenerConfig<?> listener : _listeners) {
      try {
        addListenerObject(listener.createListenerObject(), false);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }

    contextInitialized(_containerListeners, event, true);

    for (int i = 0; i < _webAppListeners.size(); i++) {
      ServletContextListener listener = _webAppListeners.get(i);

      listener.contextInitialized(event);
    }
  }

  void contextInitialized(List<ServletContextListener> listeners,
                          ServletContextEvent event,
                          boolean isContextConfigUnsuppored)
  {
    WebApp webApp = getWebApp();
    webApp.setContextConfigUnsuppored(isContextConfigUnsuppored);

    for (int i = 0; i < listeners.size(); i++) {
      ServletContextListener listener = listeners.get(i);

      listener.contextInitialized(event);
    }

    webApp.setContextConfigUnsuppored(false);
  }

  /**
   * Returns true if a listener with the given type exists.
   */
  public boolean hasListener(Class<?> listenerClass)
  {
    for (int i = 0; i < _listeners.size(); i++) {
      ListenerConfig<?> listener = _listeners.get(i);

      if (listenerClass.equals(listener.getListenerClass())) {
        return true;
      }
    }

    return false;
  }

  /**
   * listener configuration
   */
  public void addListener(ListenerConfig<?> listener)
    throws Exception
  {
    if (! hasListener(listener.getListenerClass())) {
      _listeners.add(listener);

      // jsp/18n, server/12t7
      //if (getWebApp().isStarting() || getWebApp().isActive()) {
      if (_isStart) {
        addListenerObject(listener.createListenerObject(), true);
      }
    }
  }

  /**
   * Adds the listener object.
   */
  void addListenerObject(Object listenerObj, boolean isStart)
  {
    boolean isValid = false;
    
    if (listenerObj instanceof ServletContextListener) {
      ServletContextListener scListener = (ServletContextListener) listenerObj;

      if (getWebApp().isContainerInit()) {
        _containerListeners.add(scListener);
      } else {
        _webAppListeners.add(scListener);

        if (isStart) {
          ServletContextEvent event = new ServletContextEvent(getWebApp());
          scListener.contextInitialized(event);
        }
      }

      isValid = true;
    }

    if (listenerObj instanceof ServletContextAttributeListener) {
      _attributeListeners.add((ServletContextAttributeListener) listenerObj);
      
      isValid = true;
    }

    if (listenerObj instanceof ServletRequestListener) {
      _requestListeners.add((ServletRequestListener) listenerObj);
      
      isValid = true;
    }

    if (listenerObj instanceof ServletRequestAttributeListener) {
      _requestAttributeListeners.add((ServletRequestAttributeListener) listenerObj);
      
      isValid = true;
    }

    if (listenerObj instanceof HttpSessionListener) {
      getSessionManager().addListener((HttpSessionListener) listenerObj);
      
      isValid = true;
    }

    if (listenerObj instanceof HttpSessionAttributeListener) {
      getSessionManager().addAttributeListener((HttpSessionAttributeListener) listenerObj);
      
      isValid = true;
    }

    if (listenerObj instanceof HttpSessionIdListener) {
      getSessionManager().addSessionIdListener((HttpSessionIdListener) listenerObj);
      isValid = true;
    }

    if (listenerObj instanceof HttpSessionActivationListener) {
      getSessionManager().addActivationListener((HttpSessionActivationListener) listenerObj);
      
      isValid = true;
    }
    
    if (! isValid) {
      throw new IllegalArgumentException(L.l("{0} is an unknown listener.",
                                             listenerObj));
    }
  }
  
  boolean isListenerClass(Class<?> cl)
  {
    if (ServletContextListener.class.isAssignableFrom(cl)) {
      return true;
    }
    
    if (ServletContextAttributeListener.class.isAssignableFrom(cl)) {
      return true;
    }
    
    if (ServletRequestListener.class.isAssignableFrom(cl)) {
      return true;
    }
    
    if (ServletRequestAttributeListener.class.isAssignableFrom(cl)) {
      return true;
    }
    
    if (HttpSessionListener.class.isAssignableFrom(cl)) {
      return true;
    }
    
    if (HttpSessionAttributeListener.class.isAssignableFrom(cl)) {
      return true;
    }
    
    if (HttpSessionActivationListener.class.isAssignableFrom(cl)) {
      return true;
    }
    
    if (HttpSessionIdListener.class.isAssignableFrom(cl)) {
      return true;
    }
    
    if (AsyncListener.class.isAssignableFrom(cl)) {
      return true;
    }
    
    return false;
  }

  public ServletRequestListener[] getRequestListeners()
  {
    return _requestListeners.toArray();
  }

  public ServletRequestAttributeListener[] getRequestAttributeListeners()
  {
    return _requestAttributeListeners.toArray();
  }

  public void destroy()
  {
    ServletContextEvent event = new ServletContextEvent(getWebApp());

    contextDestroyed(_containerListeners, event);

    // server/10g8 -- webApp listeners after session
    for (int i = _webAppListeners.size() - 1; i >= 0; i--) {
      ServletContextListener listener = _webAppListeners.get(i);

      try {
        listener.contextDestroyed(event);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    // server/10g8 -- webApp listeners after session
    for (int i = _listeners.size() - 1; i >= 0; i--) {
      ListenerConfig<?> listener = _listeners.get(i);

      try {
        listener.destroy();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  private void contextDestroyed(List<ServletContextListener> listeners,
                                ServletContextEvent event)
  {
    for (int i = listeners.size() - 1; i >= 0; i--) {
      ServletContextListener listener = listeners.get(i);

      try {
        listener.contextDestroyed(event);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public void onSetAttributeWebApp(ServletContextImpl webApp,
                                   String name, 
                                   Object oldValue, 
                                   Object value)
  {
    if (_attributeListeners != null) {
      ServletContextAttributeEvent event;

      if (oldValue != null)
        event = new ServletContextAttributeEvent(webApp, name, oldValue);
      else
        event = new ServletContextAttributeEvent(webApp, name, value);
        
      for (int i = 0; i < _attributeListeners.size(); i++) {
        ServletContextAttributeListener listener;

        listener = _attributeListeners.get(i);

        try {
          if (oldValue != null)
            listener.attributeReplaced(event);
          else
            listener.attributeAdded(event);
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  public void onRemoveAttributeWebApp(ServletContextImpl webApp,
                                      String name, 
                                      Object oldValue)
  {
    
    // Call any listeners
    if (_attributeListeners != null) {
      ServletContextAttributeEvent event;

      event = new ServletContextAttributeEvent(webApp, name, oldValue);
        
      for (int i = 0; i < _attributeListeners.size(); i++) {
        ServletContextAttributeListener listener;

        listener = _attributeListeners.get(i);

        try {
          listener.attributeRemoved(event);
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getWebApp().getId() + "]";
  }
}
