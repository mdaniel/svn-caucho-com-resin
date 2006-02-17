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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.type;

import java.util.HashMap;

import java.sql.SQLException;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.amber.AmberManager;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.Column;

import com.caucho.amber.idgen.AmberTableGenerator;

/**
 * A type which represents a table or a portion.
 */
public class GeneratorTableType extends Type {
  private static final L10N L = new L10N(GeneratorTableType.class);

  private AmberManager _amberManager;

  private Table _table;

  private String _keyColumn = "GEN_KEY";
  private String _valueColumn = "GEN_VALUE";

  private HashMap<String,AmberTableGenerator> _genMap =
    new HashMap<String,AmberTableGenerator>();

  public GeneratorTableType(AmberManager amberManager, String name)
  {
    _amberManager = amberManager;

    _table = amberManager.createTable(name);
  }

  /**
   * Returns the amber manager.
   */
  public AmberManager getAmberManager()
  {
    return _amberManager;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return "Generator";
  }

  /**
   * Returns the table.
   */
  public Table getTable()
  {
    return _table;
  }

  /**
   * Returns the key name.
   */
  public String getKeyColumn()
  {
    return _keyColumn;
  }

  /**
   * Returns the value name.
   */
  public String getValueColumn()
  {
    return _valueColumn;
  }
  
  /**
   * Creates a new generator.
   */
  public AmberTableGenerator createGenerator(String name)
  {
    synchronized (_genMap) {
      AmberTableGenerator gen = _genMap.get(name);

      if (gen == null) {
	gen = new AmberTableGenerator(getAmberManager(), this, name);
	_genMap.put(name, gen);
      }

      return gen;
    }
  }

  /**
   * Initialize the table.
   */
  public void init()
    throws ConfigException
  {
    Column keyColumn = getTable().createColumn(_keyColumn,
					       StringType.create());
    keyColumn.setPrimaryKey(true);
    keyColumn.setLength(254);

    Column valueColumn = getTable().createColumn(_valueColumn,
						 LongType.create());

    if (getAmberManager().getCreateDatabaseTables())
      getTable().createDatabaseTable(getAmberManager());

    for (AmberTableGenerator gen : _genMap.values()) {
      try {
	gen.init(getAmberManager());
      } catch (SQLException e) {
	throw new ConfigException(e);
      }
    }
  }

  /**
   * Printable version of the entity.
   */
  public String toString()
  {
    return "GeneratorTableType[" + getName() + "]";
  }
}
