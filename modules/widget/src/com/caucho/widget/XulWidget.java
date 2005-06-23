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

abstract public class XulWidget
  extends AbstractWidgetContainer
{
  private StringProperty _cssClassProperty = new StringProperty(this, "cssClass", false, getTypeName());
  private TransientProperty _transientProperty = new TransientProperty(this);

  /**
   * A Widget is immutable if it does not have an id, because it cannot
   * store changes to it's properties as request parameters.
   */
  protected boolean isImmutable()
  {
    return getId() == null;
  }

  public void setCssClass(String cssClass)
  {
    _cssClassProperty.setValue(cssClass);
  }

  public String getCssClass()
  {
    return _cssClassProperty.getValue();
  }

  protected boolean isContainer()
  {
    return false;
  }

  public void setTransient(boolean isTransient)
  {
    _transientProperty.setValue(isTransient);
  }

  public boolean isTransient()
  {
    return _transientProperty.getValue();
  }

  public void add(Widget child)
  {
    if (!isContainer())
      throw new UnsupportedOperationException();

    super.add(child);
  }

  public void init(WidgetInit init)
    throws WidgetException
  {
    super.init(init);

    _transientProperty.init(init);
    _cssClassProperty.init(init);
  }

  public void setCssClass(VarContext context, String cssClass)
  {
    _cssClassProperty.setValue(context, cssClass);
  }

  public String getCssClass(VarContext context)
  {
    return _cssClassProperty.getValue(context);
  }

  public void setTransient(VarContext context, boolean isTransient)
  {
    _transientProperty.setValue(context, isTransient);
  }

  public boolean isTransient(VarContext context)
  {
    return _transientProperty.getValue(context);
  }
}
