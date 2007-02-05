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

import javax.faces.*;
import javax.faces.context.*;
import javax.faces.convert.*;

public class UIOutput extends UIComponentBase implements ValueHolder
{
  public static final String COMPONENT_FAMILY = "javax.faces.Output";
  public static final String COMPONENT_TYPE = "javax.faces.Output";

  private Converter _converter;
  
  private Object _value;
  private ValueExpression _valueExpr;

  public UIOutput()
  {
    setRendererType("javax.faces.Text");
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  //
  // Properties
  //

  public Object getLocalValue()
  {
    return _value;
  }

  public Object getValue()
  {
    if (_value != null)
      return _value;
    else if (_valueExpr != null)
      return Util.eval(_valueExpr);
    else
      return null;
  }

  public void setValue(Object value)
  {
    _value = value;
  }

  //
  // expression map override
  //

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("value".equals(name)) {
      return _valueExpr;
    }
    else {
      return super.getValueExpression(name);
    }
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("value".equals(name)) {
      if (expr != null && expr.isLiteralText())
	_value = expr.getValue(null);
      else
	_valueExpr = expr;
    }
    else {
      super.setValueExpression(name, expr);
    }
  }

  //
  // Rendering
  //

  /**
   * Returns true, since the component renders the children.
   */
  @Override
  public boolean getRendersChildren()
  {
    return true;
  }

  public Converter getConverter()
  {
    return _converter;
  }

  public void setConverter(Converter converter)
  {
    _converter = converter;
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    Object parent = super.saveState(context);

    return new Object[] {
      parent,
      _value,
      Util.save(_valueExpr, context)
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    if (state != null) 
      super.restoreState(context, state[0]);

    _value = state[1];
    _valueExpr = Util.restore(state[2], String.class, context);
  }
}
