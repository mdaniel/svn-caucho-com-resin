/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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
 * Abstract implementation of an expression.
 */
abstract public class AbstractExp extends QueryEval implements QueryExp {
  /**
   * Returns true for equality.
   */
  protected boolean eq(ValueExp v1, ValueExp v2)
    throws BadBinaryOpValueExpException
  {
    if (v1 instanceof StringValueExp &&
	v2 instanceof StringValueExp)
      return toString(v1).equals(toString(v2));
    else if (v1 instanceof BooleanValueExp &&
	     v2 instanceof BooleanValueExp)
      return toBoolean(v1) == toBoolean(v2);
    else if (v1 instanceof NumericValueExp &&
	     v2 instanceof NumericValueExp)
      return toDouble(v1) == toDouble(v2);
    else
      throw new BadBinaryOpValueExpException(v1);
  }
  /**
   * Returns true for less-than
   */
  protected boolean lt(ValueExp v1, ValueExp v2)
    throws BadBinaryOpValueExpException
  {
    if (v1 instanceof StringValueExp &&
	v2 instanceof StringValueExp)
      return toString(v1).compareTo(toString(v2)) < 0;
    else if (v1 instanceof BooleanValueExp &&
	     v2 instanceof BooleanValueExp) {
      boolean b1 = toBoolean(v1);
      boolean b2 = toBoolean(v2);
      
      return ! b1 && b2;
    }
    else if (v1 instanceof NumericValueExp &&
	     v2 instanceof NumericValueExp)
      return toDouble(v1) < toDouble(v2);
    else
      throw new BadBinaryOpValueExpException(v1);
  }
  
  /**
   * Converts to a string value.
   */
  public static String toString(ValueExp exp)
    throws BadBinaryOpValueExpException
  {
    if (exp instanceof StringValueExp)
      return ((StringValueExp) exp).getValue();
    else if (exp instanceof AbstractValueExp)
      return ((AbstractValueExp) exp).getString();
    else
      throw new BadBinaryOpValueExpException(exp);
  }
  
  /**
   * Converts to a long value.
   */
  public static long toLong(ValueExp exp)
    throws BadBinaryOpValueExpException
  {
    if (exp instanceof StringValueExp)
      return Long.parseLong(((StringValueExp) exp).getValue());
    else if (exp instanceof AbstractValueExp)
      return ((AbstractValueExp) exp).getLong();
    else
      throw new BadBinaryOpValueExpException(exp);
  }
  /**
   * Converts to a long value.
   */
  public static boolean toBoolean(ValueExp exp)
    throws BadBinaryOpValueExpException
  {
    if (exp instanceof StringValueExp) {
      String value = ((StringValueExp) exp).getValue();
      
      return value != null && ! value.equals("");
    }
    else if (exp instanceof AbstractValueExp)
      return ((AbstractValueExp) exp).getBoolean();
    else
      throw new BadBinaryOpValueExpException(exp);
  }
  
  /**
   * Converts to a double value.
   */
  public static double toDouble(ValueExp exp)
    throws BadBinaryOpValueExpException
  {
    if (exp instanceof StringValueExp)
      return Double.parseDouble(((StringValueExp) exp).getValue());
    else if (exp instanceof AbstractValueExp)
      return ((AbstractValueExp) exp).getDouble();
    else
      throw new BadBinaryOpValueExpException(exp);
  }
}
