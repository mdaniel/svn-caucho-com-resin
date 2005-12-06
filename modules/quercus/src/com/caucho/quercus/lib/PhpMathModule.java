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

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import com.caucho.util.L10N;

import com.caucho.quercus.module.PhpModule;
import com.caucho.quercus.module.AbstractPhpModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.VarMap;
import com.caucho.quercus.env.ChainedMap;

import com.caucho.util.RandomUtil;

import com.caucho.vfs.WriteStream;

/**
 * PHP math routines.
 */
public class PhpMathModule extends AbstractPhpModule {
  private static final L10N L = new L10N(PhpMathModule.class);

  public static final DoubleValue PI = new DoubleValue(Math.PI);
  public static final DoubleValue E = new DoubleValue(Math.E);

  public static final long RAND_MAX = 1L << 32;

  private static final HashMap<String,Value> _constMap =
          new HashMap<String,Value>();

  static {
    _constMap.put("M_PI", PI);
    _constMap.put("M_E", E);
    
    _constMap.put("M_LOG2E", new DoubleValue(log2(Math.E)));
    _constMap.put("M_LOG10E", new DoubleValue(Math.log10(Math.E)));
    _constMap.put("M_LN2", new DoubleValue(Math.log(2)));
    _constMap.put("M_LN10", new DoubleValue(Math.log(10)));
    _constMap.put("M_PI_2", new DoubleValue(Math.PI / 2));
    _constMap.put("M_PI_4", new DoubleValue(Math.PI / 4));
    _constMap.put("M_1_PI", new DoubleValue(1 / Math.PI));
    _constMap.put("M_2_PI", new DoubleValue(2 / Math.PI));
    _constMap.put("M_SQRTPI", new DoubleValue(Math.sqrt(Math.PI)));
    _constMap.put("M_2_SQRTPI", new DoubleValue(2 / Math.sqrt(Math.PI)));
    _constMap.put("M_SQRT2", new DoubleValue(Math.sqrt(2)));
    _constMap.put("M_SQRT3", new DoubleValue(Math.sqrt(3)));
    _constMap.put("M_SQRT1_2", new DoubleValue(1 / Math.sqrt(2)));
    _constMap.put("M_LNPI", new DoubleValue(Math.log(Math.PI)));
    _constMap.put("M_EULER", new DoubleValue(0.57721566490153286061));
  }

  private static double log2(double v)
  {
    return Math.log(v) / Math.log(2);
  }

  /**
   * Adds the constant to the PHP engine's constant map.
   *
   * @return the new constant chain
   */
  public Map<String,Value> getConstMap()
  {
    return _constMap;
  }

  public static Value abs(Env env, Value value)
  {
    if (value instanceof DoubleValue)
      return new DoubleValue(Math.abs(value.toDouble()));
    else
      return new LongValue(Math.abs(value.toLong()));
  }

  public static double acos(double value)
  {
    return Math.acos(value);
  }

  public static Value acosh(Env env, Value value)
  {
    throw new UnsupportedOperationException();
  }

  public static Value asin(Value value)
  {
    return new DoubleValue(Math.asin(value.toDouble()));
  }

  public static Value asinh(Value value)
  {
    throw new UnsupportedOperationException();
  }

  public static double atan2(double yV, double xV)
  {
    return Math.atan2(yV, xV);
  }

  public static double atan(double value)
  {
    return Math.atan(value);
  }

  public static Value atanh(Value value)
  {
    throw new UnsupportedOperationException();
  }

  public static double ceil(double value)
  {
    return Math.ceil(value);
  }

  public static double cos(double value)
  {
    return Math.cos(value);
  }

  public static double cosh(double value)
  {
    return Math.cosh(value);
  }

  public static String dechex(long value)
  {
    StringBuilder sb = new StringBuilder();

    while (value != 0) {
      int d = (int) (value & 0xf);
      value = value / 16;

      if (d < 10)
	sb.append((char) (d + '0'));
      else
	sb.append((char) (d + 'a' - 10));
    }

    StringBuilder sb2 = new StringBuilder();
    for (int i = sb.length() - 1; i >= 0; i--)
      sb2.append(sb.charAt(i));

    return sb2.toString();
  }

  public static Value deg2rad(Value value)
  {
    return new DoubleValue(value.toDouble() * Math.PI / 180);
  }

  public static Value exp(Value value)
  {
    return new DoubleValue(Math.exp(value.toDouble()));
  }

  public static Value expm1(Value value)
  {
    return new DoubleValue(Math.expm1(value.toDouble()));
  }

  public static Value floor(Value value)
  {
    return new DoubleValue(Math.floor(value.toDouble()));
  }

  public static double fmod(double xV, double yV)
  {
    return Math.IEEEremainder(xV, yV);
  }

