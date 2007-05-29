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

import java.io.*;
import java.util.*;

import javax.faces.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.render.*;
import javax.faces.model.*;

/**
 * The base renderer
 */
abstract class SelectRenderer extends BaseRenderer
{
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

	SelectItem item = new SelectItem(uiSelectItem.getItemValue(),
					 uiSelectItem.getItemLabel(),
					 uiSelectItem.getItemDescription(),
					 uiSelectItem.isItemDisabled());

	items.add(item);
      }
      else if (child instanceof UISelectItems) {
	UISelectItems selectItems = (UISelectItems) child;

	Object value = selectItems.getValue();

	if (value instanceof SelectItem)
	  items.add((SelectItem) value);
	else if (value instanceof Collection) {
	  Iterator iter = ((Collection) value).iterator();

	  while (iter.hasNext()) {
	    Object subValue = iter.next();

	    if (subValue instanceof SelectItem)
	      items.add((SelectItem) subValue);
	  }
	}
	else if (value instanceof Map) {
	  Map map = (Map) value;

	  Iterator iter = map.entrySet().iterator();
	  while (iter.hasNext()) {
	    Map.Entry entry = (Map.Entry) iter.next();
	    
	    items.add(new SelectItem(entry.getValue(),
				     String.valueOf(entry.getKey())));
	  }
	}
      }
    }

    return items;
  }
}
