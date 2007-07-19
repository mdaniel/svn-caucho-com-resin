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

/**
 * Represents a 16-bit unicode string value.
 */
abstract public class UnicodeValue extends StringValue {
  /**
   * Convert to a unicode value.
   */
  @Override
  public UnicodeValue toUnicodeValue(Env env)
  {
    return this;
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    StringBuilderValue sb = new StringBuilderValue();

    sb.append(this);

    return sb;
  }

  /**
   * Returns true for UnicodeValue
   */
  @Override
  public boolean isUnicode()
  {
    return true;
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    rValue = rValue.toValue();

    if (rValue.isUnicode()) {
      if (equals(rValue))
        return true;
    }
    else if (rValue.isBinary()) {
      if (equals(rValue.toUnicodeValue(Env.getInstance())))
        return true;
    }
    else if (rValue instanceof LongValue) {
      return toLong() == rValue.toLong();
    }
    else if (rValue instanceof DoubleValue) {
      return toDouble() == rValue.toDouble();
    }
    else if (rValue instanceof BooleanValue) {
      return toBoolean() == rValue.toBoolean();
    }

    if (isNumberConvertible() && rValue.isNumberConvertible())
      return toDouble() == rValue.toDouble();
    else
      return equals(rValue.toStringValue());
    
    /*
    int type = getNumericType();

    if (type == IS_STRING) {
      if (rValue.isUnicode())
        return equals(rValue);
      else if (rValue.isBinary())
        return equals(rValue.toUnicodeValue(Env.getInstance()));
      else if (rValue.isLongConvertible())
        return toLong() ==  rValue.toLong();
      else if (rValue instanceof BooleanValue)
        return toLong() == rValue.toLong();
      else
        return equals(rValue.toStringValue());
    }
    else if (rValue.isNumberConvertible())
      return toDouble() == rValue.toDouble();
    else
      return equals(rValue.toStringValue());
    */
  }

}

