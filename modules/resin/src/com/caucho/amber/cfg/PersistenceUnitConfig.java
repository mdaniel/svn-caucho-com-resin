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
 * @author Scott Ferguson
 */

package com.caucho.amber.cfg;

import java.util.ArrayList;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.config.ConfigException;

/**
 * <persistence-unit> tag in the persistence.xml
 */
public class PersistenceUnitConfig {
  private String _name;

  private ArrayList<String> _classList = new ArrayList<String>();

  /**
   * Returns the unit name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the unit name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Adds a configured class.
   */
  public void addClass(String cl)
  {
    _classList.add(cl);
  }

  public AmberPersistenceUnit init(AmberContainer container)
    throws Exception
  {
    AmberPersistenceUnit unit = new AmberPersistenceUnit(container, _name);

      unit.init();

      for (String cl : _classList) {
	unit.addEntityClass(cl);
      }

      unit.generate();

    return unit;
  }

  public String toString()
  {
    return "PersistenceUnitConfig[" + _name + "]";
  }
}
