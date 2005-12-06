/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.php.lib;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

import com.caucho.php.Quercus;

import com.caucho.php.module.PhpModule;
import com.caucho.php.module.AbstractPhpModule;
import com.caucho.php.module.Optional;

import com.caucho.php.env.Value;
import com.caucho.php.env.Env;
import com.caucho.php.env.NullValue;
import com.caucho.php.env.BooleanValue;
import com.caucho.php.env.ArrayValue;
import com.caucho.php.env.ArrayValueImpl;
import com.caucho.php.env.LongValue;
import com.caucho.php.env.DoubleValue;
import com.caucho.php.env.StringValue;
import com.caucho.php.env.VarMap;
import com.caucho.php.env.ObjectValue;
import com.caucho.php.env.ChainedMap;
import com.caucho.php.env.ResourceValue;

import com.caucho.php.program.PhpProgram;

import com.caucho.vfs.WriteStream;

/**
 * PHP class information
 */
public class PhpClassesModule extends AbstractPhpModule {
  private static final L10N L = new L10N(PhpClassesModule.class);
  private static final Logger log
    = Logger.getLogger(PhpClassesModule.class.getName());

  /**
   * Returns true if the class exists.
   */
  public boolean class_exists(Env env, String className)
  {
    return env.findClass(className) != null;
  }
  
  /**
   * Returns the object's class name
   */
  public Value get_class(Value value)
    throws Throwable
  {
    if (value instanceof ObjectValue) {
      ObjectValue obj = (ObjectValue) value;

      return new StringValue(obj.getName());
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the object's variables
   */
  public Value get_object_vars(Value obj)
    throws Throwable
  {
    // 
    ArrayValue result = new ArrayValueImpl();

    for (Value name : obj.getIndices()) {
      result.put(name, obj.get(name));
    }

    return result;
  }

  /**
   * Returns true if the argument is an object.
   */
  public static boolean is_object(Value value)
  {
    return value instanceof ObjectValue;
  }
}
