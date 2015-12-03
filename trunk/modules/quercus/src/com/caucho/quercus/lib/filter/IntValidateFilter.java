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
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;

public class IntValidateFilter extends AbstractFilter implements ValidateFilter
{
  @Override
  protected Value filterImpl(Env env, Value value, int flags, ArrayValue options)
  {
    long min = Long.MIN_VALUE;
    long max = Long.MAX_VALUE;

    if (options != null) {
      Value minV = options.get(env.createString("min_range"));
      Value maxV = options.get(env.createString("max_range"));

      if (minV != UnsetValue.UNSET) {
        min = minV.toLong();
      }

      if (maxV != UnsetValue.UNSET) {
        max = maxV.toLong();
      }
    }

    if (! value.isLongConvertible() && value != BooleanValue.TRUE) {
      return BooleanValue.FALSE;
    }

    LongValue val =  value.toLongValue();
    long v = val.toLong();

    if (min <= v && v <= max) {
      return val;
    }
    else {
      return BooleanValue.FALSE;
    }
  }
}
