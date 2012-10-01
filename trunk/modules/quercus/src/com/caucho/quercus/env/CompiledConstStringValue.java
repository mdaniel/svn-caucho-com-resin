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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.env;

/**
 * Represents a StringValue that is never modified.
 * For compiled code.
 */
public final class CompiledConstStringValue
  extends ConstStringValue
{
  private final int _compiledHashCode;

  public CompiledConstStringValue(StringValue s)
  {
    super(s);

    setLongValue(s.toLongValue());
    setDoubleValue(s.toDoubleValue());
    setString(s.toString());

    setValueType(s.getValueType());
    _compiledHashCode = s.hashCode();
    setKey(s.toKey());
  }

  public CompiledConstStringValue(String s)
  {
    super(s);

    setLongValue(super.toLongValue());
    setDoubleValue(super.toDoubleValue());
    setString(s);
    setValueType(super.getValueType());
    _compiledHashCode = super.hashCode();
    setKey(super.toKey());
  }

  public CompiledConstStringValue(char ch,
                                  LongValue longValue,
                                  DoubleValue doubleValue,
                                  ValueType valueType,
                                  Value key,
                                  int hashCode)
  {
    super(ch);

    setString(String.valueOf(ch));
    setLongValue(longValue);
    setDoubleValue(doubleValue);

    setValueType(valueType);
    setKey(key);
    _compiledHashCode = hashCode;
  }

  public CompiledConstStringValue(char ch,
                                  LongValue longValue,
                                  DoubleValue doubleValue,
                                  ValueType valueType,
                                  int hashCode)
  {
    super(ch);

    setString(String.valueOf(ch));
    setLongValue(longValue);
    setDoubleValue(doubleValue);

    setValueType(valueType);
    setKey(super.toKey());
    _compiledHashCode = hashCode;
  }

  public CompiledConstStringValue(String s,
                                  LongValue longValue,
                                  DoubleValue doubleValue,
                                  ValueType valueType,
                                  Value key,
                                  int hashCode)
  {
    super(s);

    setString(s);
    setLongValue(longValue);
    setDoubleValue(doubleValue);
    setValueType(valueType);

    setKey(key);
    
    _compiledHashCode = hashCode;
  }

  public CompiledConstStringValue(String s,
                                  LongValue longValue,
                                  DoubleValue doubleValue,
                                  ValueType valueType,
                                  int hashCode)
  {
    super(s);

    setString(s);
    setLongValue(longValue);
    setDoubleValue(doubleValue);
    setValueType(valueType);

    setKey(super.toKey());
    _compiledHashCode = hashCode;
  }

  public boolean isStatic()
  {
    return true;
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return toLongValue().toLong();
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toDoubleValue().toDouble();
  }

  @Override
  public final int hashCode()
  {
    return _compiledHashCode;
  }
}
