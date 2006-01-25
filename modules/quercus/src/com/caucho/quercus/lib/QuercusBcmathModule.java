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
import java.math.MathContext;
import java.math.BigInteger;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.*;

/**
 * PHP math routines.
 */
public class QuercusBcmathModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusBcmathModule.class);

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final BigDecimal TWO = new BigDecimal(2);
  private static final int SQRT_MAX_ITERATIONS = 50;

  private static BigDecimal toBigDecimal(Value value)
  {
    try {
      if (value instanceof StringValue)
        return new BigDecimal(value.toString());
      if (value instanceof DoubleValue)
        return new BigDecimal(value.toDouble());
      else if (value instanceof LongValue)
        return new BigDecimal(value.toLong());
      else
        return new BigDecimal(value.toString());
    }
    catch (NumberFormatException ex) {
      return ZERO;
    }
    catch (IllegalArgumentException ex) {
      return ZERO;
    }
  }

  private static int calculateScale(Env env, int scale)
  {
    if (scale < 0) {
      Value iniValue = env.getIni("bcmath.scale");

      if (iniValue != null)
        scale = iniValue.toInt();
    }

    if (scale < 0)
      scale = 0;

    return scale;
  }

  public static boolean bcscale(Env env, int scale)
  {
    env.setIni("bcmath.scale", String.valueOf(scale));

    return true;
  }

  public static String bcadd(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    BigDecimal bd = bd1.add(bd2);

    bd = bd.setScale(scale, RoundingMode.DOWN);

    return bd.toPlainString();
  }

  public static String bcsub(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    BigDecimal bd = bd1.subtract(bd2);

    bd = bd.setScale(scale, RoundingMode.DOWN);

    return bd.toPlainString();
  }

  public static String bcmul(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    BigDecimal bd = bd1.multiply(bd2);

    // odd php special case for 0, scale is ignored:
    if (bd.compareTo(ZERO) == 0) {
      if (scale > 0)
        return "0.0";
      else
        return "0";
    }

    bd = bd.setScale(scale, RoundingMode.DOWN);
    bd = bd.stripTrailingZeros();

    return bd.toPlainString();
  }

  public static String bcdiv(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    if (bd2.compareTo(ZERO) == 0) {
      env.warning(L.l("division by zero"));
      return null;
    }

    BigDecimal result;

    if (scale > 0) {
      result = bd1.divide(bd2, scale + 2, RoundingMode.DOWN);
    }
    else {
      result = bd1.divide(bd2, 2, RoundingMode.DOWN);
    }

    result = result.setScale(scale, RoundingMode.DOWN);

    return result.toPlainString();
  }

  public static String bcpow(Env env, Value base, Value exp, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(base);
    BigDecimal bd2 = toBigDecimal(exp);

    if (bd2.scale() > 0)
      env.warning("fractional exponent not supported");

    int exponent = bd2.toBigInteger().intValue();

    if (exponent == 0)
      return "1";

    boolean isNeg;

    if (exponent < 0)  {
      isNeg = true;
      exponent *= -1;
    }
    else
      isNeg = false;

    BigDecimal bd = bd1.pow(exponent);

    if (isNeg)
      bd = ONE.divide(bd, scale + 2, RoundingMode.DOWN);

    bd = bd.setScale(scale, RoundingMode.DOWN);

    if (bd.compareTo(BigDecimal.ZERO) == 0)
      return "0";

    bd = bd.stripTrailingZeros();

    return bd.toPlainString();
  }

  public static String bcsqrt(Env env, Value operand, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal value = toBigDecimal(operand);

    int compareToZero = value.compareTo(ZERO);

    if (compareToZero < 0) {
      env.warning(L.l("square root of negative number"));
      return null;
    }
    else if (compareToZero == 0) {
      return "0";
    }

    int compareToOne = value.compareTo(ONE);

    if (compareToOne == 0)
      return "1";

    // newton's algorithm

    int cscale;

    // initial guess

    BigDecimal initialGuess;

    if (compareToOne < 1) {
      initialGuess = ONE;
      cscale = value.scale();
    }
    else {
      BigInteger integerPart = value.toBigInteger();

      int length = integerPart.toString().length();

      if ((length % 2) == 0)
        length--;

      length /= 2;

      initialGuess = ONE.movePointRight(length);

      cscale = Math.max(scale, value.scale()) + 2;
    }

    // iterate

    BigDecimal guess = initialGuess;

    BigDecimal lastGuess;

    for (int iteration = 0; iteration < SQRT_MAX_ITERATIONS; iteration++) {
      lastGuess = guess;
      guess = value.divide(guess, cscale, RoundingMode.DOWN);
      guess = guess.add(lastGuess);
      guess = guess.divide(TWO, cscale, RoundingMode.DOWN);

      if (lastGuess.equals(guess)) {
          break;
      }
    }

    value = guess;

    value = value.setScale(scale, RoundingMode.DOWN);

    return value.toPlainString();
  }

  public static int bccomp(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    bd1 = bd1.setScale(scale, RoundingMode.DOWN);
    bd2 = bd2.setScale(scale, RoundingMode.DOWN);

    return bd1.compareTo(bd2);
  }

  public static String bcmod(Env env, Value value, Value modulus)
  {
    BigDecimal bd1 = toBigDecimal(value).setScale(0, RoundingMode.DOWN);
    BigDecimal bd2 = toBigDecimal(modulus).setScale(0, RoundingMode.DOWN);

    if (bd2.compareTo(ZERO) == 0) {
      env.warning(L.l("division by zero"));
      return null;
    }

    BigDecimal bd = bd1.remainder(bd2, MathContext.DECIMAL128);

    // scale is always 0 in php
    bd = bd.setScale(0, RoundingMode.DOWN);

    return bd.toPlainString();
  }

  public static String bcpowmod(Env env, Value base, Value exp, Value modulus, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    // XXX: this is inefficient, s/b fast-exponentiation
    String pow = bcpow(env, base, exp, scale);

    if (pow == null)
      return null;

    return bcmod(env, new StringValue(pow), modulus);
  }
}
