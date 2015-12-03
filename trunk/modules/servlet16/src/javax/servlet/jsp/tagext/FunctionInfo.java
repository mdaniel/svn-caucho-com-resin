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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.servlet.jsp.tagext;

/**
 * Information about EL functions.
 *
 * @since JSP 2.0
 */
public class FunctionInfo {
  private String _name;
  private String _className;
  private String _signature;

  /**
   * Creates information for a function.
   *
   * @param name name of the function
   * @param className class name for the function
   * @param signature signature of the function
   */
  public FunctionInfo(String name, String className, String signature)
  {
    _name = name;
    _className = className;
    _signature = signature;
  }

  /**
   * Returns the function name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the function's Java class.
   */
  public String getFunctionClass()
  {
    return _className;
  }

  /**
   * Returns the function's signature.
   */
  public String getFunctionSignature()
  {
    return _signature;
  }
}
