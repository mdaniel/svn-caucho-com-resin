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

package com.caucho.quercus.lib;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.*;

/**
 * PHP math routines.
 */
public class QuercusBcmathModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusBcmathModule.class);

  private static BigDecimal toBigDecimal(Value value, BigDecimal invalid)
  {
    try {
      if (value.isNumber())
        return new BigDecimal(value.toDouble());
      else if (value instanceof StringValue)
        return new BigDecimal(value.toString());
      else
        return invalid;
    }
    catch (NumberFormatException ex) {
      return invalid;
    }
    catch (IllegalArgumentException ex) {
      return invalid;
    }
  }

  private static String toString(BigDecimal bd, int scale)
  {
    if (scale < 0)
      scale = 0;

    return bd.setScale(scale, RoundingMode.DOWN).toPlainString();
  }

  public static String bcadd(Value value1, Value value2, @Optional int scale)
  {
    BigDecimal bd1 = toBigDecimal(value1, BigDecimal.ZERO);
    BigDecimal bd2 = toBigDecimal(value2, BigDecimal.ZERO);

    return toString(bd1.add(bd2), scale);
  }
}
