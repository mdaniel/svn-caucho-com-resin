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

package javax.faces.component.html;

import java.util.*;

import javax.el.*;

import javax.faces.component.*;
import javax.faces.context.*;

public class HtmlPanelGrid extends UIPanel
{
  public static final String COMPONENT_TYPE = "javax.faces.HtmlPanelGrid";

  private static final HashMap<String,PropEnum> _propMap
    = new HashMap<String,PropEnum>();

  private String _bgcolor;
  private ValueExpression _bgcolorExpr;
  
  private Integer _border;
  private ValueExpression _borderExpr;
  
  private String _captionClass;
  private ValueExpression _captionClassExpr;
  
  private String _captionStyle;
  private ValueExpression _captionStyleExpr;
  
  private String _cellpadding;
  private ValueExpression _cellpaddingExpr;
  
  private String _cellspacing;
  private ValueExpression _cellspacingExpr;
  
  private String _columnClasses;
  private ValueExpression _columnClassesExpr;
  
  private Integer _columns;
  private ValueExpression _columnsExpr;
  
  private String _dir;
  private ValueExpression _dirExpr;
  
  private String _footerClass;
  private ValueExpression _footerClassExpr;
  
  private String _frame;
  private ValueExpression _frameExpr;
  
  private String _headerClass;
  private ValueExpression _headerClassExpr;
  
  private String _lang;
  private ValueExpression _langExpr;
  
  private String _onclick;
  private ValueExpression _onclickExpr;
  
  private String _ondblclick;
  private ValueExpression _ondblclickExpr;

  private String _onkeydown;
  private ValueExpression _onkeydownExpr;

  private String _onkeypress;
  private ValueExpression _onkeypressExpr;

  private String _onkeyup;
  private ValueExpression _onkeyupExpr;

  private String _onmousedown;
  private ValueExpression _onmousedownExpr;

  private String _onmousemove;
  private ValueExpression _onmousemoveExpr;

  private String _onmouseout;
  private ValueExpression _onmouseoutExpr;

  private String _onmouseover;
  private ValueExpression _onmouseoverExpr;

  private String _onmouseup;
  private ValueExpression _onmouseupExpr;

  private String _rowClasses;
  private ValueExpression _rowClassesExpr;

  private String _rules;
  private ValueExpression _rulesExpr;

  private String _style;
  private ValueExpression _styleExpr;

  private String _styleClass;
  private ValueExpression _styleClassExpr;

  private String _summary;
  private ValueExpression _summaryExpr;

  private String _title;
  private ValueExpression _titleExpr;

  private String _width;
  private ValueExpression _widthExpr;

  public HtmlPanelGrid()
  {
    setRendererType("javax.faces.Grid");
  }

  //
  // properties
  //

  public String getBgcolor()
  {
    if (_bgcolor != null)
      return _bgcolor;
    else if (_bgcolorExpr != null)
      return Util.evalString(_bgcolorExpr);
    else
      return null;
  }

  public void setBgcolor(String value)
  {
    _bgcolor = value;
  }

  public int getBorder()
  {
    if (_border != null)
      return _border;
    else if (_borderExpr != null)
      return Util.evalInt(_borderExpr);
    else
      return Integer.MIN_VALUE;
  }

  public void setBorder(int value)
  {
    _border = value;
  }
  
  public String getCaptionClass()
  {
    if (_captionClass != null)
      return _captionClass;
    else if (_captionClassExpr != null)
      return Util.evalString(_captionClassExpr);
    else
      return null;
  }

  public void setCaptionClass(String value)
  {
    _captionClass = value;
  }
  
  public String getCaptionStyle()
  {
    if (_captionStyle != null)
      return _captionStyle;
    else if (_captionStyleExpr != null)
      return Util.evalString(_captionStyleExpr);
    else
      return null;
  }

  public void setCaptionStyle(String value)
  {
    _captionStyle = value;
  }
  
  public String getCellpadding()
  {
    if (_cellpadding != null)
      return _cellpadding;
    else if (_cellpaddingExpr != null)
      return Util.evalString(_cellpaddingExpr);
    else
      return null;
  }

  public void setCellpadding(String value)
  {
    _cellpadding = value;
  }
  
  public String getCellspacing()
  {
    if (_cellspacing != null)
      return _cellspacing;
    else if (_cellspacingExpr != null)
      return Util.evalString(_cellspacingExpr);
    else
      return null;
  }

  public void setCellspacing(String value)
  {
    _cellspacing = value;
  }
  
