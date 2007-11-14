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

import com.caucho.config.ConfigException;
import com.caucho.ejb.gen.AmberQueryMethod;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.ejb.ql.EjbSelectQuery;
import com.caucho.ejb.ql.QLParser;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

/**
 * Configuration for find method.
 */
public class EjbAmberFindMethod extends EjbMethod {
  private static final L10N L = new L10N(EjbAmberFindMethod.class);

  private String _query;
  private String _location;

  private boolean _queryLoadsBean = true;
  
  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public EjbAmberFindMethod(EjbView view, ApiMethod apiMethod,
			    String query, String location)
    throws ConfigException
  {
    super(view, apiMethod, null);

    if (apiMethod == null)
      throw new NullPointerException();

    _query = query;
    _location = location;
  }

  /**
   * Sets true if the query loads the bean.
   */
  public void setQueryLoadsBean(boolean queryLoadsBean)
  {
    _queryLoadsBean = queryLoadsBean;
  }

  /**
   * Assembles the method.
   */
  public BaseMethod assemble(ViewClass viewAssembler, String fullClassName)
    throws ConfigException
  {
    ApiMethod apiMethod = getApiMethod();
    EjbEntityBean bean = (EjbEntityBean) getView().getBean();
    
    QLParser parser =  new QLParser(bean,
				    apiMethod.getName(), apiMethod,
				    apiMethod.getReturnType());

    if (_location != null)
      parser.setLocation(_location);

    EjbSelectQuery query = (EjbSelectQuery) parser.parseQuery(_query);

    String returnEJB = parser.getReturnEJB();

    if (returnEJB == null || ! returnEJB.equals(bean.getEJBName()))
      throw new ConfigException(L.l("{0}: '{1}' query must return collection of '{2}'",
				    bean.getEJBClass().getName(),
				    apiMethod.getName(),
				    bean.getLocal().getName()));
    
    AmberQueryMethod queryMethod = new AmberQueryMethod(bean,
							getApiMethod(),
							fullClassName,
							getViewPrefix(),
							query);

    queryMethod.setQueryLoadsBean(_queryLoadsBean);

    return queryMethod;
  }
}
