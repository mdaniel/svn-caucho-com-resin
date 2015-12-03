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
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

public class FloatValidateFilter
  extends AbstractFilter
  implements ValidateFilter
{
  private static final L10N L = new L10N(FloatValidateFilter.class);

  @Override
  protected Value filterImpl(Env env, Value value,
                             int flags, ArrayValue options)
  {
    if (value == BooleanValue.TRUE) {
      return DoubleValue.create(1.0);
    }
    else if (value.isLong()) {
      return value.toDoubleValue();
    }
    else if (value.isDoubleConvertible()) {
      return value.toDoubleValue();
    }
    else {
      String originalStr = value.toStringValue(env).toString();
      String str = originalStr;

      if ((flags & FilterModule.FILTER_FLAG_ALLOW_THOUSAND) > 0) {
        str = str.replace(",", "");
      }

      if (options != null) {
        Value decimalSeparator = options.get(env.createString("decimal"));

        if (decimalSeparator.isString()) {
          if (decimalSeparator.length() > 1) {
            env.warning(L.l("decimal separator must be one char: {0}", decimalSeparator));

            return BooleanValue.FALSE;
          }

          str = str.replace(decimalSeparator.toString(), ".");
        }
      }

      if (str != originalStr) {
        value = env.createString(str);

        if (value.isDoubleConvertible()) {
          return value.toDoubleValue();
        }
      }

      return BooleanValue.FALSE;
    }
  }
}
