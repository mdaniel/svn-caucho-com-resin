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

package com.caucho.v5.el;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Abstract variable resolver.  Supports chaining and the "Var"
 * special variable.
 */
public class VariableResolverBase extends ELResolver
{
  private static final Logger log
    = Logger.getLogger(VariableResolverBase.class.getName());
  
  private ELResolver _next;
  
  /**
   * Creates the resolver
   */
  public VariableResolverBase()
  {
  }
  
  /**
   * Creates the resolver
   */
  public VariableResolverBase(ELResolver next)
  {
    _next = next;
  }

  /**
   * Returns the next resolver.
   */
  public ELResolver getNext()
  {
    return _next;
  }
  
  /**
   * Returns the named variable value.
   */
  @Override
  public Object getValue(ELContext context,
                         Object base,
                         Object property)
  {
    return null;
  }

  //
  // ELResolver stubs
  //

  @Override
  public Class<?> getCommonPropertyType(ELContext context,
                                        Object base)
  {
    return null;
  }

  public Iterator<FeatureDescriptor>
    getFeatureDescriptors(ELContext context, Object base)
  {
    return null;
  }

  public Class<?> getType(ELContext context,
                          Object base,
                          Object property)
  {
    Object value = getValue(context, base, property);

    if (value == null)
      return null;
    else
      return value.getClass();
  }

  public boolean isReadOnly(ELContext context,
                            Object base,
                            Object property)
    throws PropertyNotFoundException,
           ELException
  {
    return true;
  }

  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
    throws PropertyNotFoundException,
           PropertyNotWritableException,
           ELException
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
