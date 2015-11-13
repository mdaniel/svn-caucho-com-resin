/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.v5.config.type;

import javax.el.ELContext;
import javax.el.ValueExpression;

import com.caucho.v5.config.xml.ContextConfigXml;
import com.caucho.v5.el.ELParser;

/**
 * Represents a ValueExpression type.
 */
public final class ValueExpressionType extends ConfigType
{
  public static final ValueExpressionType TYPE = new ValueExpressionType();
  
  /**
   * The ValueExpressionType is a singleton
   */
  private ValueExpressionType()
  {
  }
  
  /**
   * Returns the Java type.
   */
  public Class getType()
  {
    return ValueExpression.class;
  }
  
  /**
   * Converts the string to a value of the type.
   */
  public Object valueOf(String text)
  {
    ELContext elContext = ContextConfigXml.getCurrent().getELContext();
    
    ELParser parser = new ELParser(elContext, text);
    
    return parser.parse();
  }
}
