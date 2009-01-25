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
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import com.caucho.config.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.scope.DependentScope;
import com.caucho.util.*;

import java.util.logging.*;
import java.lang.reflect.*;
import javax.inject.manager.Bean;

public class FieldComponentProgram extends ConfigProgram
{
  private static final L10N L = new L10N(FieldComponentProgram.class);
  private static final Logger log
    = Logger.getLogger(FieldComponentProgram.class.getName());

  private InjectManager _manager;
  private Bean _bean;
  private Field _field;

  public FieldComponentProgram(InjectManager manager,
			       Bean bean,
			       Field field)
  {
    _manager = manager;
    _bean = bean;
    _field = field;

    field.setAccessible(true);
  }

  public void inject(Object bean, ConfigContext env)
  {
    Object value = null;
    try {
      value = _manager.getInstance(_bean, env);

      _field.set(bean, value);
    } catch (IllegalArgumentException e) {
      throw new ConfigException(ConfigException.loc(_field) + L.l("Can't set field value '{0}'", value), e);
    } catch (Exception e) {
      throw new ConfigException(ConfigException.loc(_field) + e.toString(), e);
    }
  }
}
