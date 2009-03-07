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

import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.scope.ApplicationScope;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.types.*;
import com.caucho.loader.Environment;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.config.*;
import com.caucho.config.bytecode.*;
import com.caucho.config.cfg.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.annotation.*;
import javax.context.ApplicationScoped;
import javax.context.Context;
import javax.context.CreationalContext;
import javax.context.Dependent;
import javax.inject.manager.Bean;

/**
 * Configuration for the xml web bean component.
 */
public class ProxyBean<T> extends AbstractBean<T>
{
  private static final L10N L = new L10N(ProxyBean.class);

  private String _envId;

  public ProxyBean(InjectManager inject,
		   String envId,
		   Class api,
		   Annotation []bindings)
  {
    super(inject);

    _envId = envId;

    setTargetType(api);

    for (Annotation binding : bindings) {
      addBinding(binding);
    }
  }

  public ProxyBean(InjectManager inject,
		   AbstractBean bean)
  {
    super(inject);

    _envId = Environment.getEnvironmentName();

    setTargetType(bean.getTargetType());
    setDeploymentType(bean.getDeploymentType());
    setScopeType(ApplicationScoped.class);

    for (Annotation binding : bean.getBindingArray()) {
      addBinding(binding);
    }
  }

  /**
   * Initialization.
   */
  public void init()
  {
    super.init();
  }

  /**
   * Creates a new instance of the component.
   */
  public T create(CreationalContext<T> context)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public Object createObject(Hashtable env)
  {
    return null;
  }

  public void destroy(T object)
  {
  }
}
