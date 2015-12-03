/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Alex Rojkov
 */

package javax.faces.event;

import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;
import javax.el.MethodExpression;
import javax.el.ELContext;
import javax.el.ELException;

public class MethodExpressionValueChangeListener
  implements StateHolder, ValueChangeListener
{
  private MethodExpression _expression;
  private boolean _transient;

  public MethodExpressionValueChangeListener()
  {
  }

  public MethodExpressionValueChangeListener(MethodExpression expression)
  {
    _expression = expression;
  }

  public Object saveState(FacesContext context)
  {
    return _expression;
  }

  public void restoreState(FacesContext context, Object state)
  {
    _expression = (MethodExpression) state;
  }

  public boolean isTransient()
  {
    return _transient;
  }

  public void setTransient(boolean isTransient)
  {
    _transient = isTransient;
  }

  public void processValueChange(ValueChangeEvent event)
    throws AbortProcessingException
  {
    ELContext elContext = FacesContext.getCurrentInstance().getELContext();
    
    try {
      _expression.invoke(elContext, new Object []{event});
    }
    catch (ELException e) {
      Throwable t = e.getCause();
      if (t instanceof AbortProcessingException) {
        throw (AbortProcessingException) t;
      }
      throw e;
    }
  }
}
