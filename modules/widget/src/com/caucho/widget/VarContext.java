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

public interface VarContext
{
  /**
   * Set a variable.  The value of the variable will be available at least until
   * the current connection completes.  If the variable has been defined as
   * `persistent', then the value will be available for all subsequent
   * connections from the same user.
   *
   * @throws UnsupportedOperationException if the variable has been defined as
   * read-only.
   *
   * @see VarDefinition#setReadOnly(boolean)
   */
  public void setVar(Widget widget, VarDefinition varDefinition, Object value)
    throws UnsupportedOperationException;

  public void setVar(Widget widget, String name, Object value)
    throws UnsupportedOperationException;

  /**
   * Get the value of a variable.  If children of the widget defines a variable
   * with the same name as `inherited' then this value will also apply to
   * children that do not set their own value.
   *
   * @see VarDefinition#setInherited(boolean)
   */
  public <T> T getVar(Widget widget, VarDefinition varDefinition);

  public <T> T getVar(Widget widget, String name);

  /**
   * Get the value of a variable, and return the specified deflt if the
   * value of the var has not been set.
   */
  public <T> T getVar(Widget widget, VarDefinition varDefinition, T deflt);

  public <T> T getVar(Widget widget, String name, T deflt);

  /**
   * Remove the value of the variable for the widget, allowing parent values
   * or the default to come through.
   */
  public void removeVar(Widget widget, String name);

  public void removeVar(Widget widget, VarDefinition varDefinition);

}
