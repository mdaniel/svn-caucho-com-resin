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
import com.caucho.config.ConfigException;
import com.caucho.config.NodeBuilder;
import com.caucho.util.L10N;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JndiFieldInjectProgram extends BuilderProgram {
  private static final Logger log
    = Logger.getLogger(JndiFieldInjectProgram.class.getName());
  private static final L10N L = new L10N(JndiFieldInjectProgram.class);

  private String _jndiName;
  private Field _field;

  JndiFieldInjectProgram(String jndiName, Field field)
  {
    _jndiName = jndiName;
    _field = field;
  }

  @Override
  public void configureImpl(NodeBuilder builder, Object bean)
    throws ConfigException
  {
    try {
      Object value = new InitialContext().lookup(_jndiName);

      if (value == null)
	return;

      if (! _field.getType().isAssignableFrom(value.getClass())) {
	throw new ConfigException(L.l("Resource at '{0}' of type {1} is not assignable to field '{2}' of type {3}.",
				      _jndiName,
				      value.getClass().getName(),
				      _field.getName(),
				      _field.getType().getName()));
      }

      _field.setAccessible(true);
      _field.set(bean, value);
    } catch (RuntimeException e) {
      throw e;
    } catch (NamingException e) {
      log.finer(String.valueOf(e));
      log.log(Level.FINEST, e.toString(), e);
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  @Override
  public Object configureImpl(NodeBuilder builder, Class type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jndiName + "," + _field + "]";
  }
}
