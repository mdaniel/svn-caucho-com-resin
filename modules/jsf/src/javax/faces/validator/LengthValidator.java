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

package javax.faces.validator;

import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;

public class LengthValidator
  implements Validator, StateHolder
{
  public static final String MAXIMUM_MESSAGE_ID
    = "javax.faces.validator.LengthValidator.MAXIMUM";
  public static final String MINIMUM_MESSAGE_ID
    = "javax.faces.validator.LengthValidator.MINIMUM";
  public static final String VALIDATOR_ID
    = "javax.faces.Length";

  private int _minimum = 0;
  private int _maximum = Integer.MAX_VALUE;
  private boolean _isTransient;

  public LengthValidator()
  {
  }

  public LengthValidator(int maximum)
  {
    _maximum = maximum;
  }

  public LengthValidator(int maximum, int minimum)
  {
    _maximum = maximum;
    _minimum = minimum;
  }

  public int getMinimum()
  {
    return _minimum;
  }

  public void setMinimum(int min)
  {
    _minimum = min;
  }

  public int getMaximum()
  {
    return _maximum;
  }

  public void setMaximum(int max)
  {
    _maximum = max;
  }

  public boolean isTransient()
  {
    return _isTransient;
  }

  public void setTransient(boolean isTransient)
  {
    _isTransient = isTransient;
  }

  public void validate(FacesContext context,
		       UIComponent component,
		       Object value)
    throws ValidatorException
  {
    if (context == null || component == null)
      throw new NullPointerException();

    if (value == null)
      return;
    
    String str = value.toString();

    int len = str.length();

    if (len < getMinimum()) {
      throw Util.validationExn(context, MINIMUM_MESSAGE_ID, new Object[] { value });
    }

    if (getMaximum() < len) {
      throw Util.validationExn(context, MAXIMUM_MESSAGE_ID, new Object[] { value });
    }
  }

  public Object saveState(FacesContext context)
  {
    return new Object[] { _minimum, _maximum };
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object []stateV = (Object []) state;

    _minimum = (Integer) stateV[0];
    _maximum = (Integer) stateV[1];
  }

  public int hashCode()
  {
    return 65521 * (int) _minimum + (int) _maximum;
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof LengthValidator))
      return false;

    LengthValidator validator = (LengthValidator) o;

    return (_minimum == validator._minimum
	    && _maximum == validator._maximum);
  }
}
