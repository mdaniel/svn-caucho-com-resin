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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.persistence.*;

import com.caucho.amber.AmberTableCache;

import com.caucho.amber.field.*;

import com.caucho.amber.idgen.IdGenerator;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.Table;

import com.caucho.amber.type.*;

import com.caucho.bytecode.*;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.ejb.EjbServerManager;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

/**
 * Configuration for a mapped superclass type
 */
public class MappedSuperIntrospector extends BaseConfigIntrospector {
  private static final L10N L = new L10N(MappedSuperIntrospector.class);
  private static final Logger log
    = Logger.getLogger(MappedSuperIntrospector.class.getName());

  // HashMap<String, MappedSuperType> _mappedSuperMap
  // = new HashMap<String, MappedSuperType>();

  /**
   * Creates the introspector.
   */
  public MappedSuperIntrospector(AmberPersistenceUnit persistenceUnit)
  {
    super(persistenceUnit);
  }

  /**
   * Returns true for mapped superclass type.
   */
  public boolean isMappedSuper(JClass type)
  {
    getInternalMappedSuperclassConfig(type);
    JAnnotation mappedSuperAnn = _annotationCfg.getAnnotation();
    MappedSuperclassConfig mappedSuperConfig
      = _annotationCfg.getMappedSuperclassConfig();

    return (! _annotationCfg.isNull());
  }

  /**
   * Introspects.
   */
  //XXX: public MappedSuperType introspect(JClass type)
  public AbstractEnhancedType introspect(JClass type)
    throws ConfigException, SQLException
  {
    return null;

    /* XXX:
    String typeName = type.getName();

    MappedSuperType mappedSuperType = _mappedSuperMap.get(typeName);

    if (mappedSuperType != null)
      return mappedSuperType;

    try {
      mappedSuperType = _persistenceUnit.createMappedSuper(typeName, type);
      _mappedSuperMap.put(typeName, mappedSuperType);

      boolean isField = isField(type, mappedSuperConfig);

      if (isField)
        mappedSuperType.setFieldAccess(true);

      mappedSuperType.setInstanceClassName(type.getName() +
                                           "__ResinExt");
      mappedSuperType.setEnhanced(true);

      if (isField)
        introspectFields(_persistenceUnit, mappedSuperType, null,
                         type, entityConfig, false);
      else
        introspectMethods(_persistenceUnit, mappedSuperType, null,
                          type, entityConfig);

    } catch (ConfigException e) {
      mappedSuperType.setConfigException(e);

      throw e;
    } catch (SQLException e) {
      mappedSuperType.setConfigException(e);

      throw e;
    } catch (RuntimeException e) {
      mappedSuperType.setConfigException(e);

      throw e;
    }

    return mappedSuperType;
    */
  }
}
