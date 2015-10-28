/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.*;

import com.caucho.v5.el.Expr;

public class TypeConverterImpl extends ELResolver
{
  public TypeConverterImpl()
  {
    
  }
  
  public Object convertToType(ELContext context, 
                              Object obj, 
                              Class<?> targetType)
  {
    return Expr.coerceToType(obj, targetType);
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    return null;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
                                                           Object base)
  {
    return null;
  }

  @Override
  public Class<?> getType(ELContext context, Object base, Object property)
  {
    return null;
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property)
    throws PropertyNotFoundException, ELException
  {
    return null;
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property)
    throws PropertyNotFoundException, ELException
  {
    return false;
  }

  @Override
  public void setValue(ELContext context, 
                       Object base, 
                       Object property,
                       Object value) 
    throws PropertyNotFoundException, PropertyNotWritableException, ELException
  {
  }
  
}