  public String getColumnClasses()
  {
    if (_columnClasses != null)
      return _columnClasses;
    else if (_columnClassesExpr != null)
      return Util.evalString(_columnClassesExpr);
    else
      return null;
  }

  public void setColumnClasses(String value)
  {
    _columnClasses = value;
  }

  public int getColumns()
  {
    if (_columns != null)
      return _columns;
    else if (_columnsExpr != null)
      return Util.evalInt(_columnsExpr);
    else
      return Integer.MIN_VALUE;
  }

  public void setColumns(int value)
  {
    _columns = value;
  }
  
  public String getDir()
  {
    if (_dir != null)
      return _dir;
    else if (_dirExpr != null)
      return Util.evalString(_dirExpr);
    else
      return null;
  }

  public void setDir(String value)
  {
    _dir = value;
  }
  
  public String getFooterClass()
  {
    if (_footerClass != null)
      return _footerClass;
    else if (_footerClassExpr != null)
      return Util.evalString(_footerClassExpr);
    else
      return null;
  }

  public void setFooterClass(String value)
  {
    _footerClass = value;
  }
  
  public String getFrame()
  {
    if (_frame != null)
      return _frame;
    else if (_frameExpr != null)
      return Util.evalString(_frameExpr);
    else
      return null;
  }

  public void setFrame(String value)
  {
    _frame = value;
  }
  
  public String getHeaderClass()
  {
    if (_headerClass != null)
      return _headerClass;
    else if (_headerClassExpr != null)
      return Util.evalString(_headerClassExpr);
    else
      return null;
  }

  public void setHeaderClass(String value)
  {
    _headerClass = value;
  }
  
  public String getLang()
  {
    if (_lang != null)
      return _lang;
    else if (_langExpr != null)
      return Util.evalString(_langExpr);
    else
      return null;
  }

  public void setLang(String value)
  {
    _lang = value;
  }
  
  public String getOnclick()
  {
    if (_onclick != null)
      return _onclick;
    else if (_onclickExpr != null)
      return Util.evalString(_onclickExpr);
    else
      return null;
  }

  public void setOnclick(String value)
  {
    _onclick = value;
  }
  
  public String getOndblclick()
  {
    if (_ondblclick != null)
      return _ondblclick;
    else if (_ondblclickExpr != null)
      return Util.evalString(_ondblclickExpr);
    else
      return null;
  }

  public void setOndblclick(String value)
  {
    _ondblclick = value;
  }
  
  public String getOnkeydown()
  {
    if (_onkeydown != null)
      return _onkeydown;
    else if (_onkeydownExpr != null)
      return Util.evalString(_onkeydownExpr);
    else
      return null;
  }

  public void setOnkeydown(String value)
  {
    _onkeydown = value;
  }
  
  public String getOnkeypress()
  {
    if (_onkeypress != null)
      return _onkeypress;
    else if (_onkeypressExpr != null)
      return Util.evalString(_onkeypressExpr);
    else
      return null;
  }

  public void setOnkeypress(String value)
  {
    _onkeypress = value;
  }
  
  public String getOnkeyup()
  {
    if (_onkeyup != null)
      return _onkeyup;
    else if (_onkeyupExpr != null)
      return Util.evalString(_onkeyupExpr);
    else
      return null;
  }

  public void setOnkeyup(String value)
  {
    _onkeyup = value;
  }
  
  public String getOnmousedown()
  {
    if (_onmousedown != null)
      return _onmousedown;
    else if (_onmousedownExpr != null)
      return Util.evalString(_onmousedownExpr);
    else
      return null;
  }

  public void setOnmousedown(String value)
  {
    _onmousedown = value;
  }
  
  public String getOnmousemove()
  {
    if (_onmousemove != null)
      return _onmousemove;
    else if (_onmousemoveExpr != null)
      return Util.evalString(_onmousemoveExpr);
    else
      return null;
  }

  public void setOnmousemove(String value)
  {
    _onmousemove = value;
  }
  
  public String getOnmouseout()
  {
    if (_onmouseout != null)
      return _onmouseout;
    else if (_onmouseoutExpr != null)
      return Util.evalString(_onmouseoutExpr);
    else
      return null;
  }

  public void setOnmouseout(String value)
  {
    _onmouseout = value;
  }
  
  public String getOnmouseover()
  {
    if (_onmouseover != null)
      return _onmouseover;
    else if (_onmouseoverExpr != null)
      return Util.evalString(_onmouseoverExpr);
    else
      return null;
  }

