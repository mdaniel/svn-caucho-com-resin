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
abstract public class AbstractValueExp extends QueryEval implements ValueExp {
  /**
   * Returns a value expression the value expression matches the name.
   */
  public ValueExp apply(ObjectName name)
    throws BadStringOperationException,
    BadBinaryOpValueExpException,
    BadAttributeValueExpException,
    InvalidApplicationException
  {
    return this;
  }
  
  /**
   * Returns the value as a string.
   */
  public String getString()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns the value as a boolean.
   */
  public boolean getBoolean()
  {
    String value = getString();
    
    return value != null && value.equals("true");
  }
  
  /**
   * Returns the value as a long
   */
  public long getLong()
  {
    String value = getString();

    if (value == null)
      return 0;
    else
      return Long.parseLong(value);
  }
  
  /**
   * Returns the value as a double
   */
  public double getDouble()
  {
    String value = getString();

    if (value == null)
      return 0;
    else
      return Double.parseDouble(value);
  }
  
  /**
   * Converts to a string value.
   */
  public static String toString(ValueExp exp)
    throws BadBinaryOpValueExpException
  {
    return AbstractExp.toString(exp);
  }
  
  /**
   * Converts to a long value.
   */
  public static long toLong(ValueExp exp)
    throws BadBinaryOpValueExpException
  {
    return AbstractExp.toLong(exp);
  }
  
  /**
   * Converts to a long value.
   */
  public static boolean toBoolean(ValueExp exp)
    throws BadBinaryOpValueExpException
  {
    return AbstractExp.toBoolean(exp);
  }
  
  /**
   * Converts to a double value.
   */
  public static double toDouble(ValueExp exp)
    throws BadBinaryOpValueExpException
  {
    return AbstractExp.toDouble(exp);
  }
}
