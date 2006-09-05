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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import com.caucho.quercus.Quercus;

/**
 * Represents a PHP string value.
 */
public final class InternStringValue extends StringValueImpl {
  private final int _hashCode;
  private final Value _key;
  private final int _type;

  public InternStringValue(String value)
  {
    super(value.intern());

    _hashCode = super.hashCode();
    _key = super.toKey();
    _type = super.getNumericType();
  }

  /**
   * Interns the string.
   */
  public InternStringValue intern(Quercus quercus)
  {
    return this;
  }

  /**
   * Returns true for a long
   */
  public final boolean isLong()
  {
    return _type == IS_LONG;
  }

  /**
   * Returns true for a double
   */
  public final boolean isDouble()
  {
    return _type == IS_DOUBLE;
  }

  /**
   * Returns true for a number
   */
  @Override
  public final boolean isNumber()
  {
    return _type != IS_STRING;
  }

  /**
   * Converts to a double.
   */
  protected int getNumericType()
  {
    return _type;
  }

  /**
   * Converts to a key.
   */
  public final Value toKey()
  {
    return _key;
  }

  /**
   * Returns the hash code.
   */
  public final int hashCode()
  {
    return _hashCode;
  }

  public String toInternString()
  {
    return toString();
  }
}

