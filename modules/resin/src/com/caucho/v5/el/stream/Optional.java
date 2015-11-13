/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Paul Cowan
 */

package com.caucho.v5.el.stream;

import javax.el.*;

import com.caucho.v5.util.L10N;

/**
 * Interface for an optional result.
 */
public class Optional<T>
{
  private static final L10N L = new L10N(Optional.class);

  private T _value = null;

  public Optional()
  {

  }

  public Optional(T value)
  {
    if (value == null)
      throw new ELException(L.l("Optional requires a value"));

    _value = value;
  }

  public T get()
  {
    if (_value == null) {
      throw new ELException(L.l("Optional.get() without assigned value"));
    }

    return _value;
  }

  public void ifPresent(LambdaExpression consumer)
  {
    if (_value != null) {
      consumer.invoke(_value);
    }
  }

  // this method is undocumented in the spec but present in glassfish
  // and seems sensible
  public boolean isPresent()
  {
    return (_value != null);
  }

  public T orElse(T other)
  {
    if (_value != null) {
      return _value;
    }
    else {
      return other;
    }
  }

  public T orElseGet(LambdaExpression other)
  {
    if (_value != null) {
      return _value;
    }
    else {
      return (T) other.invoke();
    }
  }

  @Override
  public String toString()
  {
    return _value != null ? _value.toString() : "";
  }
}
