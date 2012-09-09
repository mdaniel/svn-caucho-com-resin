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

package com.caucho.quercus.lib.filter;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

public class AbstractFilter implements Filter
{
  public Value filter(Env env, Value value, Value flagsV)
  {
    ArrayValue options = null;
    int flags = 0;

    if (flagsV.isNull()) {
    }
    else if (flagsV.isArray()) {
      ArrayValue array = flagsV.toArrayValue(env);

      Value optionsV = array.get(env.createString("options"));

      if (optionsV.isArray()) {
        options = optionsV.toArrayValue(env);
      }

      flags = array.get(env.createString("flags")).toInt();
    }
    else {
      flags = flagsV.toInt();
    }

    return filterImpl(env, value, flags, options);
  }

  protected Value filterImpl(Env env, Value value, int flags, ArrayValue options)
  {
    throw new UnsupportedOperationException();
  }
}
