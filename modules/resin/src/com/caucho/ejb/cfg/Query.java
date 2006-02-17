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

import java.util.*;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.util.*;
import com.caucho.ejb.*;
import com.caucho.config.ConfigException;

/**
 * <pre>
 * query ::= (description?,
 *            query-method,
 *            result-type-mapping?,
 *            ejb-ql)
 * </pre>            
 */
public class Query {
  private static L10N L = new L10N(MethodSignature.class);

  private EjbEntityBean _entity;

  private String _location;
  
  private String _description;

  private MethodSignature _signature;
  
  private String methodName;
  private String methodIntf;

  private Object value;

  private String _ejbQL;

  private ArrayList paramTypes;

  public Query(EjbEntityBean entity)
  {
    _entity = entity;
  }

  public void setConfigLocation(String filename, int line)
  {
    if (filename != null)
      _location = filename + ':' + line + ": ";
  }

  public String getConfigLocation()
  {
    return _location;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Sets the query method.
   */
  public void setQueryMethod(QueryMethod queryMethod)
    throws ConfigException
  {
    _signature = queryMethod.getSignature();

    String methodName = _signature.getName();
    
    if (methodName.equals("findByPrimaryKey")) {
      throw new ConfigException(L.l("'findByPrimaryKey' can't be defined in a query."));
    }
    else if (methodName.startsWith("find")) {
      JMethod method = _entity.findMethod(_signature, _entity.getRemoteHome(), "home");

      if (method == null)
	method = _entity.findMethod(_signature, _entity.getLocalHome(), "local-home");

      if (method == null)
	throw new ConfigException(L.l("Query method '{0}' must be defined in either the <home> or <local-home>.",
				      _signature.toSignatureString()));
    }
    else if (methodName.startsWith("ejbSelect")) {
      JMethod method = _entity.findMethod(_signature,
					  _entity.getEJBClassWrapper(),
					  null);

      if (method == null)
	throw new ConfigException(L.l("{0}: Query method '{1}' must be defined.",
				      _entity.getEJBClass().getName(),
				      _signature.toSignatureString()));
    }
    else
      throw new ConfigException(L.l("'{0}' is an invalid method name for an ejb-ql query.  Only findXXX and ejbSelectXXX methods have queries.",
				    methodName));
  }

  /**
   * Returns the signature.
   */
  public MethodSignature getSignature()
  {
    return _signature;
  }

  /**
   * Sets the query.
   */
  public void setEjbQl(String ejbQL)
  {
    _ejbQL = ejbQL;
  }

  /**
   * Returns the query.
   */
  public String getEjbQl()
  {
    return _ejbQL;
  }
}
