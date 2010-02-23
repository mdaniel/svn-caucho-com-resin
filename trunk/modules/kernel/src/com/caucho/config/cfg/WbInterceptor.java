/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.cfg;

import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import javax.interceptor.*;

import com.caucho.config.*;
import com.caucho.util.*;

/**
 * Configuration for the xml interceptor.
 */
public class WbInterceptor {
  private static final L10N L = new L10N(WbInterceptor.class);

  private Class _cl;

  private ArrayList<Annotation> _bindingList
    = new ArrayList<Annotation>();

  private Method _invokeMethod;

  WbInterceptor(Class cl)
  {
    if (! cl.isAnnotationPresent(Interceptor.class))
      throw new ConfigException(L.l("'{0}' must have an @Interceptor annotation to be declared as an interceptor.",
                                    cl.getName()));

    _cl = cl;

    for (Annotation ann : cl.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(InterceptorBinding.class)) {
        _bindingList.add(ann);
      }
    }

    if (_bindingList.size() == 0) {
      throw new ConfigException(L.l("'{0}' must have at least one @InterceptorBinding annotation to be declared as an interceptor.",
                                    cl.getName()));
    }

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class)) {
        if (_invokeMethod != null) {
          throw new ConfigException(L.l("'{0}' has two @AroundInvoke methods: '{1}' and '{2}'.",
                                        cl.getName(), _invokeMethod, method));
        }

        _invokeMethod = method;
      }
    }

    if (_invokeMethod == null) {
      throw new ConfigException(L.l("'{0}' must have at least one @AroundInvoke method",
                                    cl.getName()));
    }
  }

  public Class getInterceptorClass()
  {
    return _cl;
  }

  public Method getMethod()
  {
    return _invokeMethod;
  }

  public Object getObject()
  {
    try {
      return _cl.newInstance();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public boolean isMatch(ArrayList<Annotation> bindList)
  {
    for (int i = 0; i < _bindingList.size(); i++) {
      if (! isMatch(_bindingList.get(i), bindList))
        return false;
    }

    return true;
  }

  /**
   * Returns true if at least one of this component's bindings match
   * the injection binding.
   */
  public boolean isMatch(Annotation binding, ArrayList<Annotation> bindList)
  {
    return false;
    /*
    for (int i = 0; i < bindList.size(); i++) {
      if (binding.isMatch(bindList.get(i)))
        return true;
    }

    return false;
    */
  }
}
