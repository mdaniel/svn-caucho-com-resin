/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.config.inject;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;
// import com.caucho.hessian.io.HessianHandle;
import com.caucho.config.cfg.*;

/**
 * Handle for webbeans serialization
 */
public class SingletonHandle implements Serializable //, HessianHandle
{
  private static final L10N L = new L10N(SingletonHandle.class);
  private static final Logger log
    = Logger.getLogger(SingletonHandle.class.getName());
  
  private Class _type;
  private Annotation []_bindings;

  public SingletonHandle()
  {
  }
  
  public SingletonHandle(Type type, Annotation ... bindings)
  {
    if (type instanceof Class)
      _type = (Class) type;

    if (bindings != null && bindings.length > 0)
      _bindings = bindings;
  }
  
  public SingletonHandle(Type type, Set<Annotation> bindingSet)
  {
    if (type instanceof Class)
      _type = (Class) type;

    if (bindingSet.size() > 0) {
      ArrayList bindingList = new ArrayList<Annotation>(bindingSet);

      _bindings = new Annotation[bindingList.size()];
      bindingList.toArray(_bindings);
    }
  }

  /**
   * Deserialization resolution
   */
  public Object readResolve()
  {
    try {
      InjectManager inject = InjectManager.create();

      // return inject.getInstanceByType(_type, _bindings);
      throw new UnsupportedOperationException(getClass().getName());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_type.getName());
    sb.append(", {");

    if (_bindings != null) {
      for (int i = 0; i < _bindings.length; i++) {
	if (i != 0)
	  sb.append(", ");
	sb.append(_bindings[i]);
      }
    }

    sb.append("}]");
    
    return sb.toString();
  }
}
