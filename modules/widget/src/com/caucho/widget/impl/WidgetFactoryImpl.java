/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.widget.impl;

import com.caucho.widget.*;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.config.Config;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.net.URL;
import java.util.Map;

public class WidgetFactoryImpl
  extends WidgetFactory
{
  private WidgetLibraries _widgetLibraries;

  public WidgetFactoryImpl()
  {
    super(true);

    _widgetLibraries = new WidgetLibraries();
    _widgetLibraries.add(new WidgetLibraryStandard());
  }

  public WidgetContext createContext(ExternalContext externalContext,
                                     Widget widget)
    throws WidgetException
  {
    WidgetContextImpl context = new WidgetContextImpl();
    context.setExternalContext(externalContext);
    context.setWidget(widget);
    context.init();

    return context;
  }

  public WidgetContext createContext(ServletContext servletContext,
                                     Widget widget)
    throws ServletException
  {
    ExternalContext externalContext = new ServletExternalContext(servletContext);

    WidgetContextImpl context = null;

    try {
      context = new WidgetContextImpl();
      context.setExternalContext(externalContext);
      context.setWidget(widget);
      context.setServletContext(servletContext);
      context.init();
    }
    catch (WidgetException e) {
      throw new ServletException(e);
    }

    return context;
  }

  public Widget createFromXml(URL url)
    throws WidgetException
  {
    return createFromXml(url, null);
  }

  // XXX: get rid of this, var should be passed to context and render
  public Widget createFromXml(URL url, Map<String, Object> varMap)
    throws WidgetException
  {
    WidgetConfig widgetConfig = new WidgetConfig();

    if (varMap != null)
      widgetConfig.setVarMap(varMap);

    Path path = Vfs.lookup(url.toString());

    try {
      Config config = new Config();
      config.configure(widgetConfig, path);
    }
    catch (Exception e) {
      throw new WidgetException(e);
    }

    return widgetConfig;
  }

  public Class<? extends Widget> getWidgetClass(String namespaceURI, String localName)
  {
    return _widgetLibraries.getWidgetClass(namespaceURI, localName);
  }
}
