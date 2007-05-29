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

package com.caucho.jsf.taglib;

import java.io.*;
import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.webapp.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.caucho.util.*;

public class ConvertDateTimeTag extends ConverterTag
{
  private ValueExpression _bindingExpr;
  
  private ValueExpression _dateStyleExpr;
  private ValueExpression _localeExpr;
  private ValueExpression _patternExpr;
  private ValueExpression _timeStyleExpr;
  private ValueExpression _timeZoneExpr;
  private ValueExpression _typeExpr;

  public void setBinding(ValueExpression expr)
  {
    _bindingExpr = expr;
  }

  public void setDateStyle(ValueExpression expr)
  {
    _dateStyleExpr = expr;
  }

  public void setLocale(ValueExpression expr)
  {
    _localeExpr = expr;
  }

  public void setPattern(ValueExpression expr)
  {
    _patternExpr = expr;
  }

  public void setTimeStyle(ValueExpression expr)
  {
    _timeStyleExpr = expr;
  }

  public void setTimeZone(ValueExpression expr)
  {
    _timeZoneExpr = expr;
  }

  public void setType(ValueExpression expr)
  {
    _typeExpr = expr;
  }
  
  protected Converter createConverter()
  {
    FacesContext context = FacesContext.getCurrentInstance();

    Application app = context.getApplication();

    DateTimeConverter converter = null;

    String id = "javax.faces.DateTime";

    ELContext elContext = context.getELContext();
      
    if (_bindingExpr != null)
      converter = (DateTimeConverter) _bindingExpr.getValue(elContext);

    if (converter == null) {
      converter = (DateTimeConverter) app.createConverter(id);

      if (_bindingExpr != null)
	_bindingExpr.setValue(elContext, converter);
    }

    String type = null;

    if (_typeExpr != null)
      type = (String) _typeExpr.getValue(elContext);

    if (type != null) {
    }
    else if (_dateStyleExpr != null && _timeStyleExpr != null)
      type = "both";
    else if (_dateStyleExpr != null)
      type = "date";
    else if (_timeStyleExpr != null)
      type = "time";

    if (type != null)
      converter.setType(type);

    if (_dateStyleExpr != null)
      converter.setDateStyle((String) _dateStyleExpr.getValue(elContext));

    if (_dateStyleExpr != null)
      converter.setTimeStyle((String) _timeStyleExpr.getValue(elContext));

    if (_localeExpr != null) {
      Object value = _localeExpr.getValue(elContext);

      if (value instanceof Locale)
	converter.setLocale((Locale) value);
      else if (value != null)
	converter.setLocale(new Locale(value.toString()));
    }

    if (_patternExpr != null) {
      converter.setPattern((String) _patternExpr.getValue(elContext));
    }

    if (_timeZoneExpr != null) {
      Object value = _timeZoneExpr.getValue(elContext);

      if (value instanceof TimeZone)
	converter.setTimeZone((TimeZone) value);
      else if (value != null)
	converter.setTimeZone(TimeZone.getTimeZone(value.toString()));
    }

    return converter;
  }
}
