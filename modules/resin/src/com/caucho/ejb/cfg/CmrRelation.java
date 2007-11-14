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

package com.caucho.ejb.cfg;

import com.caucho.amber.field.AmberField;
import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.ejb.gen.EntityBean;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.ejb.EJBLocalObject;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Abstract relation.
 */
public class CmrRelation extends CmpProperty {
  private static final L10N L = new L10N(CmrRelation.class);

  private String _relationName;
  
  private String _fieldName;
  private boolean _hasGetter;

  private CmrRelation _targetRelation;

  /**
   * Creates a new cmp-relation
   */
  public CmrRelation(EjbEntityBean entityBean)
    throws ConfigException
  {
    super(entityBean);
  }

  /**
   * Creates a new cmp-relation
   */
  public CmrRelation(EjbEntityBean entityBean, String fieldName)
    throws ConfigException
  {
    super(entityBean);

    setFieldName(fieldName);

    ApiMethod getter = getGetter();

    if (! getter.isAbstract() && ! entityBean.isAllowPOJO())
      throw new ConfigException(L.l("{0}: '{1}' must have an abstract getter method. cmr-relations must have abstract getter methods returning a local interface.",
				    entityBean.getEJBClass().getName(),
				    getter.getFullName()));

    Class retType = getter.getReturnType();

    if (! EJBLocalObject.class.isAssignableFrom(retType)
	&& ! Collection.class.isAssignableFrom(retType)
	&& ! Map.class.isAssignableFrom(retType)) {
      throw new ConfigException(L.l("{0}: '{1}' must return an EJBLocalObject or a Collection. cmr-relations must have abstract getter methods returning a local interface.",
				    entityBean.getEJBClass().getName(),
				    getter.getFullName()));
    }

    ApiMethod setter = getSetter();

    if (setter == null)
      return;

    Class []paramTypes = setter.getParameterTypes();

    if (! retType.equals(paramTypes[0]))
      throw new ConfigException(L.l("{0}: '{1}' must return an '{2}'.  Persistent setters must match the getter types .",
				    entityBean.getEJBClass().getName(),
				    setter.getFullName(),
				    retType.getName()));
  }

  /**
   * Returns the relation name.
   */
  public String getRelationName()
  {
    return _relationName;
  }

  /**
   * Sets the relation name.
   */
  public void setRelationName(String name)
  {
    _relationName = name;
  }

  /**
   * Returns the target bean
   */
  public EjbEntityBean getTargetBean()
  {
    return null;
  }

  /**
   * Returns the target type.
   */
  public ApiClass getTargetType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets a target relation.
   */
  public void setTarget(CmrRelation target)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true for a collection.
   */
  public boolean isCollection()
  {
    return false;
  }

  /**
   * Sets the paired target relation.
   */
  public void setTargetRelation(CmrRelation target)
  {
    _targetRelation = target;
  }

  /**
   * Gets the paired target relation.
   */
  public CmrRelation getTargetRelation()
  {
    return _targetRelation;
  }

  /**
   * Link amber.
   */
  public void linkAmber()
    throws ConfigException
  {
  }

  /**
   * Assemble any bean methods.
   */
  public void assemble(EntityBean bean)
  {
    
  }

  /**
   * Set true for having a getter.
   */
  public void setHasGetter(boolean hasGetter)
  {
    _hasGetter = true;
  }

  /**
   * Set true for having a getter.
   */
  public boolean getHasGetter()
  {
    return _hasGetter;
  }
  
  /**
   * Create any bean methods.
   */
  public EjbMethod createGetter(EjbView view,
				ApiMethod apiMethod,
				ApiMethod implMethod)
    throws ConfigException
  {
    return new CmpGetter(view, apiMethod, implMethod);    
  }

  /**
   * Creates the amber type.
   */
  public AmberField assembleAmber(EntityType type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates the after commit code.
   */
  public void generateAfterCommit(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the destroy method.
   */
  public void generateDestroy(JavaWriter out)
    throws IOException
  {
  }

  public String toString()
  {
    return "CmrRelation[" + getName() + "]";
  }
}
