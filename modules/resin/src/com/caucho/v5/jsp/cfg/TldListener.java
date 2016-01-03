/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.jsp.cfg;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.http.webapp.ListenerConfig;
import com.caucho.v5.http.webapp.WebAppResin;

/**
 * Configuration for the taglib listener in the .tld
 */
public class TldListener {
  private Class<?> _listenerClass;

  public void setId(String id)
  {
  }

  public void setDescription(String desc)
  {
  }

  public void setDisplayName(String displayName)
  {
  }

  public void setIcon(com.caucho.v5.config.types.Icon icon)
  {
  }

  /**
   * Sets the listener class.
   */
  public void setListenerClass(Class<?> listenerClass)
  {
    _listenerClass = listenerClass;
  }

  /**
   * Gets the listener class.
   */
  public Class<?> getListenerClass()
  {
    return _listenerClass;
  }

  /**
   * Registers with the web-app.
   */
  public void register(WebAppResin webApp)
  {
    if (webApp == null)
      return;

    if (webApp.hasListener(_listenerClass)) {
      return;
    }

    String className = _listenerClass.getName();

    if ("com.sun.faces.config.ConfigureListener".equals(className)
        && ! webApp.isFacesServletConfigured()) {
      // avoid initializing JSF if it's not used.
      return;
    }
    
    try {
      ListenerConfig listener = new ListenerConfig();
      listener.setListenerClass(_listenerClass);
      //listener.init();
      webApp.addListener(listener);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _listenerClass + "]";
  }
}
