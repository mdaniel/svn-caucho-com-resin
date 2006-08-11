/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.el;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Resolves properties based on arrays.
 */
abstract public class ArrayELResolver extends ELResolver {
  private final boolean _isReadOnly;
  
  public ArrayELResolver()
  {
    _isReadOnly = false;
  }
  
  public ArrayELResolver(boolean isReadOnly)
  {
    _isReadOnly = true;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    if (base == null)
      return null;
    else if (base.getClass().isArray())
      return Integer.class;
    else
      return null;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
							   Object base)
  {
    return null;
  }

  @Override
  public Class<?> getType(ELContext context,
			  Object base,
			  Object property)
  {
    if (base == null)
      return null;
    else if (! base.getClass().isArray()) {
      context.setPropertyResolved(false);

      return null;
    }
    else {
      context.setPropertyResolved(true);

      return base.getClass().getComponentType();
    }
  }

  @Override
  public Object getValue(ELContext context,
			 Object base,
			 Object property)
  {
    if (base == null)
      return null;
    else if (! base.getClass().isArray()) {
      context.setPropertyResolved(false);

      return null;
    }
    else {
      context.setPropertyResolved(true);

      return Array.get(base, ((Number) property).intValue());
    }
  }

  @Override
  public boolean isReadOnly(ELContext context,
			    Object base,
			    Object property)
  {
    return _isReadOnly;
  }

  @Override
  public void setValue(ELContext context,
		       Object base,
		       Object property,
		       Object value)
  {
    if (base == null) {
    }
    else if (! base.getClass().isArray()) {
      context.setPropertyResolved(false);
    }
    else {
      context.setPropertyResolved(true);

      Array.set(base, ((Number) property).intValue(), property);
    }
  }
}
