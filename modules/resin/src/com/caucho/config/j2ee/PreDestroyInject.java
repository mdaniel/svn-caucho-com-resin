/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import com.caucho.config.BuilderProgram;
import com.caucho.config.LineConfigException;
import com.caucho.config.ConfigException;
import com.caucho.config.NodeBuilder;
import com.caucho.util.*;
import com.caucho.webbeans.context.DependentScope;

import java.lang.reflect.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PreDestroyInject extends Inject
{
  private static final Logger log
    = Logger.getLogger(PreDestroyInject.class.getName());
  private static final L10N L = new L10N(PreDestroyInject.class);

  private Method _destroy;

  public PreDestroyInject(Method destroy)
  {
    _destroy = destroy;
    _destroy.setAccessible(true);
  }

  @Override
  public void inject(Object value, DependentScope scope)
    throws ConfigException
  {
    try {
      _destroy.invoke(value);
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException) e.getCause();
      else if (e.getCause() instanceof LineCompileException)
        throw new LineConfigException(e.getCause().getMessage(), e.getCause());
      else
        throw new ConfigException(e.getCause());
    } catch (IllegalAccessException e) {
      throw new ConfigException(e);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _destroy + "]";
  }
}
