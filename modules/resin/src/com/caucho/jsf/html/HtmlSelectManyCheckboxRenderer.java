/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.html;

import com.caucho.jsf.SelectOptionsHelper;
import com.caucho.jsf.SelectOptionsRenderer;

import java.io.*;
import java.util.*;

import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.convert.*;
import javax.faces.context.*;
import javax.faces.render.*;
import javax.faces.model.SelectItemGroup;
import javax.faces.model.SelectItem;

/**
 * The HTML selectMany/checkbox renderer
 */
class HtmlSelectManyCheckboxRenderer
  extends Renderer
{
  public static final Renderer RENDERER
    = new HtmlSelectManyCheckboxRenderer();

  /**
   * True if the renderer is responsible for rendering the children.
   */
  @Override
  public boolean getRendersChildren()
  {
    return true;
  }

  /**
   * Decodes the data from the form.
   */
  @Override
  public void decode(FacesContext context, UIComponent component)
  {
    String clientId = component.getClientId(context);

    ExternalContext ext = context.getExternalContext();
    Map<String, String[]> paramMap = ext.getRequestParameterValuesMap();

    String[] value = paramMap.get(clientId);

    if (value != null)
      ((EditableValueHolder) component).setSubmittedValue(value);
  }

  public Object getConvertedValue(FacesContext context,
                                  UIComponent component,
                                  Object submittedValue)
    throws ConverterException
  {
    Converter converter = null;//component.getConverter();

    UISelectMany uiSelectMany = (UISelectMany) component;

    Object value = submittedValue;
    /*
    if (converter != null)
      value = converter.getAsObject(context, component, value);
    */

    uiSelectMany.setSelectedValues((Object[]) value);
    uiSelectMany.setValid(true);

    return value;
  }

  /**
   * Renders the open tag for the text.
   */
  @Override
  public void encodeBegin(FacesContext context, final UIComponent component)
    throws IOException
  {
    final ResponseWriter out = context.getResponseWriter();

    String id = component.getId();

    final String accesskey;
    final int border;
    final String dir;
    final boolean disabled;
    final String disabledClass;
    final String enabledClass;
    final String lang;
    final String layout;

    final String onblur;
    final String onchange;
    final String onclick;
    final String ondblclick;
    final String onfocus;

    final String onkeydown;
    final String onkeypress;
    final String onkeyup;

    final String onmousedown;
    final String onmousemove;
    final String onmouseout;
    final String onmouseover;
    final String onmouseup;

    final String onselect;

    final boolean readonly;
    final String style;
    final String styleClass;
    final String tabindex;
    final String title;
    final Object value;

    if (component instanceof HtmlSelectManyCheckbox) {
      HtmlSelectManyCheckbox htmlComponent
	= (HtmlSelectManyCheckbox) component;

      accesskey = htmlComponent.getAccesskey();
      border = htmlComponent.getBorder();
      dir = htmlComponent.getDir();
      disabled = htmlComponent.isDisabled();
      disabledClass = htmlComponent.getDisabledClass();
      enabledClass = htmlComponent.getEnabledClass();
      lang = htmlComponent.getLang();
      layout = htmlComponent.getLayout();
      
      onblur = htmlComponent.getOnblur();
      onchange = htmlComponent.getOnchange();
      onclick = htmlComponent.getOnclick();
      ondblclick = htmlComponent.getOndblclick();
      onfocus = htmlComponent.getOnfocus();

      onkeydown = htmlComponent.getOnkeydown();
      onkeypress = htmlComponent.getOnkeypress();
      onkeyup = htmlComponent.getOnkeyup();

      onmousedown = htmlComponent.getOnmousedown();
      onmousemove = htmlComponent.getOnmousemove();
      onmouseout = htmlComponent.getOnmouseout();
      onmouseover = htmlComponent.getOnmouseover();
      onmouseup = htmlComponent.getOnmouseup();

      onselect = htmlComponent.getOnselect();

      readonly = htmlComponent.isReadonly();
      style = htmlComponent.getStyle();
      styleClass = htmlComponent.getStyleClass();
      tabindex = htmlComponent.getTabindex();
      title = htmlComponent.getTitle();

      value = htmlComponent.getValue();
    }
    else {
      Map<String,Object> attrMap = component.getAttributes();

      accesskey = (String) attrMap.get("accesskey");
      border = (Integer) attrMap.get("border");
      dir = (String) attrMap.get("dir");
      disabled = (Boolean) attrMap.get("disabled");
      disabledClass = (String) attrMap.get("disabledClass");
      enabledClass = (String) attrMap.get("enabledClass");
      lang = (String) attrMap.get("lang");
      layout = (String) attrMap.get("layout");

      onblur = (String) attrMap.get("onblur");
      onchange = (String) attrMap.get("onchange");
      onclick = (String) attrMap.get("onclick");
      ondblclick = (String) attrMap.get("ondblclick");
      onfocus = (String) attrMap.get("onfocus");
      
      onkeydown = (String) attrMap.get("onkeydown");
      onkeypress = (String) attrMap.get("onkeypress");
      onkeyup = (String) attrMap.get("onkeyup");
      
      onmousedown = (String) attrMap.get("onmousedown");
      onmousemove = (String) attrMap.get("onmousemove");
      onmouseout = (String) attrMap.get("onmouseout");
      onmouseover = (String) attrMap.get("onmouseover");
      onmouseup = (String) attrMap.get("onmouseup");
      
      onselect = (String) attrMap.get("onselect");

      readonly = (Boolean) attrMap.get("readonly");
      style = (String) attrMap.get("style");
      styleClass = (String) attrMap.get("styleClass");
      tabindex = (String) attrMap.get("tabindex");
      title = (String) attrMap.get("title");
      
      value = attrMap.get("value");
    }

    UIViewRoot viewRoot = context.getViewRoot();
    
    out.startElement("table", component);

    if (border > 0)
      out.writeAttribute("border", border, "border");

    if (style != null)
      out.writeAttribute("style", style, "style");

    if (styleClass != null)
      out.writeAttribute("class", styleClass, "class");

    if (disabled)
      out.writeAttribute("disabled", "disabled", "disabled");

    out.write("\n");
    
    if (! "pageDirection".equals(layout)) {
      out.startElement("tr", component);
      out.write("\n");
    }

    final String clientId = component.getClientId(context);

    SelectOptionsHelper.render(component, new SelectOptionsRenderer() {
      private int counter;

      public void renderItemGroupStart(SelectItemGroup parentGroup,
                                       SelectItemGroup itemGroup)
        throws IOException
      {

        if ("pageDirection".equals(layout)) {
          out.startElement("tr", component);
          out.write("\n");
        }

        out.startElement("td", component);
        
        out.write("\n");

        out.startElement("table", component);

        if (border > 0)
          out.writeAttribute("border", border, "border");

        if (style != null)
          out.writeAttribute("style", style, "style");

        if (styleClass != null)
          out.writeAttribute("class", styleClass, "class");

        if (disabled)
          out.writeAttribute("disabled", "disabled", "disabled");

        out.write("\n");

        if (! "pageDirection".equals(layout)) {
          out.startElement("tr", component);
          out.write("\n");
        }
      }

      public void renderItemGroupEnd(SelectItemGroup parentGroup,
                                     SelectItemGroup itemGroup)
        throws IOException
      {
        if (! "pageDirection".equals(layout)) {
          out.endElement("tr");
          out.write("\n");
        }

        out.endElement("table");
        out.write("\n");

        out.endElement("td");
        out.write("\n");

        if ("pageDirection".equals(layout)) {
          out.endElement("tr");
          out.write("\n");
        }
      }

      public void render(UISelectItem item)
        throws IOException
      {
        SelectItem selectItem = new SelectItem(item.getItemValue(),
                                               item.getItemLabel());
        selectItem.setDisabled(item.isItemDisabled());
        selectItem.setEscape(item.isItemEscaped());
        this.render(item, null, selectItem);
      }

      public void render(UIComponent component,
                         SelectItemGroup selectItemGroup,
                         SelectItem selectItem)
        throws IOException
      {
        String childId = clientId + ":" + counter++;
        if ("pageDirection".equals(layout)) {
          out.startElement("tr", component);
          out.write("\n");
        }

        out.startElement("td", component);

        out.startElement("input", component);
        out.writeAttribute("id", childId, "id");
        out.writeAttribute("name", clientId, "name");
        out.writeAttribute("type", "checkbox", "type");

        if (selectItem.isDisabled() || disabled)
          out.writeAttribute("disabled", "disabled", "disabled");

        if (value instanceof String[]) {
          String[] values = (String[]) value;

          for (int j = 0; j < values.length; j++) {
            if (values[j].equals(selectItem.getValue())) {
              //todo alex need to coerse to getType of ValueExpression
              out.writeAttribute("checked", "checked", "value");
              break;
            }
          }
        }

        if (accesskey != null)
          out.writeAttribute("accesskey", accesskey, "accesskey");

        if (dir != null)
          out.writeAttribute("dir", dir, "dir");

        if (lang != null)
          out.writeAttribute("lang", lang, "lang");

        if (onblur != null)
          out.writeAttribute("onblur", onblur, "onblur");

        if (onchange != null)
          out.writeAttribute("onchange", onchange, "onchange");

        if (onclick != null)
          out.writeAttribute("onclick", onclick, "onclick");

        if (ondblclick != null)
          out.writeAttribute("ondblclick", ondblclick, "ondblclick");

        if (onfocus != null)
          out.writeAttribute("onfocus", onfocus, "onfocus");

        if (onkeydown != null)
          out.writeAttribute("onkeydown", onkeydown, "onkeydown");

        if (onkeypress != null)
          out.writeAttribute("onkeypress", onkeypress, "onkeypress");

        if (onkeyup != null)
          out.writeAttribute("onkeyup", onkeyup, "onkeyup");

        if (onmousedown != null)
          out.writeAttribute("onmousedown", onmousedown, "onmousedown");

        if (onmousemove != null)
          out.writeAttribute("onmousemove", onmousemove, "onmousemove");

        if (onmouseout != null)
          out.writeAttribute("onmouseout", onmouseout, "onmouseout");

        if (onmouseover != null)
          out.writeAttribute("onmouseover", onmouseover, "onmouseover");

        if (onmouseup != null)
          out.writeAttribute("onmouseup", onmouseup, "onmouseup");

        if (onselect != null)
          out.writeAttribute("onselect", onselect, "onselect");

        if (readonly)
          out.writeAttribute("readonly", "readonly", "readonly");

        if (tabindex != null)
          out.writeAttribute("tabindex", tabindex, "tabindex");

        if (title != null)
          out.writeAttribute("title", title, "title");

        Object itemValue = selectItem.getValue();
        if (itemValue != null)
          out.writeAttribute("value", String.valueOf(itemValue), "value");

        out.endElement("input");

        if (selectItem.getLabel() != null) {
          out.startElement("label", component);
          out.writeAttribute("for", childId, "for");

          if (selectItem.isDisabled() || disabled) {
            if (disabledClass != null)
              out.writeAttribute("class", disabledClass, "disabledClass");
          }
          else {
            if (enabledClass != null)
              out.writeAttribute("class", enabledClass, "enabledClass");
          }

          out.writeText(selectItem.getLabel(), "itemLabel");

          out.endElement("label");
        }
        out.endElement("td");
        out.write("\n");

        if ("pageDirection".equals(layout)) {
          out.endElement("tr");
          out.write("\n");
        }
      }
    });

    if (!"pageDirection".equals(layout)) {
      out.endElement("tr");
      out.write("\n");
    }

    out.endElement("table");
    out.write("\n");
  }

  /**
   * Renders the content for the component.
   */
  @Override
  public void encodeChildren(FacesContext context, UIComponent component)
    throws IOException
  {
  }

  /**
   * Renders the closing tag for the component.
   */
  @Override
  public void encodeEnd(FacesContext context, UIComponent component)
    throws IOException
  {
  }

  public String toString()
  {
    return "HtmlInputTextRenderer[]";
  }
}
