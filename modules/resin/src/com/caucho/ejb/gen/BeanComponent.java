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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import java.io.IOException;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

/**
 * Generates the skeleton for a bean component.
 */
public class BeanComponent {
  private static L10N L = new L10N(BeanComponent.class);

  public void generate(JavaWriter out)
    throws IOException
  {
  }
  
  /**
   * Converts a java primitive type to a Java object.
   *
   * @param value the java expression to be converted
   * @param javaType the type of the converted expression.
   */
  protected void printJavaTypeToObject(JavaWriter out,
				       String value, Class javaType)
    throws IOException
  {
    if (Object.class.isAssignableFrom(javaType))
      out.print(value);
    else if (javaType.equals(boolean.class))
      out.print("new Boolean(" + value + ")");
    else if (javaType.equals(byte.class))
      out.print("new Byte(" + value + ")");
    else if (javaType.equals(short.class))
      out.print("new Short(" + value + ")");
    else if (javaType.equals(int.class))
      out.print("new Integer(" + value + ")");
    else if (javaType.equals(long.class))
      out.print("new Long(" + value + ")");
    else if (javaType.equals(char.class))
      out.print("String.valueOf(" + value + ")");
    else if (javaType.equals(float.class))
      out.print("new Float(" + value + ")");
    else if (javaType.equals(double.class))
      out.print("new Double(" + value + ")");
    else
      out.print(value);
  }
}
