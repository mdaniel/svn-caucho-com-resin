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

public class UISelectMany extends UIInput
{
  public static final String COMPONENT_FAMILY = "javax.faces.SelectMany";
  public static final String COMPONENT_TYPE = "javax.faces.SelectMany";
  public static final String INVALID_MESSAGE_ID
    = "javax.faces.component.UISelectMany.INVALID";

  private Object []_selectedValues;
  private ValueExpression _selectedValuesExpr;

  public UISelectMany()
  {
    setRendererType("javax.faces.Listbox");
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

  public Object []getSelectedValues()
  {
    if (_selectedValues != null)
      return _selectedValues;
    else if (_selectedValuesExpr != null)
      return (Object [])Util.eval(_selectedValuesExpr);
    else
      return null;
  }

  public void setSelectedValues(Object []value)
  {
    _selectedValues = value;
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("selectedValues".equals(name))
      return _selectedValuesExpr;
    else
      return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("selectedValues".equals(name)) {
      if (expr != null && expr.isLiteralText())
	_selectedValues = (Object []) Util.eval(expr);
      else
	_selectedValuesExpr = expr;
    }
    else
      super.setValueExpression(name, expr);
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    Object []state = new Object[3];

    state[0] = super.saveState(context);
    
    state[1] = _selectedValues;
    state[2] = Util.save(_selectedValuesExpr, context);

    return state;
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _selectedValues = (Object []) state[1];
    _selectedValuesExpr = Util.restore(state[2],
				       Object.class,
				       context);
  }
}
