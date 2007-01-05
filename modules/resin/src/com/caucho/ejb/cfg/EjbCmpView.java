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

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Configuration for a cmp view.
 */
public class EjbCmpView extends EjbEntityView {
  private static final Logger log = Log.open(EjbCmpView.class);
  private static final L10N L = new L10N(EjbCmpView.class);

  private EjbEntityBean _entityBean;

  /**
   * Creates a new entity bean configuration.
   */
  public EjbCmpView(EjbEntityBean bean,
		    ArrayList<JClass> apiClass,
		    String prefix)
    throws ConfigException
  {
    super(bean, apiClass, prefix);

    _entityBean = bean;
  }

  /**
   * Introspects an ejb method.
   */
  protected EjbMethod introspectEJBMethod(JMethod method)
    throws ConfigException
  {
    String methodName = method.getName();
    JClass []paramTypes = method.getParameterTypes();

    if (methodName.startsWith("ejbSelect") && method.isAbstract()) {
      _entityBean.addStubMethod(method);

      return null;
    }
    
    return super.introspectEJBMethod(method);
  }

  /**
   * Creates a new business method.
   */
  protected EjbMethod createBusinessMethod(JMethod apiMethod,
					   JMethod implMethod)
    throws ConfigException
  {
    String methodName = implMethod.getName();
    JClass []paramTypes = implMethod.getParameterTypes();

    if (methodName.startsWith("get") &&
	methodName.length() > 3 &&
	paramTypes.length == 0) {
      String fieldName = toFieldName(methodName.substring(3));

      CmpField field = _entityBean.getCmpField(fieldName);

      if (field != null) {
	validateCmpMethod(implMethod);
	
	if (isLocal() && apiMethod.getExceptionTypes().length != 0) {
	  throw new ConfigException(L.l("{0}: '{1}' must not throw {2}.  Container managed fields and relations must not throw exceptions.",
					apiMethod.getDeclaringClass().getName(),
					EjbBean.getFullMethodName(apiMethod),
					apiMethod.getExceptionTypes()[0].getName()));
	}

	if (field.isId())
	  return new CmpIdGetter(this, apiMethod, implMethod);
	else
	  return new CmpGetter(this, apiMethod, implMethod);
      }
      
      CmrRelation rel = _entityBean.getRelation(fieldName);

      if (rel != null) {
	validateCmpMethod(implMethod);

	rel.setHasGetter(true);

	return rel.createGetter(this, apiMethod, implMethod);
      }
      
      if (! implMethod.isAbstract()) {
	validateCmpMethod(implMethod);

	// return new CmpGetter(this, apiMethod, implMethod);
	
	return new EjbMethod(this, apiMethod, implMethod);
      }
    }
    else if (methodName.startsWith("get") &&
	methodName.length() > 3 &&
	paramTypes.length == 1) {
      String fieldName = toFieldName(methodName.substring(3));
      
      CmrRelation rel = _entityBean.getRelation(fieldName);

      if (rel instanceof CmrMap) {
	CmrMap map = (CmrMap) rel;
	
	validateCmpMethod(implMethod);

	rel.setHasGetter(true);

	// return new EjbMapGetter(this, apiMethod, implMethod, map);
	return new CmpGetter(this, apiMethod, implMethod);
      }
    }
    else if (methodName.startsWith("set") &&
	     methodName.length() > 3 &&
	     paramTypes.length == 1) {
      String fieldName = toFieldName(methodName.substring(3));

      CmpField field = _entityBean.getCmpField(fieldName);
      
      if (field != null) {
	validateCmpMethod(implMethod);
	
	if (isLocal() && apiMethod.getExceptionTypes().length != 0) {
	  throw new ConfigException(L.l("{0}: '{1}' must not throw {2}.  Container managed fields and relations must not throw exceptions.",
					_entityBean.getEJBClass().getName(),
					EjbBean.getFullMethodName(apiMethod),
					apiMethod.getExceptionTypes()[0].getName()));
	}

	return new EjbMethod(this, apiMethod, implMethod);
      }
      
      CmrRelation rel = _entityBean.getRelation(fieldName);

      if (rel instanceof CmrOneToMany) {
	validateCmpMethod(implMethod);

	return new CmpCollectionSetter(this, apiMethod, implMethod);
	// return new CmpRelationGetter(this, apiMethod, implMethod, rel);
      }
      else if (rel instanceof CmrManyToOne) {
	validateCmpMethod(implMethod);
	
	CmrManyToOne manyToOne = (CmrManyToOne) rel;
	
	return new EjbManyToOneSetMethod(this, apiMethod, implMethod, manyToOne);
      }
      else if (rel instanceof CmrManyToMany) {
	validateCmpMethod(implMethod);

	return new CmpCollectionSetter(this, apiMethod, implMethod);
	// return new CmpRelationGetter(this, apiMethod, implMethod, rel);
      }
      
      if (! implMethod.isAbstract()) {
	validateCmpMethod(implMethod);

	return new EjbMethod(this, apiMethod, implMethod);
      }
      else
	throw new ConfigException(L.l("{0}: abstract setter {1}.",
				      implMethod.getDeclaringClass().getName(),
				      getFullMethodName(implMethod)));
    }
      

    return super.createBusinessMethod(apiMethod, implMethod);
  }

  protected String toFieldName(String name)
  {
    if (name.length() == 0)
      return "";
    else if (name.length() == 1)
      return String.valueOf(Character.toLowerCase(name.charAt(0)));
    else if (Character.isUpperCase(name.charAt(1)))
      return name;
    else
      return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  /**
   * Validate impl method.
   */
  protected void validateCmpMethod(JMethod implMethod)
    throws ConfigException
  {
    if (! implMethod.isPublic()) {
      throw error(L.l("{0}: `{1}' must be public.  CMP method implementations must be public.",
                      implMethod.getDeclaringClass().getName(),
                      getFullMethodName(implMethod)));
    }
    
    if (implMethod.isStatic()) {
      throw error(L.l("{0}: `{1}' must not be static.  CMP method implementations must not be static.",
                      implMethod.getDeclaringClass().getName(),
                      getFullMethodName(implMethod)));
    }
  }
}