  public void setOnmouseover(String value)
  {
    _onmouseover = value;
  }
  
  public String getOnmouseup()
  {
    if (_onmouseup != null)
      return _onmouseup;
    else if (_onmouseupExpr != null)
      return Util.evalString(_onmouseupExpr);
    else
      return null;
  }

  public void setOnmouseup(String value)
  {
    _onmouseup = value;
  }
  
  public String getRowClasses()
  {
    if (_rowClasses != null)
      return _rowClasses;
    else if (_rowClassesExpr != null)
      return Util.evalString(_rowClassesExpr);
    else
      return null;
  }

  public void setRowClasses(String value)
  {
    _rowClasses = value;
  }
  
  public String getRules()
  {
    if (_rules != null)
      return _rules;
    else if (_rulesExpr != null)
      return Util.evalString(_rulesExpr);
    else
      return null;
  }

  public void setRules(String value)
  {
    _rules = value;
  }
  
  public String getStyle()
  {
    if (_style != null)
      return _style;
    else if (_styleExpr != null)
      return Util.evalString(_styleExpr);
    else
      return null;
  }

  public void setStyle(String value)
  {
    _style = value;
  }
  
  public String getStyleClass()
  {
    if (_styleClass != null)
      return _styleClass;
    else if (_styleClassExpr != null)
      return Util.evalString(_styleClassExpr);
    else
      return null;
  }

  public void setStyleClass(String value)
  {
    _styleClass = value;
  }
  
  public String getSummary()
  {
    if (_summary != null)
      return _summary;
    else if (_summaryExpr != null)
      return Util.evalString(_summaryExpr);
    else
      return null;
  }

  public void setSummary(String value)
  {
    _summary = value;
  }
  
  public String getTitle()
  {
    if (_title != null)
      return _title;
    else if (_titleExpr != null)
      return Util.evalString(_titleExpr);
    else
      return null;
  }

  public void setTitle(String value)
  {
    _title = value;
  }
  
  public String getWidth()
  {
    if (_width != null)
      return _width;
    else if (_widthExpr != null)
      return Util.evalString(_widthExpr);
    else
      return null;
  }

  public void setWidth(String value)
  {
    _width = value;
  }

  //
  // value expression override
  //

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    PropEnum prop = _propMap.get(name);

