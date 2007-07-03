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
import java.sql.ResultSet;

import javax.el.*;

import javax.faces.*;
import javax.faces.context.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.model.*;

import javax.servlet.jsp.jstl.sql.Result;

public class UIData extends UIComponentBase
  implements NamingContainer
{
  public static final String COMPONENT_FAMILY = "javax.faces.Data";
  public static final String COMPONENT_TYPE = "javax.faces.Data";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private static final Object[] NULL_ARRAY = new Object[0];

  private DataModel _dataModel;

  private Integer _first;
  private ValueExpression _firstExpr;

  private Integer _rows;
  private ValueExpression _rowsExpr;

  private Object _value;
  private ValueExpression _valueExpr;

  private String _var;
  private int _rowIndex = -1;

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
      return Util.evalInt(_firstExpr, getFacesContext());
    else
      return 0;
  }

  public void setFirst(int first)
  {
    if (first < 0)
      throw new IllegalArgumentException("UIData.setFirst must have a positive value at '" + first + "'");
    _first = first;
  }

  public int getRows()
  {
    if (_rows != null)
      return _rows;
    else if (_rowsExpr != null)
      return Util.evalInt(_rowsExpr, getFacesContext());
    else
      return -1;
  }

  public void setRows(int rows)
  {
    if (rows < 0)
      throw new IllegalArgumentException("UIData.setFirst must have a positive value at '" + rows + "'");
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
      return Util.eval(_valueExpr, getFacesContext());
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
    
    Object value = getValue();

    if (value == null)
      _dataModel = new ArrayDataModel(NULL_ARRAY);
    else if (value instanceof DataModel)
      _dataModel = (DataModel) value;
    else if (value instanceof List)
      _dataModel = new ListDataModel((List) value);
    else if (value instanceof ResultSet)
      _dataModel = new ResultSetDataModel((ResultSet) value);
    else if (value instanceof Result)
      _dataModel = new ResultDataModel((Result) value);
    else if (value.getClass().isArray())
      _dataModel = new ArrayDataModel((Object []) value);
    else
      _dataModel = new ScalarDataModel(value);
    
    return _dataModel;
  }

  protected void setDataModel(DataModel dataModel)
  {
    _dataModel = dataModel;
  }

  public int getRowIndex()
  {
    return _rowIndex;
  }

  public Object getRowData()
  {
    return getDataModel().getRowData();
  }

  public void setRowIndex(int value)
  {
    if (value < -1)
      throw new IllegalArgumentException("UIData.setRowIndex must not be less than -1 at '" + value + "'");
    
    _rowIndex = value;

    DataModel dataModel = getDataModel();
    
    dataModel.setRowIndex(value);

    if (_var == null) {
    }
    else if (value < 0) {
      FacesContext context = FacesContext.getCurrentInstance();

      context.getExternalContext().getRequestMap().remove(_var);
    }
    else {
      Object rowData = dataModel.getRowData();
      
      FacesContext context = FacesContext.getCurrentInstance();

      context.getExternalContext().getRequestMap().put(_var, rowData);
    }
  }

  public int getRowCount()
  {
    DataModel model = getDataModel();

    if (model != null)
      return model.getRowCount();
    else
      return -1;
  }

  public boolean isRowAvailable()
  {
    DataModel model = getDataModel();

    if (model != null)
      return model.isRowAvailable();
    else
      return false;
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
	return;
      
      case FIRST:
	if (expr != null && expr.isLiteralText())
	  _first = (Integer) expr.getValue(null);
	else
	  _firstExpr = expr;
	return;
      
      case ROWS:
	if (expr != null && expr.isLiteralText())
	  _rows = (Integer) expr.getValue(null);
	else
	  _rowsExpr = expr;
	return;
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
    return new Object[] {
      super.saveState(context),
    
      _value, // XXX: stateHolder issues
      Util.save(_valueExpr, context),
    
      _first,
      Util.save(_firstExpr, context),
    
      _rows,
      Util.save(_rowsExpr, context),
    
      _var,
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;
    
    super.restoreState(context, state[0]);

    _value = state[1];
    _valueExpr = Util.restore(state[2], Object.class, context);

    _first = (Integer) state[3];
    _firstExpr = Util.restore(state[4], Integer.class, context);

    _rows = (Integer) state[5];
    _rowsExpr = Util.restore(state[6], Integer.class, context);

    _var = (String) state[7];
  }

  //
  // private helpers
  //

  private static enum PropEnum {
    VALUE,
    FIRST,
    ROWS,
  }

  static {
    _propMap.put("value", PropEnum.VALUE);
    _propMap.put("first", PropEnum.FIRST);
    _propMap.put("rows", PropEnum.ROWS);
  }
}
