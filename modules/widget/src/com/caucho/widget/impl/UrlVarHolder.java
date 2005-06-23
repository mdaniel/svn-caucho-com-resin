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

import com.caucho.util.L10N;

public class UrlVarHolder
  extends VarHolder
{
  private static final L10N L = new L10N(UrlVarHolder.class);

  private static final Object NULL_VALUE = new Object();

  private VarHolder _varHolder;

  /**
   * The underlying VarHolder, values in this VarHolder override.
   *
   * @param varHolder
   */
  public void setVarHolder(VarHolder varHolder)
  {
    _varHolder = varHolder;
    super.setWidgetInit(_varHolder.getWidgetInit());
  }

  public void init()
  {
    super.init();

    if (_varHolder == null)
      throw new IllegalStateException(L.l("`{0}' is required", "var-holder"));
  }

  public void destroy()
  {
    _varHolder = null;

    super.destroy();
  }

  protected Object getVarImpl(String name,
                              boolean isDefaultValue,
                              Object defaultValue)
  {
    Object value = super.getVarImpl(name, true, NULL_VALUE);

    if (value == NULL_VALUE)
       value = _varHolder.getVarImpl(name, isDefaultValue, defaultValue);

    return value;
  }
}

