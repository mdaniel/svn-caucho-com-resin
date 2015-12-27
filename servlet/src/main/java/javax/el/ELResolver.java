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

package javax.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Resolves EL variables
 */
public abstract class ELResolver 
{
  private static final Logger log = Logger.getLogger(ELResolver.class.getName());

  public static final String RESOLVABLE_AT_DESIGN_TIME 
    = "resolvableAtDesignTime";
  
  public static final String TYPE = "type";

  public ELResolver()
  {
  }

  public abstract Class<?> getCommonPropertyType(ELContext context,
                                                 Object base);

  public abstract Iterator<FeatureDescriptor>
    getFeatureDescriptors(ELContext context, Object base);

  public abstract Class<?> getType(ELContext context,
                                   Object base,
                                   Object property);

  public abstract Object getValue(ELContext context,
                                  Object base,
                                  Object property)
    throws PropertyNotFoundException,
           ELException;

  public java.lang.Object invoke(ELContext context,
                                 Object base,
                                 Object method,
                                 Class<?> []paramTypes,
                                 Object []params)
  {
    log.fine(String.format("Call to unimplemented method '%s.%s'",
                           this.getClass().getName(),
                           "invoke"));
    return null;
  }

  public abstract boolean isReadOnly(ELContext context,
                                     Object base,
                                     Object property)
    throws PropertyNotFoundException,
           ELException;

  public abstract void setValue(ELContext context,
                                Object base,
                                Object property,
                                Object value)
    throws PropertyNotFoundException,
           PropertyNotWritableException,
           ELException;
  
  public Object convertToType(ELContext context,
                              Object obj,
                              Class<?> targetType)
  {
    //return context.convertToType(obj, targetType);
    return null;
  }
  
  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
}