    if (prop != null) {
      switch (prop) {
      case BGCOLOR:
	return _bgcolorExpr;
      case BORDER:
	return _borderExpr;
      case CAPTION_CLASS:
	return _captionClassExpr;
      case CAPTION_STYLE:
	return _captionStyleExpr;
      case CELLPADDING:
	return _cellpaddingExpr;
      case CELLSPACING:
	return _cellspacingExpr;
      case COLUMN_CLASSES:
	return _columnClassesExpr;
      case COLUMNS:
	return _columnsExpr;
      case DIR:
	return _dirExpr;
      case FOOTER_CLASS:
	return _footerClassExpr;
      case FRAME:
	return _frameExpr;
      case HEADER_CLASS:
	return _headerClassExpr;
      case LANG:
	return _langExpr;
      case ONCLICK:
	return _onclickExpr;
      case ONDBLCLICK:
	return _ondblclickExpr;
      case ONKEYDOWN:
	return _onkeydownExpr;
      case ONKEYPRESS:
	return _onkeypressExpr;
      case ONKEYUP:
	return _onkeyupExpr;
      case ONMOUSEDOWN:
	return _onmousedownExpr;
      case ONMOUSEMOVE:
	return _onmousemoveExpr;
      case ONMOUSEOUT:
	return _onmouseoutExpr;
      case ONMOUSEOVER:
	return _onmouseoverExpr;
      case ONMOUSEUP:
	return _onmouseupExpr;
      case ROW_CLASSES:
	return _rowClassesExpr;
      case RULES:
	return _rulesExpr;
      case STYLE:
	return _styleExpr;
      case STYLE_CLASS:
	return _styleClassExpr;
      case SUMMARY:
	return _summaryExpr;
      case TITLE:
	return _titleExpr;
      case WIDTH:
	return _widthExpr;
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
      case BGCOLOR:
	if (expr != null && expr.isLiteralText())
	  _bgcolor = Util.evalString(expr);
	else
	  _bgcolorExpr = expr;
	return;
	
      case BORDER:
	if (expr != null && expr.isLiteralText())
	  _border = Util.evalInt(expr);
	else
	  _borderExpr = expr;
	return;
	
      case CAPTION_CLASS:
	if (expr != null && expr.isLiteralText())
	  _captionClass = Util.evalString(expr);
	else
	  _captionClassExpr = expr;
	return;
	
      case CAPTION_STYLE:
	if (expr != null && expr.isLiteralText())
	  _captionStyle = Util.evalString(expr);
	else
	  _captionStyleExpr = expr;
	return;
	
      case CELLPADDING:
	if (expr != null && expr.isLiteralText())
	  _cellpadding = Util.evalString(expr);
	else
	  _cellpaddingExpr = expr;
	return;
	
      case CELLSPACING:
	if (expr != null && expr.isLiteralText())
	  _cellspacing = Util.evalString(expr);
	else
	  _cellspacingExpr = expr;
	return;
	
      case COLUMN_CLASSES:
	if (expr != null && expr.isLiteralText())
	  _columnClasses = Util.evalString(expr);
	else
	  _columnClassesExpr = expr;
	return;
	
      case COLUMNS:
	if (expr != null && expr.isLiteralText())
	  _columns = Util.evalInt(expr);
	else
	  _columnsExpr = expr;
	return;

      case DIR:
	if (expr != null && expr.isLiteralText())
	  _dir = Util.evalString(expr);
	else
	  _dirExpr = expr;
	return;
	
      case FOOTER_CLASS:
	if (expr != null && expr.isLiteralText())
	  _footerClass = Util.evalString(expr);
	else
	  _footerClassExpr = expr;
	return;
	
      case FRAME:
	if (expr != null && expr.isLiteralText())
	  _frame = Util.evalString(expr);
	else
	  _frameExpr = expr;
	return;
	
      case HEADER_CLASS:
	if (expr != null && expr.isLiteralText())
	  _headerClass = Util.evalString(expr);
	else
	  _headerClassExpr = expr;
	return;
	
      case LANG:
	if (expr != null && expr.isLiteralText())
	  _lang = Util.evalString(expr);
	else
	  _langExpr = expr;
	return;
	
      case ONCLICK:
	if (expr != null && expr.isLiteralText())
	  _onclick = Util.evalString(expr);
	else
	  _onclickExpr = expr;
	return;

      case ONDBLCLICK:
	if (expr != null && expr.isLiteralText())
	  _ondblclick = Util.evalString(expr);
	else
	  _ondblclickExpr = expr;
	return;

      case ONKEYDOWN:
	if (expr != null && expr.isLiteralText())
	  _onkeydown = Util.evalString(expr);
	else
	  _onkeydownExpr = expr;
	return;
	
      case ONKEYPRESS:
	if (expr != null && expr.isLiteralText())
	  _onkeypress = Util.evalString(expr);
	else
	  _onkeypressExpr = expr;
	return;

      case ONKEYUP:
	if (expr != null && expr.isLiteralText())
	  _onkeyup = Util.evalString(expr);
	else
	  _onkeyupExpr = expr;
	return;

      case ONMOUSEDOWN:
	if (expr != null && expr.isLiteralText())
	  _onmousedown = Util.evalString(expr);
	else
	  _onmousedownExpr = expr;
	return;

      case ONMOUSEMOVE:
	if (expr != null && expr.isLiteralText())
	  _onmousemove = Util.evalString(expr);
	else
	  _onmousemoveExpr = expr;
	return;

      case ONMOUSEOUT:
	if (expr != null && expr.isLiteralText())
	  _onmouseout = Util.evalString(expr);
	else
	  _onmouseoutExpr = expr;
	return;

      case ONMOUSEOVER:
	if (expr != null && expr.isLiteralText())
	  _onmouseover = Util.evalString(expr);
	else
	  _onmouseoverExpr = expr;
	return;

      case ONMOUSEUP:
	if (expr != null && expr.isLiteralText())
	  _onmouseup = Util.evalString(expr);
	else
	  _onmouseupExpr = expr;
	return;

      case ROW_CLASSES:
	if (expr != null && expr.isLiteralText())
	  _rowClasses = Util.evalString(expr);
	else
	  _rowClassesExpr = expr;
	return;

      case RULES:
	if (expr != null && expr.isLiteralText())
	  _rules = Util.evalString(expr);
	else
	  _rulesExpr = expr;
	return;

      case STYLE:
	if (expr != null && expr.isLiteralText())
	  _style = Util.evalString(expr);
	else
	  _styleExpr = expr;
	return;

      case STYLE_CLASS:
	if (expr != null && expr.isLiteralText())
	  _styleClass = Util.evalString(expr);
	else
	  _styleClassExpr = expr;
	return;

      case SUMMARY:
	if (expr != null && expr.isLiteralText())
	  _summary = Util.evalString(expr);
	else
	  _summaryExpr = expr;
	return;

      case TITLE:
	if (expr != null && expr.isLiteralText())
	  _title = Util.evalString(expr);
	else
	  _titleExpr = expr;
	return;

      case WIDTH:
	if (expr != null && expr.isLiteralText())
	  _width = Util.evalString(expr);
	else
	  _widthExpr = expr;
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
    Object parent = super.saveState(context);

    return new Object[] {
      parent,
      _bgcolor,
      Util.save(_bgcolorExpr, context),
      _border,
      Util.save(_borderExpr, context),
      _captionClass,
      Util.save(_captionClassExpr, context),
      _captionStyle,
      Util.save(_captionStyleExpr, context),
      _cellpadding,
      Util.save(_cellpaddingExpr, context),
      _cellspacing,
      Util.save(_cellspacingExpr, context),
      _columnClasses,
      Util.save(_columnClassesExpr, context),
      _columns,
      Util.save(_columnsExpr, context),
      _dir,
      Util.save(_dirExpr, context),
      _footerClass,
      Util.save(_footerClassExpr, context),
      _frame,
      Util.save(_frameExpr, context),
      _headerClass,
      Util.save(_headerClassExpr, context),
      _lang,
      Util.save(_langExpr, context),
      
      _onclick,
      Util.save(_onclickExpr, context),
      _ondblclick,
      Util.save(_ondblclickExpr, context),
      _onkeydown,
      Util.save(_onkeydownExpr, context),
      _onkeypress,
      Util.save(_onkeypressExpr, context),
      _onkeyup,
      Util.save(_onkeyupExpr, context),
      
      _onmousedown,
      Util.save(_onmousedownExpr, context),
      _onmousemove,
      Util.save(_onmousemoveExpr, context),
      _onmouseout,
      Util.save(_onmouseoutExpr, context),
      _onmouseover,
      Util.save(_onmouseoverExpr, context),
      _onmouseup,
      Util.save(_onmouseupExpr, context),
      
      _rowClasses,
      Util.save(_rowClassesExpr, context),
      _rules,
      Util.save(_rulesExpr, context),
      
      _style,
      Util.save(_styleExpr, context),
      _styleClass,
      Util.save(_styleClassExpr, context),
      _summary,
      Util.save(_summaryExpr, context),
      _title,
      Util.save(_titleExpr, context),
      _width,
      Util.save(_widthExpr, context),
    };
  }

  public void restoreState(FacesContext context, Object value)
  {
    Object []state = (Object []) value;

    int i = 0;

    if (state != null) 
      super.restoreState(context, state[i++]);

    _bgcolor = (String) state[i++];
    _bgcolorExpr = Util.restoreString(state[i++], context);

    _border = (Integer) state[i++];
    _borderExpr = Util.restoreInt(state[i++], context);

    _captionClass = (String) state[i++];
    _captionClassExpr = Util.restoreString(state[i++], context);

    _captionStyle = (String) state[i++];
    _captionStyleExpr = Util.restoreString(state[i++], context);

    _cellpadding = (String) state[i++];
    _cellpaddingExpr = Util.restoreString(state[i++], context);

    _cellspacing = (String) state[i++];
    _cellspacingExpr = Util.restoreString(state[i++], context);

    _columnClasses = (String) state[i++];
    _columnClassesExpr = Util.restoreString(state[i++], context);

    _columns = (Integer) state[i++];
    _columnsExpr = Util.restoreInt(state[i++], context);

    _dir = (String) state[i++];
    _dirExpr = Util.restoreString(state[i++], context);

    _footerClass = (String) state[i++];
    _footerClassExpr = Util.restoreString(state[i++], context);

    _frame = (String) state[i++];
    _frameExpr = Util.restoreString(state[i++], context);

    _headerClass = (String) state[i++];
    _headerClassExpr = Util.restoreString(state[i++], context);

    _lang = (String) state[i++];
    _langExpr = Util.restoreString(state[i++], context);

    _onclick = (String) state[i++];
    _onclickExpr = Util.restoreString(state[i++], context);

    _ondblclick = (String) state[i++];
    _ondblclickExpr = Util.restoreString(state[i++], context);

    _onkeydown = (String) state[i++];
    _onkeydownExpr = Util.restoreString(state[i++], context);

    _onkeypress = (String) state[i++];
    _onkeypressExpr = Util.restoreString(state[i++], context);

    _onkeyup = (String) state[i++];
    _onkeyupExpr = Util.restoreString(state[i++], context);

    _onmousedown = (String) state[i++];
    _onmousedownExpr = Util.restoreString(state[i++], context);

    _onmousemove = (String) state[i++];
    _onmousemoveExpr = Util.restoreString(state[i++], context);

    _onmouseout = (String) state[i++];
    _onmouseoutExpr = Util.restoreString(state[i++], context);

    _onmouseover = (String) state[i++];
    _onmouseoverExpr = Util.restoreString(state[i++], context);

    _onmouseup = (String) state[i++];
    _onmouseupExpr = Util.restoreString(state[i++], context);

    _rowClasses = (String) state[i++];
    _rowClassesExpr = Util.restoreString(state[i++], context);

    _rules = (String) state[i++];
    _rulesExpr = Util.restoreString(state[i++], context);

    _style = (String) state[i++];
    _styleExpr = Util.restoreString(state[i++], context);

    _styleClass = (String) state[i++];
    _styleClassExpr = Util.restoreString(state[i++], context);

    _summary = (String) state[i++];
    _summaryExpr = Util.restoreString(state[i++], context);

    _title = (String) state[i++];
    _titleExpr = Util.restoreString(state[i++], context);

    _width = (String) state[i++];
    _widthExpr = Util.restoreString(state[i++], context);
  }

  //
  // utility
  //

  private enum PropEnum {
    BGCOLOR,
    BORDER,
    CAPTION_CLASS,
    CAPTION_STYLE,
    CELLPADDING,
    CELLSPACING,
    COLUMN_CLASSES,
    COLUMNS,
    DIR,
    FOOTER_CLASS,
    FRAME,
    HEADER_CLASS,
    LANG,
    ONCLICK,
    ONDBLCLICK,
    ONKEYDOWN,
    ONKEYPRESS,
    ONKEYUP,
    ONMOUSEDOWN,
    ONMOUSEMOVE,
    ONMOUSEOUT,
    ONMOUSEOVER,
    ONMOUSEUP,
    ROW_CLASSES,
    RULES,
    STYLE,
    STYLE_CLASS,
    SUMMARY,
    TITLE,
    WIDTH,
  }

  static {
    _propMap.put("bgcolor", PropEnum.BGCOLOR);
    _propMap.put("border", PropEnum.BORDER);
    _propMap.put("captionClass", PropEnum.CAPTION_CLASS);
    _propMap.put("captionStyle", PropEnum.CAPTION_STYLE);
    _propMap.put("cellpadding", PropEnum.CELLPADDING);
    _propMap.put("cellspacing", PropEnum.CELLSPACING);
    _propMap.put("columnClasses", PropEnum.COLUMN_CLASSES);
    _propMap.put("columns", PropEnum.COLUMNS);
    _propMap.put("dir", PropEnum.DIR);
    _propMap.put("footerClass", PropEnum.FOOTER_CLASS);
    _propMap.put("frame", PropEnum.FRAME);
    _propMap.put("headerClass", PropEnum.HEADER_CLASS);
    _propMap.put("lang", PropEnum.LANG);
    _propMap.put("onclick", PropEnum.ONCLICK);
    _propMap.put("ondblclick", PropEnum.ONDBLCLICK);
    _propMap.put("onkeydown", PropEnum.ONKEYDOWN);
    _propMap.put("onkeypress", PropEnum.ONKEYPRESS);
    _propMap.put("onkeyup", PropEnum.ONKEYUP);
    _propMap.put("onmousedown", PropEnum.ONMOUSEDOWN);
    _propMap.put("onmousemove", PropEnum.ONMOUSEMOVE);
    _propMap.put("onmouseover", PropEnum.ONMOUSEOVER);
    _propMap.put("onmouseout", PropEnum.ONMOUSEOUT);
    _propMap.put("onmouseup", PropEnum.ONMOUSEUP);
    _propMap.put("rowClasses", PropEnum.ROW_CLASSES);
    _propMap.put("rules", PropEnum.RULES);
    _propMap.put("style", PropEnum.STYLE);
    _propMap.put("styleClass", PropEnum.STYLE_CLASS);
    _propMap.put("summary", PropEnum.SUMMARY);
    _propMap.put("title", PropEnum.TITLE);
    _propMap.put("width", PropEnum.WIDTH);
  }
}
