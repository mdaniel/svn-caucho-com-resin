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

import com.caucho.jmx.query.*;
import com.caucho.jmx.query.BooleanValueExp;

/**
 * Represents an expression to extract an attribute value.
 */
public class AttributeValueExp implements ValueExp {
  private String attr;
  private transient MBeanServer _server;
  
  /**
   * Creates a null attribute value expression.
   *
   * @deprecated
   */
  public AttributeValueExp()
  {
  }
  
  /**
   * Creates a names attribute value expression.
   */
  public AttributeValueExp(String attr)
  {
    this.attr = attr;
  }
  
  /**
   * Returns the attribute name.
   */
  protected String getAttributeName()
  {
    return this.attr;
  }
  
  /**
   * Sets the MBean server.
   */
  public void setMBeanServer(MBeanServer server)
  {
    _server = server;
  }

  /**
   * Applies the expression.
   */
  public ValueExp apply(ObjectName name)
    throws BadStringOperationException, BadBinaryOpValueExpException,
    BadAttributeValueExpException, InvalidApplicationException
  {
    MBeanServer server = _server;

    if (server == null)
      server = QueryEval.getMBeanServer();
    
    if (server == null)
      throw new InvalidApplicationException("Missing MBean server in evaluation.");
    if (attr == null)
      throw new InvalidApplicationException("Missing attribute in evaluation.");

    try {
      Object value = server.getAttribute(name, attr);

      if (value == null)
        return new StringValueExp();
      else if (value instanceof String)
        return new StringValueExp((String) value);
      else if (value instanceof Double || value instanceof Float)
        return new DoubleValueExp(((Number) value).doubleValue());
      else if (value instanceof Number)
        return new LongValueExp(((Number) value).longValue());
      else if (value instanceof Boolean)
        return BooleanValueExp.create(((Boolean) value).booleanValue());
      else
        return new StringValueExp(value.toString());
    } catch (Exception e) {
      throw new JMRuntimeException(e);
    }
  }

  /**
   * Returns the attribute value
   */
  protected Object getAttribute(ObjectName name)
  {
    if (_server == null)
      return null;

    try {
      return _server.getAttribute(name, attr);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the string.
   */
  public String toString()
  {
    return "attributeValue(\"" + attr + "\")";
  }
}
