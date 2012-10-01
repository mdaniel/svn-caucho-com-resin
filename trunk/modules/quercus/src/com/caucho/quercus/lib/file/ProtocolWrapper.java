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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;

public class ProtocolWrapper {
  private QuercusClass _qClass;

  protected ProtocolWrapper()
  {
  }

  public ProtocolWrapper(QuercusClass qClass)
  {
    _qClass = qClass;
  }

  public BinaryStream fopen(Env env, StringValue path, StringValue mode,
                            LongValue options)
  {
    return new WrappedStream(env, _qClass, path, mode, options);
  }

  public Value opendir(Env env, StringValue path, LongValue flags)
  {
    WrappedDirectory value = new WrappedDirectory(env, _qClass);

    if (! value.open(env, path, flags))
      return BooleanValue.FALSE;
    else
      return env.wrapJava(value);
  }

  public boolean unlink(Env env, StringValue path)
  {
    // php/1e23
    Value obj = _qClass.createObject(env);
    AbstractFunction function = _qClass.findFunction("unlink");

    if (function == null) {
      return false;
    }

    Value result = function.callMethod(env, _qClass, obj, path);

    return result.toBoolean();
  }

  public boolean rename(Env env, StringValue from, StringValue to)
  {
    // php/1e24
    Value obj = _qClass.createObject(env);
    AbstractFunction function = _qClass.findFunction("rename");

    if (function == null) {
      return false;
    }

    Value result = function.callMethod(env, _qClass, obj, from, to);

    return result.toBoolean();
  }

  public boolean mkdir(Env env,
                       StringValue path, LongValue mode, LongValue options)
  {
    // creating an uninitialized object makes no sense but it's here
    // to match PHP 5.3.8 behavior for drupal-7.12
    // php/1e22
    Value obj = _qClass.createObject(env);
    AbstractFunction function = _qClass.findFunction("mkdir");

    if (function == null) {
      return false;
    }

    Value result = function.callMethod(env, _qClass, obj, path, mode, options);

    return result.toBoolean();
  }

  public boolean rmdir(Env env, StringValue path, LongValue options)
  {
    // php/1e25
    Value obj = _qClass.createObject(env);
    AbstractFunction function = _qClass.findFunction("rmdir");

    if (function == null) {
      return false;
    }

    Value result = function.callMethod(env, _qClass, obj, path, options);

    return result.toBoolean();
  }

  public Value url_stat(Env env, StringValue path, LongValue flags)
  {
    // php/1e26
    Value obj = _qClass.createObject(env);
    AbstractFunction function = _qClass.findFunction("url_stat");

    if (function == null) {
      return BooleanValue.FALSE;
    }

    Value result = function.callMethod(env, _qClass, obj, path, flags);

    return result;
  }

  public boolean stream_metadata(Env env, StringValue path,
                                 LongValue options, Value arg)
  {
    Value obj = _qClass.createObject(env);
    AbstractFunction function = _qClass.findFunction("stream_metadata");

    if (function == null) {
      return false;
    }

    Value result = function.callMethod(env, _qClass, obj, path, options, arg);

    return result.toBoolean();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _qClass + "]";
  }
}
