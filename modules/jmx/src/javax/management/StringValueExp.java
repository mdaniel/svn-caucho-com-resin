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

/**
 * Represents a value expression
 */
public class StringValueExp implements ValueExp {
  private String val;
  private transient MBeanServer _server;
  
  /**
   * Creates a null string value expression.
   */
  public StringValueExp()
  {
  }
  
  /**
   * Creates a null string value expression with a value.
   */
  public StringValueExp(String value)
  {
    this.val = value;
  }

  /**
   * Returns the underlying string.
   */
  public String getValue()
  {
    return val;
  }
  
  /**
   * Returns a value expression the value expression matches the name.
   */
  public ValueExp apply(ObjectName name)
    throws BadStringOperationException,
    BadBinaryOpValueExpException,
    BadAttributeValueExpException,
    InvalidApplicationException
  {
    return this;
  }
  
  /**
   * Sets the MBean server for the query.
   */
  public void setMBeanServer(MBeanServer s)
  {
    _server = s;
  }

  /**
   * Returns the underlying string.
   */
  public String toString()
  {
    return "\"" + val + "\"";
  }
}
