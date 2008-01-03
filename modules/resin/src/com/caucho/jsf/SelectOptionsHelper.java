/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Alex Rojkov
 */


package com.caucho.jsf;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectItems;
import javax.faces.model.SelectItemGroup;
import javax.faces.model.SelectItem;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.io.IOException;

public class SelectOptionsHelper {

  public static void render(UIComponent component,
                            SelectOptionsRenderer renderer)
    throws IOException
  {
    List children = component.getChildren();
    for (Object child : children) {
      if (child instanceof UISelectItem) {
        UISelectItem uiSelectItem = (UISelectItem) child;
        
        SelectItem selectItem = (SelectItem) uiSelectItem.getValue();

        if (selectItem == null)
          renderer.render(uiSelectItem);
        else
          render(renderer, uiSelectItem, null, selectItem);
      }
      else if (child instanceof UISelectItems) {
        UISelectItems uiSelectItems = (UISelectItems) child;
        
        Object options = uiSelectItems.getValue();
        
        if (options instanceof SelectItem) {
          render(renderer, uiSelectItems, null, (SelectItem) options);
        }
        else if (options instanceof Collection) {
          Collection collection = (Collection) options;

          for (Object o : collection) {
            render(renderer, uiSelectItems, null, (SelectItem) o);
          }
        }
        else if (options instanceof SelectItem[]) {
          SelectItem[] selectItems = (SelectItem[]) options;
          
          for (SelectItem selectItem : selectItems) {
            render(renderer, uiSelectItems, null, selectItem);
          }
        }
        else if (options instanceof Map) {
          Map map = (Map) options;
          
          for (Object label : map.keySet()) {
            SelectItem selectItem = new SelectItem(map.get(label),
                                                   String.valueOf(label));
            render(renderer, uiSelectItems, null, selectItem);

          }
        }
      }
    }
  }

  public static void render(SelectOptionsRenderer renderer,
                            UIComponent component,
                            SelectItemGroup parentGroup,
                            SelectItem item)
    throws IOException
  {
    if (item instanceof SelectItemGroup) {
      SelectItemGroup itemGroup = (SelectItemGroup) item;
      
      renderer.renderItemGroupStart(parentGroup, itemGroup);

      SelectItem[] selectItems = itemGroup.getSelectItems();
      
      for (SelectItem selectItem : selectItems) {
        renderer.render(component, itemGroup, selectItem);
      }
      
      renderer.renderItemGroupEnd(parentGroup, itemGroup);
    }
    else {
      renderer.render(component, parentGroup, item);
    }
  }
}