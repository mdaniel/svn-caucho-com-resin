/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ValueGenerator;
import com.caucho.util.L10N;

public class JndiValueGenerator extends ValueGenerator {
  private static final Logger log = Logger.getLogger(JndiValueGenerator.class.getName());
  private static final L10N L = new L10N(JndiValueGenerator.class);
  
  private final String _location;
  private final String _jndiName;
  private final Class<?> _type;

  public JndiValueGenerator(String location, Class<?> type, String jndiName)
  {
    _location = location;
    _type = type;
    _jndiName = jndiName;
    
    if (_jndiName == null || "".equals(_jndiName))
      throw new IllegalArgumentException(L.l("JNDI name cannot be empty"));
  }

  @Override
  public Object create()
    throws ConfigException
  {
    try {
      return new InitialContext().lookup(_jndiName);
    } catch (NamingException e) {
      throw ConfigException.create(_location, e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jndiName + "," + _type + "]";
  }
}