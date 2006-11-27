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
import javax.faces.convert.*;

public class UIForm extends UIComponentBase implements NamingContainer
{
  public static final String COMPONENT_FAMILY = "javax.faces.Form";
  public static final String COMPONENT_TYPE = "javax.faces.Form";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private Boolean _isPrependId;
  private ValueExpression _isPrependIdExpr;
  
  private boolean _isSubmitted;

  public UIForm()
  {
    setRendererType("javax.faces.Form");
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

  public boolean isPrependId()
  {
    if (_isPrependId != null)
      return _isPrependId;
    else if (_isPrependIdExpr != null)
      return Util.evalBoolean(_isPrependIdExpr);
    else
      return false;
  }

  public void setPrependId(boolean value)
  {
    _isPrependId = value;
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (_propMap.get(name)) {
      case PREPEND_ID:
	return _isPrependIdExpr;
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
      switch (_propMap.get(name)) {
      case PREPEND_ID:
	if (expr != null && expr.isLiteralText())
	  _isPrependId = (Boolean) expr.getValue(null);
	else
	  _isPrependIdExpr = expr;
	return;
      }
    }

    super.setValueExpression(name, expr);
  }

  //
  // render properties
  //

  public boolean isSubmitted()
  {
    return _isSubmitted;
  }

  public void setSubmitted(boolean isSubmitted)
  {
    _isSubmitted = isSubmitted;
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    State state = new State();

    state._parent = super.saveState(context);
    
    state._isPrependId = _isPrependId;
    state._isPrependIdExpr = Util.save(_isPrependIdExpr, context);

    return state;
  }

  public void restoreState(FacesContext context, Object value)
  {
    State state = (State) value;

    super.restoreState(context, state._parent);

    _isPrependId = state._isPrependId;
    _isPrependIdExpr = Util.restore(state._isPrependIdExpr,
				    Boolean.class,
				    context);
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    PREPEND_ID,
  }

  private static class State implements java.io.Serializable {
    private Object _parent;
    
    private Boolean _isPrependId;
    private String _isPrependIdExpr;
  }

  static {
    _propMap.put("prependId", PropEnum.PREPEND_ID);
  }
}
