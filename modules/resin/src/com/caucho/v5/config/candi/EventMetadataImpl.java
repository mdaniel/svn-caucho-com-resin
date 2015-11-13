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
 * @author Alex Rojkov
 */

package com.caucho.v5.config.candi;

import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public class EventMetadataImpl implements EventMetadata
{
  private final Set<Annotation> _qualifiers;
  private final InjectionPoint _injectionPoint;
  private final Type _type;

  public EventMetadataImpl(Set<Annotation> qualifiers,
                           InjectionPoint injectionPoint,
                           Type type)
  {
    _qualifiers = qualifiers;
    _injectionPoint = injectionPoint;
    _type = type;
  }

  @Override
  public Set<Annotation> getQualifiers()
  {
    return _qualifiers;
  }

  @Override
  public InjectionPoint getInjectionPoint()
  {
    return _injectionPoint;
  }

  @Override
  public Type getType()
  {
    return _type;
  }

  @Override
  public String toString()
  {
    String toString = EventMetadataImpl.class.getSimpleName();
    toString += "[event ";
    if (_qualifiers != null && _qualifiers.size() > 0) {
      toString += _qualifiers;
      toString += ' ';
    }

    toString += ((Class)_type).getName();

    if (_injectionPoint != null) {
      toString += " fired with:" + _injectionPoint;
    }

    toString += ']';

    return toString;
  }
}
