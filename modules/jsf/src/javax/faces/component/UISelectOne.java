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

package javax.faces.component;

import java.util.*;

import javax.el.*;
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.model.*;

public class UISelectOne extends UIInput
{
  public static final String COMPONENT_FAMILY = "javax.faces.SelectOne";
  public static final String COMPONENT_TYPE = "javax.faces.SelectOne";
  public static final String INVALID_MESSAGE_ID
    = "javax.faces.component.UISelectOne.INVALID";

  public UISelectOne()
  {
    setRendererType("javax.faces.Menu");
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  public void validateValue(FacesContext context, Object value)
  {
    super.validateValue(context, value);

    if (! isValid() || value == null)
      return;

    boolean hasValue = matchChildren(this, value);

    if (! hasValue) {
      String summary = Util.l10n(context, INVALID_MESSAGE_ID,
				 "{0}: Validation Error: UISelectOne value '{1}' does not match a valid option.",
				 Util.getLabel(context, this),
				 value);

      String detail = summary;

      FacesMessage msg = new FacesMessage(summary, detail);
	
      context.addMessage(getClientId(context), msg);
      
      setValid(false);
    }
  }

  static boolean matchChildren(UIComponent comp, Object value)
  {
    int count = comp.getChildCount();

    if (count <= 0)
      return false;

    List<UIComponent> children = comp.getChildren();
    
    for (int i = 0; i < count; i++) {
      UIComponent child = children.get(i);
      
      if (child instanceof UISelectItem) {
	UISelectItem item = (UISelectItem) child;

	if (value.equals(item.getItemValue())) {
	  return true;
	}
      }
      else if (child instanceof UISelectItems) {
	UISelectItems items = (UISelectItems) child;

	if (matchItems(items.getValue(), value))
	  return true;
      }
    }

    return false;
  }

  private static boolean matchItems(Object selectValue, Object value)
  {
    if (selectValue instanceof SelectItemGroup) {
      SelectItem []items = ((SelectItemGroup) selectValue).getSelectItems();

      if (items != null) {
	for (int i = 0; i < items.length; i++) {
	  if (matchItems(items[i], value))
	    return true;
	}
      }
    }
    else if (selectValue instanceof SelectItem) {
      SelectItem item = (SelectItem) selectValue;

      return value.equals(item.getValue()) && ! item.isDisabled();
    }
    else if (selectValue instanceof SelectItem[]) {
      SelectItem []item = (SelectItem[]) selectValue;

      for (int i = 0; i < item.length; i++) {
	if (value.equals(item[i].getValue()) && ! item[i].isDisabled())
	  return true;
      }
    }
    else if (selectValue instanceof List) {
      List list = (List) selectValue;

      int size = list.size();
      for (int i = 0; i < size; i++) {
	if (matchItems(list.get(i), value))
	  return true;
      }
    }

    return false;
  }
}
