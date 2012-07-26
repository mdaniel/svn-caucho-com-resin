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

import com.caucho.quercus.Location;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.lib.ErrorModule;

/**
 * Parent of PHP exceptions
 */
public class QuercusLanguageException extends QuercusException
{
  private static final StringValue FILE = new ConstStringValue("file");
  private static final StringValue LINE = new ConstStringValue("line");
  private static final StringValue MESSAGE = new ConstStringValue("message");

  private ArrayValue _trace;
  private Value _value;

  protected QuercusLanguageException(Env env)
  {
    _value = env.wrapJava(this);
  }

  public QuercusLanguageException(Value value)
  {
    super(value.toString());

    _value = value;
  }

  public Value toException(Env env)
  {
    // php/0g0j
    if (_value.isA("Exception"))
      return _value;
    else
      return env.wrapJava(this);
  }

  /**
   * Returns the value.
   */
  public Value getValue()
  {
    return _value;
  }

  /**
   * Converts the exception to a Value.
   */
  public Value toValue(Env env)
  {
    return _value;
  }

  /*
   * Returns the PHP exception message.  If null, returns the empty string.
   */
  public String getMessage(Env env)
  {
    Value field = _value.getField(env, MESSAGE);

    String msg;

    if (field != null)
      msg = field.toString();
    else
      msg = getMessage();

    return msg + env.getStackTraceAsString(getTrace(env), getLocation(env));
  }

  /**
   * Returns the location of this PHP exception.
   */
  public Location getLocation(Env env)
  {
    Value file = _value.getField(env, FILE);
    Value line = _value.getField(env, LINE);

    if (file.isNull() || line.isNull())
      return Location.UNKNOWN;
    else
      return new Location(file.toString(), line.toInt(), null, null);
  }

  public int getLine(Env env)
  {
    return getLocation(env).getLineNumber();
  }

  public String getFile(Env env)
  {
    return getLocation(env).getFileName();
  }

  public ArrayValue getTrace(Env env)
  {
    if (_trace == null) {
      _trace = ErrorModule.debug_backtrace_exception(env, this, 0);
    }

    return _trace;
  }
}
