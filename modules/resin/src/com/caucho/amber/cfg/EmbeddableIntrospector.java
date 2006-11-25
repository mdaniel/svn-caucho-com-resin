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
 * Configuration for an embeddable type
 */
public class EmbeddableIntrospector extends BaseConfigIntrospector {
  private static final L10N L = new L10N(EmbeddableIntrospector.class);
  private static final Logger log
    = Logger.getLogger(EmbeddableIntrospector.class.getName());

  HashMap<String, EmbeddableType> _embeddableMap
    = new HashMap<String, EmbeddableType>();

  /**
   * Creates the introspector.
   */
  public EmbeddableIntrospector(AmberPersistenceUnit persistenceUnit)
  {
    super(persistenceUnit);
  }

  /**
   * Returns true for embeddable type.
   */
  public boolean isEmbeddable(JClass type)
  {
    getInternalEmbeddableConfig(type);
    JAnnotation embeddableAnn = _annotationCfg.getAnnotation();
    EmbeddableConfig embeddableConfig = _annotationCfg.getEmbeddableConfig();

    return (! _annotationCfg.isNull());
  }

  /**
   * Introspects.
   */
  public EmbeddableType introspect(JClass type)
    throws ConfigException, SQLException
  {
    getInternalEmbeddableConfig(type);
    JAnnotation embeddableAnn = _annotationCfg.getAnnotation();
    EmbeddableConfig embeddableConfig = _annotationCfg.getEmbeddableConfig();

    String typeName = type.getName();

    EmbeddableType embeddableType = _embeddableMap.get(typeName);

    if (embeddableType != null)
      return embeddableType;

    try {
      embeddableType = _persistenceUnit.createEmbeddable(typeName, type);
      _embeddableMap.put(typeName, embeddableType);

      boolean isField = isField(type, embeddableConfig, true);

      if (isField)
        embeddableType.setFieldAccess(true);

      embeddableType.setInstanceClassName(type.getName() +
                                          "__ResinExt");
      embeddableType.setEnhanced(true);

      if (isField)
        introspectFields(_persistenceUnit, embeddableType, null,
                         type, embeddableConfig, true);
      else
        introspectMethods(_persistenceUnit, embeddableType, null,
                          type, embeddableConfig);

    } catch (ConfigException e) {
      embeddableType.setConfigException(e);

      throw e;
    } catch (RuntimeException e) {
      embeddableType.setConfigException(e);

      throw e;
    }

    return embeddableType;
  }
}
