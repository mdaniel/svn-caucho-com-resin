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
 * @author Sam
 */

package com.caucho.quercus.program;

import com.caucho.quercus.env.ArrayDelegate;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.JavaInvoker;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;

/**
 * A delegate that performs Array operations for Quercus objects.
 */
public class FunctionArrayDelegate implements ArrayDelegate {
  private JavaInvoker _arrayGet;
  private JavaInvoker _arrayPut;
  private JavaInvoker _arrayCount;

  public FunctionArrayDelegate()
  {
  }

  /**
   * Sets the custom function for the array get.
   */
  public void setArrayGet(JavaInvoker arrayGet)
  {
    _arrayGet = arrayGet;
  }

  /**
   * Sets the custom function for the array set.
   */
  public void setArrayPut(JavaInvoker arrayPut)
  {
    _arrayPut = arrayPut;
  }

  /**
   * Sets the custom function for the array set.
   */
  public void setArrayCount(JavaInvoker arrayCount)
  {
    _arrayCount = arrayCount;
  }

  /**
   * Returns the value for the specified key.
   */
  @Override
  public Value get(Env env, ObjectValue qThis, Value key)
  {
    if (_arrayGet != null) {
      return _arrayGet.callMethod(env,
                                  _arrayGet.getQuercusClass(),
                                  qThis,
                                  new Value[] { key });
    }
    else
      return UnsetValue.UNSET;
  }

  /**
   * Sets the value for the spoecified key.
   */
  @Override
  public Value put(Env env, ObjectValue qThis, Value key, Value value)
  {
    if (_arrayPut != null)
      return _arrayPut.callMethod(env,
                                  _arrayPut.getQuercusClass(),
                                  qThis, key, value);
    else
      return UnsetValue.UNSET;
  }

  /**
   * Appends a value.
   */
  @Override
  public Value put(Env env, ObjectValue qThis, Value value)
  {
    if (_arrayPut != null)
      return _arrayPut.callMethod(env,
                                  _arrayPut.getQuercusClass(),
                                  qThis, value);
    else
      return UnsetValue.UNSET;
  }

  /**
   * Returns true if the value is set
   */
  @Override
  public boolean isset(Env env, ObjectValue qThis, Value key)
  {
    return get(env, qThis, key).isset();
  }

  @Override
  public boolean isEmpty(Env env, ObjectValue qThis, Value key)
  {
    return get(env, qThis, key).isEmpty();
  }

  /**
   * Removes the value at the speified key.
   */
  @Override
  public Value unset(Env env, ObjectValue qThis, Value key)
  {
    return UnsetValue.UNSET;
  }

  /**
   * Returns the value for the specified key.
   */
  @Override
  public long count(Env env, ObjectValue qThis)
  {
    if (_arrayCount!= null) {
      return _arrayCount .callMethod(env,
                                     _arrayGet.getQuercusClass(),
                                     qThis).toLong();
    }
    else
      return 1;
  }
}
