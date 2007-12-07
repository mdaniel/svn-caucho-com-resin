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

import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.model.*;
import java.io.*;
import java.util.*;

/**
 * The base renderer
 */
abstract class SelectRenderer extends BaseRenderer
{
  private ArrayList<SelectItem> getSelectItems(UISelectItems selectItems)
  {
    ArrayList<SelectItem> items = new ArrayList<SelectItem>();

    Object value = selectItems.getValue();

    if (value instanceof SelectItem)
      items.add((SelectItem) value);
    else if (value instanceof Collection) {

      for (Object o : ((Collection) value)) {

        if (o instanceof SelectItem)
          items.add((SelectItem) o);
      }
    }
    else if (value instanceof Map) {
      Map map = (Map) value;

      for (Object o : map.entrySet()) {
        Map.Entry entry = (Map.Entry) o;

        items.add(new SelectItem(entry.getValue(),
                                 String.valueOf(entry.getKey())));
      }
    }
    else if (value instanceof SelectItem[]) {
      SelectItem[] items_ = (SelectItem[]) value;
      items.addAll(Arrays.asList(items_));
    }
    return items;
  }

  protected ArrayList<SelectItem> getSelectItems(UIComponent component)
  {
    ArrayList<SelectItem> items = new ArrayList<SelectItem>();

    int count = component.getChildCount();
    if (count == 0)
      return items;

    List<UIComponent> children = component.getChildren();

    for (int i = 0; i < count; i++) {
      UIComponent child = children.get(i);

      if (child instanceof UISelectItem) {
	UISelectItem uiSelectItem = (UISelectItem) child;

	SelectItem item = new SelectItem();

	item.setValue(uiSelectItem.getItemValue());
	item.setLabel(uiSelectItem.getItemLabel());
	item.setDescription(uiSelectItem.getItemDescription());
	item.setDisabled(uiSelectItem.isItemDisabled());

	items.add(item);
      }
      else if (child instanceof UISelectItems) {
        items.addAll(getSelectItems((UISelectItems)child));
      }
    }

    return items;
  }

  protected void encodeChildren(ResponseWriter out,
                              FacesContext context,
                              UIComponent component,
                              Object []values,
                              String enabledClass,
                              String disabledClass)
    throws IOException
  {
    String clientId = component.getClientId(context);
    
    int childCount = component.getChildCount();
    for (int i = 0; i < childCount; i++) {
      UIComponent child = component.getChildren().get(i);

      String childId = clientId + ":" + i;
      
      if (child instanceof UISelectItem) {
	UISelectItem selectItem = (UISelectItem) child;

	if (child.getId() != null)
	  childId = child.getClientId(context);

	out.startElement("option", child);

	/*
	out.writeAttribute("id", childId, "id");
	out.writeAttribute("name", childId, "name");
	*/

	if (selectItem.isItemDisabled()) {
	  out.writeAttribute("disabled", "disabled", "disabled");

	  if (disabledClass != null)
	    out.writeAttribute("class", disabledClass, "disabledClass");
	}
	else {
	  if (enabledClass != null)
	    out.writeAttribute("class", enabledClass, "enabledClass");
	}

	if (values != null) {
	  for (int j = 0; j < values.length; j++) {
	    if (values[j].equals(selectItem.getItemValue())) {
	      out.writeAttribute("selected", "selected", "selected");
	      break;
	    }
	  }
	}

	out.writeAttribute("value",
                           String.valueOf(selectItem.getItemValue()),
			   "value");

        out.writeText(selectItem.getItemLabel(), "label");

	out.endElement("option");
        out.write("\n");
      }
      else if (child instanceof UISelectItems) {
        UISelectItems uiSelectItems = (UISelectItems) child;
        List<SelectItem> items = getSelectItems(uiSelectItems);

        for (SelectItem selectItem : items) {

          if (child.getId() != null)
            childId = child.getClientId(context);

          out.startElement("option", child);

          if (selectItem.isDisabled()) {
            out.writeAttribute("disabled", "disabled", "disabled");

            if (disabledClass != null)
              out.writeAttribute("class", disabledClass, "disabledClass");
          }
          else {
            if (enabledClass != null)
              out.writeAttribute("class", enabledClass, "enabledClass");
          }

          if (values != null) {
            for (int j = 0; j < values.length; j++) {
              if (values[j].equals(selectItem.getValue())) {
                out.writeAttribute("selected", "selected", "selected");
                break;
              }
            }
          }

          out.writeAttribute("value",
                             String.valueOf(selectItem.getValue()),
                             "value");

          out.writeText(selectItem.getLabel(), "label");

          out.endElement("option");
          out.write("\n");
        }
      }
    }
  }

  protected void encodeOneChildren(ResponseWriter out,
                                   FacesContext context,
                                   UIComponent component,
                                   Object value,
                                   String enabledClass,
                                   String disabledClass)
    throws IOException
  {
    String clientId = component.getClientId(context);

    ArrayList<SelectItem> items = getSelectItems(component);
    for (int i = 0; i < items.size(); i++) {
      String childId = clientId + ":" + i;
      
      SelectItem selectItem = items.get(i);

      String itemLabel = selectItem.getLabel();
      Object itemValue = selectItem.getValue();
      String itemDescription = selectItem.getDescription();

      out.startElement("option", component);

      // jsf/31c4
      /*
      out.writeAttribute("id", childId, "id");
      //out.writeAttribute("name", child.getClientId(context), "name");
      */

      if (value != null && value.equals(itemValue))
	out.writeAttribute("selected", "selected", "selected");

      if (selectItem.isDisabled()) {
	out.writeAttribute("disabled", "disabled", "disabled");

	if (disabledClass != null)
	  out.writeAttribute("class", disabledClass, "disabledClass");
      }
      else {
	if (enabledClass != null)
	  out.writeAttribute("class", enabledClass, "enabledClass");
      }
      
      String itemValueString = toString(context, component, itemValue);
      out.writeAttribute("value", itemValueString, "value");
      
      if (itemLabel == null)
        itemLabel = itemValueString;

      out.writeText(itemLabel, "label");
	
      out.endElement("option");
      out.write("\n");
    }
  }
}
