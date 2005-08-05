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

import com.caucho.vfs.XmlWriter;

import java.io.IOException;

public class TextboxWidget
  extends XulWidget
{
  private static final int NULL_INTEGER = Integer.MIN_VALUE;
  private static final String NULL_VALUE = "";

  private VarDefinition valueDef = new XulVarDefinition(this, "value", String.class);

  private EditProperty _editProperty  = new EditProperty(this);
  private DisabledProperty _disabledProperty  = new DisabledProperty(this);
  private ReadonlyProperty _readonlyProperty  = new ReadonlyProperty(this);

  private boolean _isMultiline;

  private int _tabindex = NULL_INTEGER;

  private int _cols = NULL_INTEGER;
  private int _rows = NULL_INTEGER;

  private int _size = NULL_INTEGER;

  private int _maxlength = NULL_INTEGER;

  public TextboxWidget()
  {
  }

  public TextboxWidget(String id)
  {
    setId(id);
  }

  public TextboxWidget(String id, String initialValue)
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

  /**
   * Default false.
   */
  public void setMultiline(boolean isMultiline)
  {
    _isMultiline = isMultiline;
  }

  public boolean isMultiline()
  {
    return _isMultiline;
  }

  /**
   * This value determines the width of the control that is displayed,
   * the default is to use the value of <i>cols</i>.
   *
   * If both <i>cols</i> and <i>size</i> are set, then <i>size</i> is used
   * for a singleline control, <i>cols</i> is used for a multiline control.
   *
   * If no <i>size</i> or <i>cols</i> has been set, the typical result is that
   * the browser chooses a width.
   */
  public void setSize(int size)
  {
    _size = size;
  }

  public int getSize()
  {
    return _size;
  }

  /**
   * This value determines the width of the control that is displayed,
   * the default is to use the value of <i>size</i>.
   *
   * If both <i>cols</i> and <i>size</i> are set, then <i>size</i> is used
   * for a singleline control, <i>cols</i> is used for a multiline control.
   *
   * If no <i>cols</i> or <i>size</i> has been set, the typical result is that
   * the browser chooses a width.
   */
  public void setCols(int cols)
  {
    _cols = cols;
  }

  public int getCols()
  {
    return _cols;
  }

  /**
   * This value determines the height of the control that is displayed,
   * the default is to specify no <i>rows</i>, typically resulting in the browser
   * choosing a height.
   *
   * This value applies to only a multi line textbox, it is ignored for single line.
   */
  public void setRows(int rows)
  {
    _rows = rows;
  }

  public int getRows()
  {
    return _rows;
  }

  /**
   * The maximum number of characters that the textbox allows to be entered.
   * For HTML, this will typically only have an effect for single-line textboxes.
   */
  public void setMaxlength(int maxlength)
  {
    _maxlength = maxlength;
  }

  public int getMaxlength()
  {
    return _maxlength;
  }

  public void setTabindex(int tabindex)
  {
    _tabindex = tabindex;
  }

  public int getTabindex()
  {
    return _tabindex;
  }

  public void setEdit(boolean isEdit)
  {
    _editProperty.setValue(isEdit);
  }

  public boolean isEdit()
  {
    return _editProperty.getValue();
  }

  public void setDisabled(boolean isDisabled)
  {
    _disabledProperty.setValue(isDisabled);
  }

  public boolean isDisabled()
  {
    return _disabledProperty.getValue();
  }

  public void setReadonly(boolean isReadonly)
  {
    _readonlyProperty.setValue(isReadonly);
  }

  public boolean isReadonly()
  {
    return _readonlyProperty.getValue();
  }

  public void init(WidgetInit init)
    throws WidgetException
  {
    super.init(init);

    _editProperty.init(init);
    _disabledProperty.init(init);
    _readonlyProperty.init(init);

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

  public void setEdit(VarContext context, boolean isEdit)
  {
    _editProperty.setValue(context, isEdit);
  }

  public boolean isEdit(VarContext context)
  {
    return _editProperty.getValue(context);
  }

  public void setDisabled(VarContext context, boolean isDisabled)
  {
    _disabledProperty.setValue(context, isDisabled);
  }

  public boolean isDisabled(VarContext context)
  {
    return _disabledProperty.getValue(context);
  }

  public void setReadonly(VarContext context, boolean isReadonly)
  {
    _readonlyProperty.setValue(context, isReadonly);
  }

  public boolean isReadonly(VarContext context)
  {
    return _readonlyProperty.getValue(context);
  }

  public void response(WidgetResponse response)
    throws WidgetException, IOException
  {
    XmlWriter out = response.getWriter();

    String id = getId();
    String cssClass = getCssClass(response);

    String value = getValue(response);

    boolean isEdit = isEdit(response);
    boolean isReadonly = isReadonly(response);
    boolean isDisabled = isDisabled(response);

    out.startElement("span");
    out.writeAttribute("id", id);

    String readonlyAttribute = isReadonly ? "readonly" : null;
    String disabledAttribute = isDisabled ? "disabled" : null;

    out.writeAttribute("class", cssClass, readonlyAttribute, disabledAttribute);

    if (isEdit) {
      int tabindex = getTabindex();
      int cols = getCols();
      int rows = getRows();
      int maxlength = getMaxlength();
      int size = getSize();

      if (isMultiline()) {
        out.startElement("textarea");

        out.writeAttribute("name", id);

        if (tabindex != NULL_INTEGER)
          out.writeAttribute("tabindex", getCols());

        if (isReadonly)
          out.writeAttribute("readonly", true);

        if (isDisabled)
          out.writeAttribute("disabled", true);

        if (cols != NULL_INTEGER)
          out.writeAttribute("cols", getCols());
        else if (size != NULL_INTEGER)
          out.writeAttribute("cols", size);

        if (rows != NULL_INTEGER)
          out.writeAttribute("rows", rows);

        if (value != null)
          out.writeText(value);

        out.endElement("textarea");
      }
      else {
        out.startElement("input");
        out.writeAttribute("name", id);

        if (tabindex != NULL_INTEGER)
          out.writeAttribute("tabindex", getCols());

        if (isReadonly)
          out.writeAttribute("readonly", true);

        if (isDisabled)
          out.writeAttribute("disabled", true);

        if (maxlength != NULL_INTEGER)
          out.writeAttribute("maxlength", maxlength);

        if (size != NULL_INTEGER)
          out.writeAttribute("size", size);
        else if (cols != NULL_INTEGER)
          out.writeAttribute("size", cols);

        if (value != null)
          out.writeAttribute("value", value);

        out.endElement("input");
      }
    }
    else {
      out.writeText(value);
    }

    out.endElement("span");
  }
}