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

public class UISelectBoolean extends UIInput
{
  public static final String COMPONENT_FAMILY = "javax.faces.SelectBoolean";
  public static final String COMPONENT_TYPE = "javax.faces.SelectBoolean";

  private Boolean _selected;
  private ValueExpression _selectedExpr;

  public UISelectBoolean()
  {
    setRendererType("javax.faces.Checkbox");
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

  public boolean isSelected()
  {
    return (Boolean) getValue();
  }

  public void setSelected(boolean value)
  {
    setValue(value);
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("selected".equals(name))
      return super.getValueExpression("value");
    else
      return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("selected".equals(name)) {
      super.setValueExpression("value", expr);
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
    
    state[1] = _selected;
    state[2] = Util.save(_selectedExpr, context);

    return state;
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    super.restoreState(context, state[0]);

    _selected = (Boolean) state[1];
    _selectedExpr = Util.restore(state[2],
				 Boolean.class,
				 context);
  }
}
