/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package javax.inject;

import java.lang.annotation.*;
import java.lang.reflect.*;

/**
 * Usable API for Annotations types.
 */
public abstract class AnnotationLiteral<T extends Annotation>
  implements Annotation
{
  @SuppressWarnings("unchecked")
  public final Class<T> annotationType()
  {
    Type type = getClass().getGenericSuperclass();

    if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      return (Class) pType.getActualTypeArguments()[0];
    }
    else
      throw new UnsupportedOperationException(type.toString());
  }

  @Override
  public String toString()
  {
    return annotationType().getName() + "[]";
  }
}
