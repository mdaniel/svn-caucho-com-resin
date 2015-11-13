/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Paul Cowan
 */

package com.caucho.v5.el.stream;

import static com.caucho.v5.el.Expr.*;

import java.math.*;

import javax.el.*;

import com.caucho.v5.el.Expr;
import com.caucho.v5.util.L10N;

public class StreamUtil
{
  protected static final L10N L = new L10N(Stream.class);

  public static int compare(Object aObj, Object bObj)
  {
    if (aObj == bObj || aObj.equals(bObj))
      return 0;
    
    try {
      // do we need to deal with null?
      
      if (aObj instanceof Double || aObj instanceof Float ||
          bObj instanceof Double || bObj instanceof Float ||
          aObj instanceof Number || bObj instanceof Number) {
        Double a = toDouble(aObj);
        Double b = toDouble(bObj);
        return a.compareTo(b);
      }
      
      if (aObj instanceof String || bObj instanceof String) {
        String a = Expr.toString(aObj);
        String b = Expr.toString(bObj);
        return a.compareTo(b);
      }
  
      if (aObj instanceof Comparable) {
        return ((Comparable) aObj).compareTo(bObj);
      }
  
      if (bObj instanceof Comparable) {
        return ((Comparable) bObj).compareTo(aObj);
      }
    } catch (ELException e) {
      throw new ELException(L.l("can't compare {0} and {1}.", aObj, bObj), e);
    }

    throw new ELException(L.l("can't compare {0} and {1}.", aObj, bObj));
  }
  
  public static Number add(Object aObj, Object bObj)
  {
    if (aObj == null && bObj == null) {
      return new Long(0);
    } 
    else if (aObj instanceof BigDecimal || bObj instanceof BigDecimal) {
      BigDecimal a = toBigDecimal(aObj);
      BigDecimal b = toBigDecimal(bObj);
      return a.add(b);
    } 
    else if (Expr.isDouble(aObj)) {
      if (bObj instanceof BigInteger) {
        BigDecimal a = toBigDecimal(aObj);
        BigDecimal b = toBigDecimal(bObj);
        return a.add(b);
      } 
      else {
        double a = toDouble(aObj);
        double b = toDouble(bObj);
        double dValue = a + b;
        return Double.isNaN(dValue) ? new Double(0) : new Double(dValue);
      }
    } 
    else if (Expr.isDouble(bObj)) {
      if (aObj instanceof BigInteger) {
        BigDecimal a = toBigDecimal(aObj);
        BigDecimal b = toBigDecimal(bObj);
        return a.add(b);
      } 
      else {
        double a = toDouble(aObj);
        double b = toDouble(bObj);
        double dValue = a + b;
        return Double.isNaN(dValue) ? new Double(0) : new Double(dValue);
      }
    } 
    else if (aObj instanceof BigInteger || bObj instanceof BigInteger) {
      BigInteger a = (BigInteger) Expr.coerceToType(aObj, BigInteger.class);
      BigInteger b = (BigInteger) Expr.coerceToType(bObj, BigInteger.class);
      return a.add(b);
    }

    if (bObj instanceof Double || bObj instanceof Float) {
      double a = toDouble(aObj);
      double b = ((Number) bObj).doubleValue();
      double dValue = a + b;
      return Double.isNaN(dValue) ? new Double(0) : new Double(dValue);
    } 
    else if (aObj instanceof Number) {
      long a = ((Number) aObj).longValue();
      long b = toLong(bObj);
      return new Long(a + b);
    } 
    else if (bObj instanceof Number) {
      long a = toLong(aObj);
      long b = ((Number) bObj).longValue();
      return new Long(a + b);
    }

    if (isDoubleString(aObj) || isDoubleString(bObj)) {
      double a = toDouble(aObj);
      double b = toDouble(bObj);
      return new Double(a + b);
    } 
    else {
      long a = toLong(aObj);
      long b = toLong(bObj);
      return new Long(a + b);
    }
  }
  
}
