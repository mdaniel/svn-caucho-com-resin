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

package com.caucho.config.inject;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.reflect.BaseType;
import com.caucho.util.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.*;

import javax.enterprise.inject.spi.Decorator;

/**
 * Represents an introspected Decorator
 */
public class DecoratorEntry<X> {
  private static final Logger log
    = Logger.getLogger(WebComponent.class.getName());
  private static final L10N L = new L10N(WebComponent.class);

  private static final Class []NULL_ARG = new Class[0];

  private Decorator<X> _decorator;

  private ArrayList<QualifierBinding> _bindings
    = new ArrayList<QualifierBinding>();

  private BaseType _delegateType;

  public DecoratorEntry(Decorator<X> decorator,
                        BaseType delegateType)
  {
    _decorator = decorator;
    _delegateType = delegateType;

    for (Annotation ann : decorator.getDelegateQualifiers()) {
      _bindings.add(new QualifierBinding(ann));
    }

    if (_bindings.size() == 0)
      _bindings.add(new QualifierBinding(CurrentLiteral.CURRENT));
  }

  public Decorator<X> getDecorator()
  {
    return _decorator;
  }

  public BaseType getDelegateType()
  {
    return _delegateType;
  }

  public boolean isMatch(Annotation []bindingAnn)
  {
    for (QualifierBinding binding : _bindings) {
      if (! isMatch(binding, bindingAnn)) {
        return false;
      }
    }

    return true;
  }

  public boolean isMatch(QualifierBinding binding, Annotation []bindingAnn)
  {
    for (Annotation ann : bindingAnn) {
      if (binding.isMatch(ann))
        return true;
    }

    return false;
  }
}
