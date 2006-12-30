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
import javax.faces.context.*;
import javax.faces.model.*;

public class UISelectItem extends UIComponentBase
{
  public static final String COMPONENT_FAMILY = "javax.faces.SelectItem";
  public static final String COMPONENT_TYPE = "javax.faces.SelectItem";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _itemDescription;
  private ValueExpression _itemDescriptionExpr;

  private Boolean _itemDisabled;
  private ValueExpression _itemDisabledExpr;

  private String _itemLabel;
  private ValueExpression _itemLabelExpr;

  private Object _itemValue;
  private ValueExpression _itemValueExpr;

  private SelectItem _value;
  private ValueExpression _valueExpr;

  public UISelectItem()
  {
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  //
  // properties
  //

  public String getItemDescription()
  {
    if (_itemDescription != null)
      return _itemDescription;
    else if (_itemDescriptionExpr != null)
      return Util.evalString(_itemDescriptionExpr);
    else
      return null;
  }

  public void setItemDescription(String value)
  {
    _itemDescription = value;
  }

  public boolean isItemDisabled()
  {
    if (_itemDisabled != null)
      return _itemDisabled;
    else if (_itemDisabledExpr != null)
      return Util.evalBoolean(_itemDisabledExpr);
    else
      return false;
  }

  public void setItemDisabled(boolean value)
  {
    _itemDisabled = value;
  }

  public String getItemLabel()
  {
    if (_itemLabel != null)
      return _itemLabel;
    else if (_itemLabelExpr != null)
      return Util.evalString(_itemLabelExpr);
    else
      return null;
  }

  public void setItemLabel(String value)
  {
    _itemLabel = value;
  }

  public Object getItemValue()
  {
    if (_itemValue != null)
      return _itemValue;
    else if (_itemValueExpr != null)
      return Util.eval(_itemValueExpr);
    else
      return null;
  }

  public void setItemValue(Object value)
  {
    _itemValue = value;
  }

  public SelectItem getValue()
  {
    if (_value != null)
      return _value;
    else if (_valueExpr != null)
      return (SelectItem) Util.eval(_valueExpr);
    else
      return null;
  }

  public void setValue(SelectItem value)
  {
    _value = value;
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (prop) {
      case ITEM_DESCRIPTION:
	return _itemDescriptionExpr;
      case ITEM_DISABLED:
	return _itemDisabledExpr;
      case ITEM_LABEL:
	return _itemLabelExpr;
      case ITEM_VALUE:
	return _itemValueExpr;
      case VALUE:
	return _valueExpr;
      }
    }
    
    return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (prop) {
      case ITEM_DESCRIPTION:
	if (expr != null && expr.isLiteralText())
	  _itemDescription = Util.evalString(expr);
	else
	  _itemDescriptionExpr = expr;
	return;
	
      case ITEM_DISABLED:
	if (expr != null && expr.isLiteralText())
	  _itemDisabled = Util.evalBoolean(expr);
	else
	  _itemDisabledExpr = expr;
	return;
	
      case ITEM_LABEL:
	if (expr != null && expr.isLiteralText())
	  _itemLabel = Util.evalString(expr);
	else
	  _itemLabelExpr = expr;
	return;
	
      case ITEM_VALUE:
	if (expr != null && expr.isLiteralText())
	  _itemValue = Util.eval(expr);
	else
	  _itemValueExpr = expr;
	return;
	
      case VALUE:
	if (expr != null && expr.isLiteralText())
	  _value = (SelectItem) Util.eval(expr);
	else
	  _valueExpr = expr;
	return;
      }
    }
    
    super.setValueExpression(name, expr);
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    Object []state = new Object[11];

    state[0] = super.saveState(context);
    
    state[1] = _itemDescription;
    state[2] = Util.save(_itemDescriptionExpr, context);
    
    state[3] = _itemDisabled;
    state[4] = Util.save(_itemDisabledExpr, context);
    
    state[5] = _itemLabel;
    state[6] = Util.save(_itemLabelExpr, context);
    
    state[7] = _itemValue;
    state[8] = Util.save(_itemValueExpr, context);
    
    state[9] = _value;
    state[10] = Util.save(_valueExpr, context);

    return state;
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _itemDescription = (String) state[1];
    _itemDescriptionExpr = Util.restore(state[2],
					String.class,
					context);

    if (state[3] != null)
      _itemDisabled = (Boolean) state[3];
    _itemDisabledExpr = Util.restore(state[4],
				     Boolean.class,
				     context);

    _itemLabel = (String) state[5];
    _itemLabelExpr = Util.restore(state[6],
				  String.class,
				  context);

    _itemValue = state[7];
    _itemValueExpr = Util.restore(state[8],
				  Object.class,
				  context);

    _value = (SelectItem) state[9];
    _valueExpr = Util.restore(state[10],
			      Object.class,
			      context);
  }

  private enum PropEnum {
    ITEM_DESCRIPTION,
    ITEM_DISABLED,
    ITEM_LABEL,
    ITEM_VALUE,
    VALUE,
  }

  static {
    _propMap.put("itemDescription", PropEnum.ITEM_DESCRIPTION);
    _propMap.put("itemDisabled", PropEnum.ITEM_DISABLED);
    _propMap.put("itemLabel", PropEnum.ITEM_LABEL);
    _propMap.put("itemValue", PropEnum.ITEM_VALUE);
    _propMap.put("value", PropEnum.VALUE);
  }
}
