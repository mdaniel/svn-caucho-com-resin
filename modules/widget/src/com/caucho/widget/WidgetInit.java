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

package com.caucho.widget;

import java.util.Enumeration;

public interface WidgetInit
  extends VarContext
{
  /**
   * Define a variable for use in the widget, typically called from
   * {@link Widget#init(WidgetInit)}.  Undefined variables can still
   * be set with setVar(), this call gives the widget an opportunity to provide
   * information and define behaviour for the variable.
   */
  public void addVarDefinition(VarDefinition varDefinition);

  void addInterceptor(WidgetInterceptor interceptor);

  /**
   * Get a read-only {@link VarDefinition} for the named variable, or a default
   * if there is no definition (never returns null).
   */
  public VarDefinition getVarDefinition(String varName);

  /**
   * Find a widget within the same namespace.
   */
  public <T> T find(String name);

  /**
   * @see javax.servlet.ServletContext#setAttribute(String,Object)
   */
  public void setApplicationAttribute(String name, Object value);

  /**
   * @see javax.servlet.ServletContext#getAttribute(String)
   */
  public <T> T getApplicationAttribute(String name);

  /**
   * @see javax.servlet.ServletContext#getAttributeNames()
   */
  public Enumeration<String> getApplicationAttributeNames();

  /**
   * @see javax.servlet.ServletContext#removeAttribute(String name)
   */
  public void removeApplicationAttribute(String name);

  public WidgetInitChain getInitChain();

  public void finish();

}
