/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package com.caucho.jmx.query;

import javax.management.*;

/**
 * Implementation of a less-than query
 */
public class InExp extends AbstractExp implements QueryExp {
  private ValueExp _test;
  private ValueExp []_valueList;

  /**
   * Creates a new equal query.
   */
  public InExp(ValueExp test, ValueExp []valueList)
  {
    _test = test;
    _valueList = valueList;
  }

  /**
   * Evaluates the expression to a boolean.
   *
   * @param name the object to test.
   *
   * @return true if the query is a match.
   */
  public boolean apply(ObjectName name)
    throws BadStringOperationException, BadBinaryOpValueExpException,
	   BadAttributeValueExpException, InvalidApplicationException
  {
    ValueExp test = _test.apply(name);

    for (int i = 0; i < _valueList.length; i++) {
      if (eq(test, _valueList[i].apply(name)))
	return true;
    }

    return false;
  }
  
  /**
   * Returns the expression for the query.
   */
  public String toString()
  {
    return "in(" + _test + ", " + _valueList + ")";
  }
}
