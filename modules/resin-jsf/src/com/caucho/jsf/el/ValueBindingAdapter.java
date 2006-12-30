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

package com.caucho.jsf.el;

import javax.el.*;
import javax.faces.context.*;
import javax.faces.el.*;

public class ValueBindingAdapter extends ValueBinding
{
  private final ValueExpression _expr;

  public ValueBindingAdapter(ValueExpression expr)
  {
    _expr = expr;
  }
  
  @Deprecated
  public Object getValue(FacesContext context)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    return _expr.getValue(context.getELContext());
  }

  @Deprecated
  public void setValue(FacesContext context, Object value)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    _expr.setValue(context.getELContext(), value);
  }

  @Deprecated
  public boolean isReadOnly(FacesContext context)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    return _expr.isReadOnly(context.getELContext());
  }

  @Deprecated
  public Class getType(FacesContext context)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    return _expr.getType(context.getELContext());
  }

  @Deprecated
  public String getExpressionString()
  {
    return _expr.getExpressionString();
  }

  public String toString()
  {
    return "ValueBindingAdapter[" + _expr.getExpressionString() + "]";
  }
}
