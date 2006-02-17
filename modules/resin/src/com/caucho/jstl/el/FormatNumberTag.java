/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jstl.el;

import java.io.*;
import java.util.*;
import java.text.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.jstl.core.*;
import javax.servlet.jsp.jstl.fmt.*;
import javax.servlet.jsp.el.ELException;

import com.caucho.vfs.*;
import com.caucho.util.*;

import com.caucho.jsp.PageContextImpl;
import com.caucho.jsp.BodyContentImpl;

import com.caucho.el.*;

/**
 * Looks up an i18n message from a bundle and prints it.
 */
public class FormatNumberTag extends BodyTagSupport {
  private static L10N L = new L10N(FormatNumberTag.class);
  
  private Expr _valueExpr;
  private Expr _typeExpr;
  
  private Expr _patternExpr;
  
  private Expr _currencyCodeExpr;
  private Expr _currencySymbolExpr;
  
  private Expr _groupingUsedExpr;
  
  private Expr _maxIntegerDigitsExpr;
  private Expr _minIntegerDigitsExpr;
  private Expr _maxFractionDigitsExpr;
  private Expr _minFractionDigitsExpr;

  private String _var;
  private String _scope;

  /**
   * Sets the formatting value.
   *
   * @param value the JSP-EL expression for the value.
   */
  public void setValue(Expr value)
  {
    _valueExpr = value;
  }

  /**
   * Sets the formatting type.
   *
   * @param type the JSP-EL expression for the type.
   */
  public void setType(Expr type)
  {
    _typeExpr = type;
  }

  /**
   * Sets the number pattern.
   *
   * @param pattern the JSP-EL expression for the number pattern.
   */
  public void setPattern(Expr pattern)
  {
    _patternExpr = pattern;
  }

  /**
   * Sets the currency code.
   *
   * @param currencyCode the JSP-EL expression for the currency code.
   */
  public void setCurrencyCode(Expr currencyCode)
  {
    _currencyCodeExpr = currencyCode;
  }

  /**
   * Sets the currency symbol.
   *
   * @param currencySymbol the JSP-EL expression for the currency symbol.
   */
  public void setCurrencySymbol(Expr currencySymbol)
  {
    _currencySymbolExpr = currencySymbol;
  }

  /**
   * Sets the groupingUsed expression
   *
   * @param groupingUsed the JSP-EL expression for the grouping pattern.
   */
  public void setGroupingUsed(Expr groupingUsed)
  {
    _groupingUsedExpr = groupingUsed;
  }

  /**
   * Sets the minimum digits allowed in the integer portion.
   *
   * @param minIntegerDigits the JSP-EL expression for the digits.
   */
  public void setMinIntegerDigits(Expr minIntegerDigits)
  {
    _minIntegerDigitsExpr = minIntegerDigits;
  }

  /**
   * Sets the maximum digits allowed in the integer portion.
   *
   * @param maxIntegerDigits the JSP-EL expression for the digits.
   */
  public void setMaxIntegerDigits(Expr maxIntegerDigits)
  {
    _maxIntegerDigitsExpr = maxIntegerDigits;
  }

  /**
   * Sets the minimum digits allowed in the fraction portion.
   *
   * @param minFractionDigits the JSP-EL expression for the digits.
   */
  public void setMinFractionDigits(Expr minFractionDigits)
  {
    _minFractionDigitsExpr = minFractionDigits;
  }

  /**
   * Sets the maximum digits allowed in the fraction portion.
   *
   * @param maxFractionDigits the JSP-EL expression for the digits.
   */
  public void setMaxFractionDigits(Expr maxFractionDigits)
  {
    _maxFractionDigitsExpr = maxFractionDigits;
  }

  /**
   * Sets the variable name.
   *
   * @param var the variable name to store the value in.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the variable scope.
   *
   * @param scope the variable scope to store the value in.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Process the tag.
   */
  public int doEndTag()
    throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      
      JspWriter out = pageContext.getOut();

      double number;

      BodyContentImpl body = (BodyContentImpl) getBodyContent();

      if (_valueExpr != null)
        number = _valueExpr.evalDouble(pageContext);
      else if (body != null) {
	String value = body.getTrimString();

	if (! value.equals(""))
	  number = Double.parseDouble(value);
	else
	  number = 0.0;
      }
      else
        number = 0.0;
      
      if (Double.isNaN(number))
        number = 0;
      
      NumberFormat format = getFormat();

      String value;
      if (format != null)
        value = format.format(number);
      else
        value = String.valueOf(number);

      if (_var == null)
        out.print(value);
      else
        CoreSetTag.setValue(pageContext, _var, _scope, value);
    } catch (Exception e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }

  protected NumberFormat getFormat()
    throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      
      NumberFormat format = null;

      Locale locale = pageContext.getLocale();

      String type = null;
      if (_typeExpr != null)
	type = _typeExpr.evalString(pageContext);

      if (type == null || type.equals("") || type.equals("number")) {
	if (locale != null)
	  format = NumberFormat.getInstance(locale);
	else
	  format = NumberFormat.getInstance();

	DecimalFormat decimalFormat = (DecimalFormat) format;

	if (_patternExpr != null)
	  decimalFormat.applyPattern(_patternExpr.evalString(pageContext));
      }
      else if (type.equals("percent")) {
	if (locale != null)
	  format = NumberFormat.getPercentInstance(locale);
	else
	  format = NumberFormat.getPercentInstance();
      }
      else if (type.equals("currency")) {
	if (locale != null)
	  format = NumberFormat.getCurrencyInstance(locale);
	else
	  format = NumberFormat.getCurrencyInstance();

	if ((_currencyCodeExpr != null || _currencySymbolExpr != null) &&
	    format instanceof DecimalFormat) {
	  DecimalFormat dFormat = (DecimalFormat) format;
	  DecimalFormatSymbols dSymbols;

	  dSymbols = dFormat.getDecimalFormatSymbols();

	  if (_currencyCodeExpr != null && dSymbols != null)
	    dSymbols.setInternationalCurrencySymbol(_currencyCodeExpr.evalString(pageContext));

	  if (_currencySymbolExpr != null && dSymbols != null)
	    dSymbols.setCurrencySymbol(_currencySymbolExpr.evalString(pageContext));

	  dFormat.setDecimalFormatSymbols(dSymbols);
	}
      }
      else
	throw new JspException(L.l("unknown formatNumber type `{0}'",
				   type));

      if (_groupingUsedExpr != null)
	format.setGroupingUsed(_groupingUsedExpr.evalBoolean(pageContext));

      if (_minIntegerDigitsExpr != null)
	format.setMinimumIntegerDigits((int) _minIntegerDigitsExpr.evalLong(pageContext));
      
      if (_maxIntegerDigitsExpr != null)
	format.setMaximumIntegerDigits((int) _maxIntegerDigitsExpr.evalLong(pageContext));

      if (_minFractionDigitsExpr != null)
	format.setMinimumFractionDigits((int) _minFractionDigitsExpr.evalLong(pageContext));
      
      if (_maxFractionDigitsExpr != null)
	format.setMaximumFractionDigits((int) _maxFractionDigitsExpr.evalLong(pageContext));

      return format;
    } catch (Exception e) {
      throw new JspException(e);
    }
  }
}
