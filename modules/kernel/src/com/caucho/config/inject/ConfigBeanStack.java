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
 */

package com.caucho.config.inject;

import com.caucho.config.Config;
import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.program.NodeBuilderChildProgram;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.scope.DependentScope;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.types.Validator;
import com.caucho.config.type.*;
import com.caucho.config.types.*;
import com.caucho.config.attribute.*;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;

import org.w3c.dom.*;

import javax.el.*;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Stack of partially constructed beans.
 */
public class ConfigBeanStack {
  private final ConfigBeanStack _next;
  private final Contextual<?> _bean;
  private final Object _value;
  
  ConfigBeanStack(ConfigBeanStack next,
                  Contextual<?> bean,
                  Object value)
  {
    _next = next;
    _bean = bean;
    _value = value;
  }
  
  static Object find(ConfigBeanStack ptr, Contextual<?> bean)
  {
    for (; ptr != null; ptr = ptr._next){
      if (ptr._bean == bean)
        return ptr._value;
    }
    
    return null;
  }
}
