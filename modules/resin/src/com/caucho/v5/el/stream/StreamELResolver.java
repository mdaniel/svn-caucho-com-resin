/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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
 * @author Paul Cowan
 */

package com.caucho.v5.el.stream;

import java.beans.FeatureDescriptor;
import java.util.*;

import javax.el.*;

public class StreamELResolver extends ELResolver
{
  @Override
  public Object getValue(ELContext context, Object base, Object property)
  {
    return null;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property,
                       Object value)
  {
  }

  @Override
  public Object invoke(ELContext context,
                       Object base,
                       Object method,
                       Class<?> []paramTypes,
                       Object []params)
  {
    Objects.requireNonNull(context);

    if (params.length != 0 || ! "stream".equals(method)) {
      return null;
    }

    if (base instanceof Collection) {
      context.setPropertyResolved(base, method);

      return new CollectionStream<Object>((Collection) base);
    }
    else if (base instanceof Map) {
      context.setPropertyResolved(base, method);

      return new CollectionStream<Object>(((Map) base).entrySet());
    }
    else if (base.getClass().isArray()) {
      context.setPropertyResolved(base, method);

      return new ArrayStream<Object>(base);
    }
    else {
      return null;
    }
  }

  @Override
  public Class<?> getType(ELContext context, Object base, Object property)
  {
    return null;
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property)
  {
    return true;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
                                                           Object base)
  {
    return null;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    return String.class;
  }
}
