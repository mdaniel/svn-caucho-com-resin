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
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;

public class BooleanValidateFilter
  extends AbstractFilter
  implements ValidateFilter
{
  @Override
  protected Value filterImpl(Env env, Value value,
                             int flags, ArrayValue options)
  {
    // XXX: push this down to Value?

    if (value.isNull()) {
      if ((flags & FilterModule.FILTER_NULL_ON_FAILURE) > 0) {
        return NullValue.NULL;
      }
      else {
        return BooleanValue.FALSE;
      }
    }
    if (value.isBoolean()) {
      if ((flags & FilterModule.FILTER_NULL_ON_FAILURE) > 0
          && value == BooleanValue.FALSE) {
        return NullValue.NULL;
      }

      return value;
    }
    else if (value.isLong()) {
      return BooleanValue.create(value.toLong() == 1);
    }
    else if (value.isDouble()) {
      return BooleanValue.create(value.toDouble() == 1.0);
    }
    else if (value.isLongConvertible()) {
      return BooleanValue.create(value.toLong() == 1);
    }
    else {
      String str = value.toStringValue(env).toString();
      str = str.toLowerCase().trim();

      if ("on".equals(str) || "yes".equals(str) || "true".equals(str)) {
        return BooleanValue.TRUE;
      }
      else if ("off".equals(str) || "no".equals(str) || "false".equals(str)) {
        return BooleanValue.FALSE;
      }
      else if ((flags & FilterModule.FILTER_NULL_ON_FAILURE) > 0) {
        return NullValue.NULL;
      }
      else {
        return BooleanValue.FALSE;
      }
    }
  }
}
