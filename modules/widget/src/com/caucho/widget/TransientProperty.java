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

/**
 * If transient is true, the widget does not save it's parameters or
 * the parameters of any children, default is false.
 */
public class TransientProperty
{
  static public final VarDefinition TRANSIENT
    = new VarDefinition("com.caucho.widget.transient", Boolean.class)
    {
      {
        setValue(false);
        setAllowNull(false);
      }
    };

  private boolean _isTransient;
  private Widget _widget;

  public TransientProperty(Widget widget)
  {
    _widget = widget;
  }

  public void setValue(boolean isTransient)
  {
    _isTransient = isTransient;
  }

  public boolean getValue()
  {
    return _isTransient;
  }

  public void init(WidgetInit init)
    throws WidgetException
  {
    init.addVarDefinition(TRANSIENT);
    setValue(init, _isTransient);
  }

  public void setValue(VarContext context, boolean isTransient)
  {
    context.setVar(_widget, TRANSIENT, isTransient);
  }

  public boolean getValue(VarContext context)
  {
    Boolean isTransient =  context.getVar(_widget, TRANSIENT);

    return isTransient.booleanValue();
  }
}
