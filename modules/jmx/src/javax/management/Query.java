/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.management;

import com.caucho.jmx.AbstractMBeanServerFactory;

import com.caucho.jmx.query.BooleanValueExp;
import com.caucho.jmx.query.*;

/**
 * Factory for creating and finding MBeanServers.
 *
 * <p>The MBeanServer is the main root of JMX.
 */
public class Query {
  /**
   * Returns an AND expression.
   */
  public static QueryExp and(QueryExp q1, QueryExp q2)
  {
    return new AndExp(q1, q2);
  }
  
  /**
   * Returns an OR expression.
   */
  public static QueryExp or(QueryExp q1, QueryExp q2)
  {
    return new OrExp(q1, q2);
  }
  
  /**
   * Returns a boolean NOT expression.
   */
  public static QueryExp not(QueryExp q1)
  {
    return new NotExp(q1);
  }
  
  /**
   * Returns an EQ expression.
   */
  public static QueryExp eq(ValueExp v1, ValueExp v2)
  {
    return new EqExp(v1, v2);
  }
  
  /**
   * Returns a GT expression.
   */
  public static QueryExp gt(ValueExp v1, ValueExp v2)
  {
    return new LtExp(v2, v1);
  }
  
  /**
   * Returns a GEQ expression.
   */
  public static QueryExp geq(ValueExp v1, ValueExp v2)
  {
    return new LeqExp(v2, v1);
  }
  
  /**
   * Returns a LEQ expression.
   */
  public static QueryExp leq(ValueExp v1, ValueExp v2)
  {
    return new LeqExp(v1, v2);
  }
  
  /**
   * Returns a LT expression.
   */
  public static QueryExp lt(ValueExp v1, ValueExp v2)
  {
    return new LtExp(v1, v2);
  }
  
  /**
   * Returns a between expression.
   */
  public static QueryExp between(ValueExp v1, ValueExp v2, ValueExp v3)
  {
    return new BetweenExp(v1, v2, v3);
  }
  
  /**
   * Returns a set membership expression.
   */
  public static QueryExp in(ValueExp val, ValueExp []valueList)
  {
    return new InExp(val, valueList);
  }
  
  /**
   * Returns a between expression.
   */
  public static QueryExp match(AttributeValueExp a, StringValueExp s)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns an expression for an initial substring.
   */
  public static QueryExp initialSubString(AttributeValueExp a,
                                          StringValueExp s)
  {
    return new InitialSubStringExp(a, s);
  }
  
  /**
   * Returns an expression for any substring.
   */
  public static QueryExp anySubString(AttributeValueExp a, StringValueExp s)
  {
    return new AnySubStringExp(a, s);
  }
  
  /**
   * Returns an expression for a final substring.
   */
  public static QueryExp finalSubString(AttributeValueExp a, StringValueExp s)
  {
    return new FinalSubStringExp(a, s);
  }
  
  /**
   * Returns an attribute expression.
   */
  public static AttributeValueExp attr(String name)
  {
    return new AttributeValueExp(name);
  }
  
  /**
   * Returns an attribute expression.
   */
  public static AttributeValueExp attr(String className, String name)
  {
    return new AttributeValueExp(name);
  }

  /**
   * Returns an attribute expresson.
   */
  public static AttributeValueExp classattr()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a binary addition numeric expresson.
   */
  public static ValueExp plus(ValueExp value1, ValueExp value2)
  {
    return new PlusExp(value1, value2);
  }

  /**
   * Returns a binary subtraction numeric expresson.
   */
  public static ValueExp minus(ValueExp value1, ValueExp value2)
  {
    return new MinusExp(value1, value2);
  }

  /**
   * Returns a binary multiplication numeric expresson.
   */
  public static ValueExp times(ValueExp value1, ValueExp value2)
  {
    return new TimesExp(value1, value2);
  }

  /**
   * Returns a binary division numeric expresson.
   */
  public static ValueExp div(ValueExp value1, ValueExp value2)
  {
    return new DivExp(value1, value2);
  }

  /**
   * Returns a string expresson.
   */
  public static StringValueExp value(String val)
  {
    return new StringValueExp(val);
  }

  /**
   * Returns a numeric expresson.
   */
  public static ValueExp value(Number val)
  {
    if (val instanceof Double || val instanceof Float)
      return new DoubleValueExp(val.doubleValue());
    else
      return new LongValueExp(val.longValue());
  }

  /**
   * Returns a numeric expresson.
   */
  public static ValueExp value(int val)
  {
    return new LongValueExp(val);
  }

  /**
   * Returns a numeric expresson.
   */
  public static ValueExp value(long val)
  {
    return new LongValueExp(val);
  }

  /**
   * Returns a numeric expresson.
   */
  public static ValueExp value(float val)
  {
    return new DoubleValueExp(val);
  }

  /**
   * Returns a numeric expresson.
   */
  public static ValueExp value(double val)
  {
    return new DoubleValueExp(val);
  }

  /**
   * Returns a boolean expresson.
   */
  public static ValueExp value(boolean val)
  {
    return BooleanValueExp.create(val);
  }
}
