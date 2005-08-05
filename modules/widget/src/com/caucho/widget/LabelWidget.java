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

import com.caucho.util.L10N;
import com.caucho.vfs.XmlWriter;

import java.io.IOException;

public class LabelWidget
  extends XulWidget
{
  private static final L10N L = new L10N(LabelWidget.class);

  private static final String NULL_VALUE = "";

  private VarDefinition valueDef = new XulVarDefinition(this, "value", String.class);
  private DisabledProperty _disabledProperty  = new DisabledProperty(this);

  public LabelWidget()
  {
  }

  public LabelWidget(String id)
  {
    setId(id);
  }

  public LabelWidget(String id, String initialValue)
  {
    setId(id);
    setValue(initialValue);
  }

  public void setValue(String value)
  {
    valueDef.setValue(value);
  }

  public String getValue()
  {
    return valueDef.getValue();
  }

  public void setDisabled(boolean isDisabled)
  {
    _disabledProperty.setValue(isDisabled);
  }

  public boolean isDisabled()
  {
    return _disabledProperty.getValue();
  }

  public void init(WidgetInit init)
    throws WidgetException
  {
    super.init(init);

    _disabledProperty.init(init);

    if (valueDef.getValue() == null)
      valueDef.setValue(NULL_VALUE);

    init.addVarDefinition(valueDef);

  }

  /**
   * A null value becomes the empty string.
   */
  public void setValue(VarContext context, String value)
  {
    if (value == null)
      value = NULL_VALUE;

    context.setVar(this, valueDef, value);
  }

  /**
   * A null value is never returned, LabelWidget treats null as an empty
   * string.
   */
  public String getValue(VarContext context)
  {
    return context.getVar(this, valueDef);
  }

  public void setDisabled(VarContext context, boolean isDisabled)
  {
    _disabledProperty.setValue(context, isDisabled);
  }

  public boolean isDisabled(VarContext context)
  {
    return _disabledProperty.getValue(context);
  }

  public void invocation(WidgetInvocation invocation)
    throws WidgetException
  {
    super.invocation(invocation);

    if (!valueDef.isReadOnly()) {
      String value = invocation.getParameter();

      if (value != null)
        setValue(invocation, value);
    }
  }

  public void url(WidgetURL url)
    throws WidgetException
  {
    if (isTransient(url))
      return;

    super.url(url);

    if (!valueDef.isReadOnly()) {
      String value = getValue(url);

      if (value == null)
        value = NULL_VALUE;

      if (!value.equals(valueDef.getValue()))
        url.setParameter(value);
    }
  }

  public void response(WidgetResponse response)
    throws WidgetException, IOException
  {
    String value = getValue(response);

    if (value == null)
      return;

    String id = getId();
    String cssClass = getCssClass(response);
    boolean isDisabled = isDisabled(response);

    XmlWriter out = response.getWriter();

    out.startElement("span");
    out.writeAttribute("id", id);

    String disabledAttribute = isDisabled ? "disabled" : null;

    out.writeAttribute("class", cssClass, disabledAttribute);

    out.writeText(value);

    out.endElement("span");
  }
}
