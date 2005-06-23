
package com.caucho.widget;

import java.io.IOException;

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

/**
 */
public interface Widget
{
  public String getId();

  public String getTypeName();

  public boolean isNamespace();

  /**
   * Add a child widget to this widget.
   *
   * @param widget
   * @throws UnsupportedOperationException if the implementing widget cannot contain children
   */
  public void add(Widget widget)
    throws UnsupportedOperationException;

  public void init(WidgetInit init)
    throws WidgetException;

  /**
   * Use parameters to set variables as appropriate for the widget.
   */
  public void invocation(WidgetInvocation invocation)
    throws WidgetException;

  /**
   * Perform any actions indicated by the request.
   */
  public void request(WidgetRequest request)
    throws WidgetException;

  /**
   * Set parameters from variables as appropriate for the widget,
   * so that they are maintained on a subsequent request.
   *
   * This method is called only if another widget is creating a url,
   * if this widget is creating the url then this method is not called.
   */
  public void url(WidgetURL url)
    throws WidgetException;

  /**
   * Render a response to the client.
   */
  public void response(WidgetResponse response)
    throws WidgetException, IOException;

  /**
   * Destroy an instance that is being taken out of service
   */
  public void destroy(WidgetDestroy destroy);

}
