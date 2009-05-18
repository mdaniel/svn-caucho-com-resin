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

package com.caucho.config.types;

import com.caucho.config.ConfigContext;
import com.caucho.config.inject.ManagedBeanWrapper;
import com.caucho.config.program.Arg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.util.L10N;
import javax.inject.manager.*;
import javax.inject.manager.InjectionTarget;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Set;

import javax.context.Contextual;
import javax.context.CreationalContext;
/**
 * Internal implementation for a Bean
 */
public class XmlBean<X> extends ManagedBeanWrapper<X>
  implements InjectionTarget<X>
{
  private static final L10N L = new L10N(XmlBean.class);
  
  private Constructor _ctor;
  private Arg []_newProgram;
  private ConfigProgram []_injectProgram;
  
  public XmlBean(ManagedBean<X> bean,
		 Constructor ctor,
		 Arg []newProgram,
		 ConfigProgram []injectProgram)
  {
    super(bean);

    _ctor = ctor;
    _newProgram = newProgram;

    _injectProgram = injectProgram;
  }

  public X produce(CreationalContext context)
  {
    ConfigContext env = (ConfigContext) context;
    
    if (_ctor == null)
      return getBean().getInjectionTarget().produce(env);
    else {
      Object []args = new Object[_newProgram.length];

      for (int i = 0; i < args.length; i++) {
	args[i] = _newProgram[i].eval(env);
      }

      try {
	return (X) _ctor.newInstance(args);
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	// XXX: clean up exception type
	throw new RuntimeException(e);
      }
    }
  }

  public void inject(X instance, CreationalContext<X> env)
  {
    getBean().getInjectionTarget().inject(instance, env);

    if (_injectProgram.length > 0) {
      for (ConfigProgram program : _injectProgram) {
	program.inject(instance, (ConfigContext) env);
      }
    }
  }
  
  /**
   * Call pre-destroy
   */
  public void dispose(X instance)
  {
  }
  
  /**
   * Call destroy
   */
  public void destroy(X instance)
  {
  }

  /**
   * Returns the injection points.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return getBean().getInjectionPoints();
  }
}
