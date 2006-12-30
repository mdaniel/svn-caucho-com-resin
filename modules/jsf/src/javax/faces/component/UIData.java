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
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.model.*;

public class UIData extends UIComponentBase
  implements NamingContainer
{
  public static final String COMPONENT_FAMILY = "javax.faces.Data";
  public static final String COMPONENT_TYPE = "javax.faces.Data";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private DataModel _dataModel;

  private Integer _first;
  private ValueExpression _firstExpr;

  private Integer _rows;
  private ValueExpression _rowsExpr;

  private Object _value;
  private ValueExpression _valueExpr;

  private String _var;
  private int _rowIndex;

  public UIData()
  {
    setRendererType("javax.faces.Table");
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

  public int getFirst()
  {
    if (_first != null)
      return _first;
    else if (_firstExpr != null)
      return Util.evalInt(_firstExpr);
    else
      return -1;
  }

  public void setFirst(int first)
  {
    _first = first;
  }

  public int getRows()
  {
    if (_rows != null)
      return _rows;
    else if (_rowsExpr != null)
      return Util.evalInt(_rowsExpr);
    else
      return -1;
  }

  public void setRows(int rows)
  {
    _rows = rows;
  }

  public String getVar()
  {
    return _var;
  }

  public void setVar(String var)
  {
    _var = var;
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

  protected DataModel getDataModel()
  {
    if (_dataModel != null)
      return _dataModel;
    
    // XXX:
    
    return (DataModel) getValue();
  }

  protected void setDataModel(DataModel dataModel)
  {
    _dataModel = dataModel;
  }

  public int getRowIndex()
  {
    return getDataModel().getRowIndex();
  }

  public Object getRowData()
  {
    return getDataModel().getRowData();
  }

  protected void setRowIndex(int value)
  {
    getDataModel().setRowIndex(value);
  }

  public int getRowCount()
  {
    return getDataModel().getRowCount();
  }

  public boolean isRowAvailable()
  {
    return getDataModel().isRowAvailable();
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
      case VALUE:
	return _valueExpr;
      
      case FIRST:
	return _firstExpr;
      
      case ROWS:
	return _rowsExpr;
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
      case VALUE:
	if (expr != null && expr.isLiteralText())
	  _value = expr.getValue(null);
	else
	  _valueExpr = expr;
	break;
      
      case FIRST:
	if (expr != null && expr.isLiteralText())
	  _first = (Integer) expr.getValue(null);
	else
	  _firstExpr = expr;
	break;
      
      case ROWS:
	if (expr != null && expr.isLiteralText())
	  _rows = (Integer) expr.getValue(null);
	else
	  _rowsExpr = expr;
	break;
      }
    }

    super.setValueExpression(name, expr);
  }

  //
  // Facets
  //

  public UIComponent getHeader()
  {
    return getFacet("header");
  }

  public void setHeader(UIComponent header)
  {
    getFacets().put("header", header);
  }

  public UIComponent getFooter()
  {
    return getFacet("footer");
  }

  public void setFooter(UIComponent footer)
  {
    getFacets().put("footer", footer);
  }

  //
  // state
  //

  public Object saveState(FacesContext context)
  {
    State state = new State();

    state._parent = super.saveState(context);
    
    state._value = _value;
    state._valueExpr = Util.save(_valueExpr, context);
    
    state._first = _first;
    state._firstExpr = Util.save(_firstExpr, context);
    
    state._rows = _rows;
    state._rowsExpr = Util.save(_rowsExpr, context);
    
    state._var = _var;

    return state;
  }

  public void restoreState(FacesContext context, Object value)
  {
    State state = (State) value;

    super.restoreState(context, state._parent);

    _value = state._value;
    _valueExpr = Util.restore(state._valueExpr, String.class, context);

    _first = state._first;
    _firstExpr = Util.restore(state._firstExpr, Integer.class, context);

    _rows = state._rows;
    _rowsExpr = Util.restore(state._firstExpr, Integer.class, context);

    _var = state._var;
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    VALUE,
    FIRST,
    ROWS,
  }

  private static class State implements java.io.Serializable {
    private Object _parent;
    
    private Object _value;
    private String _valueExpr;
    
    private Integer _first;
    private String _firstExpr;
    
    private Integer _rows;
    private String _rowsExpr;
    
    private String _var;
  }

  static {
    _propMap.put("value", PropEnum.VALUE);
    _propMap.put("first", PropEnum.FIRST);
    _propMap.put("rows", PropEnum.ROWS);
  }
}
