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
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.type.Type;
import com.caucho.bytecode.JClassWrapper;
import com.caucho.config.ConfigException;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.gen.AbstractQueryMethod;
import com.caucho.ejb.gen.AmberSelectCollectionMethod;
import com.caucho.ejb.gen.AmberSelectMethod;
import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.ejb.ql.EjbSelectQuery;
import com.caucho.ejb.ql.QLParser;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

import javax.ejb.EJBLocalObject;
import java.util.Collection;

/**
 * Configuration for find method.
 */
public class EjbAmberSelectMethod extends EjbBaseMethod {
  private static final L10N L = new L10N(EjbAmberSelectMethod.class);

  private String _query;
  private String _location;

  private boolean _queryLoadsBean;
  
  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public EjbAmberSelectMethod(EjbEntityBean bean, ApiMethod method,
			      String query, String location)
    throws ConfigException
  {
    super(bean, method);

    _query = query;
    _location = location;
  }

  /**
   * Set true if the query should load the bean.
   */
  public void setQueryLoadsBean(boolean queryLoadsBean)
  {
    _queryLoadsBean = queryLoadsBean;
  }

  /**
   * Assembles the method.
   */
  public BaseMethod assemble(BeanAssembler assembler, String fullClassName)
    throws ConfigException
  {
    ApiMethod method = getMethod();

    QLParser parser =  new QLParser((EjbEntityBean) getBean(),
				    method.getName(), method,
				    method.getReturnType());

    parser.setLocation(_location);

    EjbSelectQuery query = (EjbSelectQuery) parser.parseQuery(_query);

    String returnEJB = parser.getReturnEJB();
    Class retType = method.getReturnType();

    EjbConfig ejbConfig = getBean().getConfig();
    EjbContainer ejbManager = ejbConfig.getEjbContainer();
    EjbEntityBean retBean = null;

    if (returnEJB != null)
      retBean = (EjbEntityBean) ejbConfig.getBeanConfig(returnEJB);
    
    AmberPersistenceUnit amberPersistenceUnit
      = ejbManager.createEjbPersistenceUnit();

    Type amberType = null;

    if (returnEJB != null) {
      amberType = amberPersistenceUnit.getEntityType(retBean.getAbstractSchemaName());

      if (amberType == null)
	throw new NullPointerException("No amber entity for " + returnEJB);
    }
    else if (EJBLocalObject.class.isAssignableFrom(retType)) {
      EjbEntityBean targetBean = getBean().getConfig().findEntityByLocal(retType);

      Class queryRetType = parser.getSelectExpr().getJavaType();
      
      if (queryRetType != null
	  && ! Object.class.equals(queryRetType)
	  && ! EJBLocalObject.class.equals(queryRetType)) {
	throw new ConfigException(L.l("Mismatched return type '{0}' in\n{1}",
				      retType.getName(), _query));
      }

      amberType = amberPersistenceUnit.getEntityType(targetBean.getAbstractSchemaName());
    }
    else if (! Collection.class.isAssignableFrom(retType))
      amberType = amberPersistenceUnit.createType(JClassWrapper.create(retType));

    AbstractQueryMethod queryMethod;
    
    if (Collection.class.isAssignableFrom(retType))
      queryMethod = new AmberSelectCollectionMethod((EjbEntityBean) getBean(),
						    getMethod(),
						    fullClassName,
						    query);
    else
      queryMethod =  new AmberSelectMethod((EjbEntityBean) getBean(),
					   method,
					   fullClassName,
					   query,
					   amberType);

    queryMethod.setQueryLoadsBean(_queryLoadsBean);

    return queryMethod;
  }
}
