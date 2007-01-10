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
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.convert.*;

final class Util
{
  static Object eval(ValueExpression expr)
  {
    try {
      return expr.getValue(currentELContext());
    } catch (ELException e) {
      throw new FacesException(e);
    }
  }
  
  static boolean evalBoolean(ValueExpression expr)
  {
    try {
      return (Boolean) expr.getValue(currentELContext());
    } catch (ELException e) {
      throw new FacesException(e);
    }
  }
  
  static int evalInt(ValueExpression expr)
  {
    try {
      return (Integer) expr.getValue(currentELContext());
    } catch (ELException e) {
      throw new FacesException(e);
    }
  }
  
  static String evalString(ValueExpression expr)
  {
    try {
      return String.valueOf(expr.getValue(currentELContext()));
    } catch (ELException e) {
      throw new FacesException(e);
    }
  }

  static String save(ValueExpression expr,
		     FacesContext context)
  {
    if (expr != null)
      return expr.getExpressionString();
    else
      return null;
  }

  static ValueExpression restore(Object value,
				 Class type,
				 FacesContext context)
  {
    if (value == null)
      return null;

    String expr = (String) value;

    Application app = context.getApplication();
    ExpressionFactory factory = app.getExpressionFactory();
    
    return factory.createValueExpression(context.getELContext(),
					 expr,
					 type);
  }

  static ELContext currentELContext()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();

    if (facesContext != null)
      return facesContext.getELContext();
    else
      return null;
  }
}
