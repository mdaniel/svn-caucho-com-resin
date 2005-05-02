/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import javax.naming.InitialContext;

import com.caucho.util.L10N;

import com.caucho.config.BuilderProgram;
import com.caucho.config.NodeBuilder;

public class JndiFieldInjectProgram extends BuilderProgram {
  private static final L10N L = new L10N(JndiFieldInjectProgram.class);

  private String _jndiName;
  private Field _field;

  JndiFieldInjectProgram(String jndiName, Field field)
  {
    _jndiName = jndiName;
    _field = field;
  }

  public void configureImpl(NodeBuilder builder, Object bean)
    throws Throwable
  {
    Object value = new InitialContext().lookup(_jndiName);

    _field.setAccessible(true);
    _field.set(bean, value);
  }

  public Object configure(NodeBuilder builder, Class type)
    throws Exception
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
