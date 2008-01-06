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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.type.EmbeddableType;
import com.caucho.bytecode.*;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

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
    getInternalEmbeddableConfig(type, _annotationCfg);
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
    getInternalEmbeddableConfig(type, _annotationCfg);
    JAnnotation embeddableAnn = _annotationCfg.getAnnotation();
    EmbeddableConfig embeddableConfig = _annotationCfg.getEmbeddableConfig();

    String typeName = type.getName();

    EmbeddableType embeddableType = _embeddableMap.get(typeName);

    if (embeddableType != null)
      return embeddableType;

    try {
      embeddableType = _persistenceUnit.createEmbeddable(typeName, type);
      _embeddableMap.put(typeName, embeddableType);

      boolean isField = isField(type, embeddableConfig);

      if (isField)
        embeddableType.setFieldAccess(true);

      // XXX: jpa/0u21
      JAnnotation ann = type.getAnnotation(javax.persistence.Embeddable.class);

      if (ann == null) {
        isField = true;
        embeddableType.setIdClass(true);
        _persistenceUnit.getAmberContainer().addEmbeddable(typeName,
                                                           embeddableType);
      }

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
      if (embeddableType != null)
	embeddableType.setConfigException(e);

      throw e;
    } catch (RuntimeException e) {
      if (embeddableType != null)
	embeddableType.setConfigException(e);

      throw e;
    }

    return embeddableType;
  }

  boolean isField(JClass type,
                  AbstractEnhancedConfig typeConfig)
    throws ConfigException
  {
    for (JMethod method : type.getDeclaredMethods()) {
      JAnnotation ann[] = method.getDeclaredAnnotations();

      for (int i = 0; ann != null && i < ann.length; i++) {
	if (isPropertyAnnotation(ann[i].getType()))
	  return false;
      }
    }

    return true;
  }

  private boolean isPropertyAnnotation(String name)
  {
    return ("javax.persistence.Basic".equals(name)
	    || "javax.persistence.Column".equals(name));
  }
}
