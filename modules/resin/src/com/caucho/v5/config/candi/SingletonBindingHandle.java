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

package com.caucho.v5.config.candi;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import javax.enterprise.inject.spi.Bean;

/**
 * Handle for webbeans serialization
 */
@SuppressWarnings("serial")
public class SingletonBindingHandle implements Serializable
{
  private final Class<?> _type;
  private final Annotation []_binding;

  @SuppressWarnings("unused")
  private SingletonBindingHandle()
  {
    _type = null;
    _binding = null;
  }

  public SingletonBindingHandle(Class<?> type, Annotation ...binding)
  {
    _type = type;

    if (binding != null && binding.length == 0)
      binding = null;

    _binding = binding;
  }

  /**
   * Deserialization resolution
   */
  public Object readResolve()
  {
    try {
      CandiManager inject = CandiManager.create();

      Bean<?> bean
        = inject.resolve(inject.getBeans(_type, _binding));

      return inject.getReference(bean);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]";
  }
}
