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
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import com.caucho.quercus.program.Arg;

/**
 * Represents a call to an object's method
 */
@SuppressWarnings("serial")
public class CallbackError extends Callback {
  private final String _errorString;

  public CallbackError(String errorString)
  {
    _errorString = errorString;
  }

  @Override
  public Value call(Env env, Value []args)
  {
    return NullValue.NULL;
  }

  @Override
  public boolean isValid(Env env)
  {
    return false;
  }

  @Override
  public boolean isInternal(Env env)
  {
    return false;
  }

  @Override
  public String getDeclFileName(Env env)
  {
    return null;
  }

  @Override
  public int getDeclStartLine(Env env)
  {
    return -1;
  }

  @Override
  public int getDeclEndLine(Env env)
  {
    return -1;
  }

  @Override
  public String getDeclComment(Env env)
  {
    return null;
  }

  @Override
  public boolean isReturnsReference(Env env)
  {
    return false;
  }

  @Override
  public Arg []getArgs(Env env)
  {
    return null;
  }

  @Override
  public String getCallbackName()
  {
    return _errorString;
  }
}
