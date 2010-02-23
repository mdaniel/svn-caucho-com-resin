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

package com.caucho.config.j2ee;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ValueGenerator;
import com.caucho.config.ConfigException;
import com.caucho.naming.*;
import com.caucho.util.L10N;

import javax.persistence.*;
import javax.enterprise.util.AnnotationLiteral;

import java.util.logging.Logger;


public class PersistenceUnitGenerator extends WebBeanGenerator
  implements ObjectProxy {
  private static final Logger log
    = Logger.getLogger(PersistenceUnitGenerator.class.getName());
  private static final L10N L = new L10N(PersistenceUnitGenerator.class);

  private InjectManager _beanManager = InjectManager.create();

  private String _location;
  private String _jndiName;
  private String _unitName;
  private EntityManagerFactory _factory;

  PersistenceUnitGenerator(String location,
                           String jndiName,
                           String unitName)
  {
    _location = location;

    _jndiName = jndiName;
    _unitName = unitName;
  }

  PersistenceUnitGenerator(String location, PersistenceUnit unit)
  {
    _location = location;

    _jndiName = unit.name();
    _unitName = unit.unitName();
  }

  /**
   * Returns the expected type
   */
  @Override
  public Class getType()
  {
    return EntityManagerFactory.class;
  }

  /**
   * Creates the value.
   */
  public Object create()
  {
    try {
      final String name;

      if (_unitName != null && ! "".equals(_unitName))
        name = _unitName;
      else if (_jndiName != null && ! _jndiName.equals(""))
        name = _jndiName;
      else
        name = _unitName;

      return create(EntityManagerFactory.class,
                    new AnnotationLiteral<JpaPersistenceContext>() {
                      public String value() { return name; }
                    });
    } catch (Exception e) {
      throw ConfigException.create(_location, e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jndiName + "," + _unitName + "]";
  }
}