  public static Value hexdec(String s)
  {
    long v = 0;
    int len = s.length();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if ('0' <= ch && ch <= '9')
	v = 16 * v + ch - '0';
      else if ('a' <= ch && ch <= 'f')
	v = 16 * v + ch - 'a' + 10;
      else if ('A' <= ch && ch <= 'F')
	v = 16 * v + ch - 'A' + 10;
    }
    
    return new LongValue(v);
  }

  public static double hypot(double a, double b)
  {
    return Math.hypot(a, b);
  }

  public static Value is_finite(Value value)
  {
    if (value instanceof LongValue)
      return BooleanValue.TRUE;
    else if (value instanceof DoubleValue) {
      double v = value.toDouble();
      
      return Double.isInfinite(v) ? BooleanValue.FALSE : BooleanValue.TRUE;
    }
    else
      return BooleanValue.FALSE;
  }

  public static Value is_infinite(Value value)
  {
    if (value instanceof LongValue)
      return BooleanValue.FALSE;
    else if (value instanceof DoubleValue) {
      double v = value.toDouble();
      
      return Double.isInfinite(v) ? BooleanValue.TRUE : BooleanValue.FALSE;
    }
    else
      return BooleanValue.FALSE;
  }

  public static Value is_nan(Value value)
  {
    if (value instanceof LongValue)
      return BooleanValue.FALSE;
    else if (value instanceof DoubleValue) {
      double v = value.toDouble();
      
      return Double.isNaN(v) ? BooleanValue.TRUE : BooleanValue.FALSE;
    }
    else
      return BooleanValue.FALSE;
  }

  public static double log(double value)
  {
    return Math.log(value);
  }

  public static double log10(double value)
  {
    return Math.log10(value);
  }

  public static double log1p(double value)
  {
    return Math.log1p(value);
  }

  public static Value getrandmax()
  {
    return mt_getrandmax();
  }

  public static Value max(Value []args)
  {
    if (args.length == 1 && args[0] instanceof ArrayValue) {
      Value array = args[0];
      Value max = NullValue.NULL;
      double maxValue = Double.MIN_VALUE;

      for (Value key : array.getIndices()) {
	Value value = array.get(key);
	double dValue = value.toDouble();
	
	if (maxValue < dValue) {
	  maxValue = dValue;
	  max = value;
	}
      }

      return max;
    }
    else {
      double maxValue = Double.MIN_VALUE;
      Value max = NullValue.NULL;

      for (int i = 0; i < args.length; i++) {
	double value = args[i].toDouble();

	if (maxValue < value) {
	  maxValue = value;
	  max = args[i];
	}
      }
      
      return max;
    }
  }

  public static Value min(Value []args)
  {
    if (args.length == 1 && args[0] instanceof ArrayValue) {
      Value array = args[0];
      Value min = NullValue.NULL;
      double minValue = Double.MAX_VALUE;

      for (Value key : array.getIndices()) {
	Value value = array.get(key);
	double dValue = value.toDouble();
	
	if (dValue < minValue) {
	  minValue = dValue;
	  min = value;
	}
      }

      return min;
    }
    else {
      double minValue = Double.MAX_VALUE;
      Value min = NullValue.NULL;

      for (int i = 0; i < args.length; i++) {
	double value = args[i].toDouble();

	if (value < minValue) {
	  minValue = value;
	  min = args[i];
	}
      }
      
      return min;
    }
  }

  public static Value mt_getrandmax()
  {
    return new LongValue(RAND_MAX);
  }

  public static long mt_rand(@Optional("0") long min,
			     @Optional("RAND_MAX") long max)
  {
    long range = max - min + 1;

    if (range <= 0)
      return min;

    long value = RandomUtil.getRandomLong();
    if (value < 0)
      value = - value;

    return min + value % range;
  }

  public static Value mt_srand(@Optional long seed)
  {
    return NullValue.NULL;
  }

  public static Value pi()
  {
    return PI;
  }

  public static Value pow(Value base, Value exp)
  {
    return new DoubleValue(Math.pow(base.toDouble(), exp.toDouble()));
  }

  public static Value rad2deg(Value value)
  {
    return new DoubleValue(180 * value.toDouble() / Math.PI);
  }

  public static Value round(Value value)
  {
    return new DoubleValue(Math.round(value.toDouble()));
  }

  public static Value sin(Value value)
  {
    return new DoubleValue(Math.sin(value.toDouble()));
  }

  public static Value sinh(Value value)
  {
    return new DoubleValue(Math.sinh(value.toDouble()));
  }

  public static Value sqrt(Value value)
  {
    return new DoubleValue(Math.sqrt(value.toDouble()));
  }

  public static Value srand(@Optional long seed)
  {
    return NullValue.NULL;
  }

  public static Value tan(Value value)
  {
    return new DoubleValue(Math.tan(value.toDouble()));
  }

  public static double tanh(double value)
  {
    return Math.tanh(value);
  }
}
