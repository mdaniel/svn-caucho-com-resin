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
import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.util.logging.Logger;
import java.lang.reflect.*;

/**
 * Configuration for a particular view.
 */
public class EjbEntityHomeView extends EjbHomeView {
  private static final Logger log = Log.open(EjbEntityHomeView.class);
  private static final L10N L = new L10N(EjbEntityHomeView.class);

  private boolean _hasFindByPrimaryKey;
  
  /**
   * Creates a new entity bean configuration.
   */
  public EjbEntityHomeView(EjbBean bean, ApiClass apiClass, String prefix)
    throws ConfigException
  {
    super(bean, apiClass, prefix);
  }

  protected boolean isCMP()
  {
    return ((EjbEntityBean) getBean()).isCMP();
  }

  /**
   * Introspects the bean's methods.
   */
  protected void introspect()
    throws ConfigException
  {
    super.introspect();

    if (! _hasFindByPrimaryKey)
      throw new ConfigException(L.l("{0}: expected 'findByPrimaryKey'.  All entity homes must define findByPrimaryKey.",
				    getApiClass().getName()));
  }

  /**
   * Introspects an ejb method.
   */
  protected EjbMethod introspectEJBMethod(ApiMethod method)
    throws ConfigException
  {
    String methodName = method.getName();
    Class []paramTypes = method.getParameterTypes();

    if (methodName.startsWith("ejbCreate")) {
      String apiMethodName = "c" + methodName.substring(4);

      ApiMethod apiMethod = getApiClass().getMethod(apiMethodName, paramTypes);

      if (apiMethod == null) {
	/*
        validateNonFinalMethod(method.getName(), method.getParameterTypes());
        if (! isPrimaryKeyClass(method.getReturnType()))
          throw error(L.l("{0}: '{1}' must return '{2}'.  ejbCreate methods must return the primary key.",
                          method.getDeclaringClass().getName(),
                          getFullMethodName(method),
                          getClassName(primKeyClass)));
        if (isCMP())
          validateException(method, CreateException.class);
	*/
	// XXX: should be fatal?
	log.config(errorMissingMethod(getApiClass(), apiMethodName, method).getMessage());
	return null;
      }

      String postMethodName = "ejbPost" + methodName.substring(3);
      
      ApiMethod postCreateMethod
	= getImplClass().getMethod(postMethodName, method.getParameterTypes());

      return new EjbEntityCreateMethod(this, apiMethod,
				       method, postCreateMethod);
    }
    else if (methodName.startsWith("ejbFind")) {
      /*
      if (isCMP()) {
	throw new ConfigException(L.l("{0}: '{1}' is not allowed.  CMP beans can not define ejbFind method implementations.",
				      method.getDeclaringClass().getName(),
				      getFullMethodName(method)));
      }
      */
      
      if (methodName.equals("ejbFindByPrimaryKey")) {
	_hasFindByPrimaryKey = true;
    
	ApiClass primKeyClass = ((EjbEntityBean) getBean()).getPrimKeyClass();
      
	if (paramTypes.length != 1 || ! paramTypes[0].equals(primKeyClass))
	  throw error(L.l("{0}: '{1}' expected as only argument of {2}. ejbFindByPrimaryKey must take the primary key as its only argument.",
			  method.getDeclaringClass().getName(),
			  primKeyClass.getName(),
			  methodName));
      }
      
      String apiMethodName = "f" + methodName.substring(4);

      ApiMethod apiMethod = EjbBean.getMethod(getApiClass(),
					   apiMethodName,
					   paramTypes);

      if (apiMethod != null)
	return new EjbEntityFindMethod(this, apiMethod, method);
    }
    else if (methodName.startsWith("ejbHome")) {
      // XXX: test for "ejbHome"
      String apiMethodName = (Character.toLowerCase(methodName.charAt(7)) +
			      methodName.substring(8));

      ApiMethod apiMethod = EjbBean.getMethod(getApiClass(),
					   apiMethodName,
					   paramTypes);

      if (apiMethod == null) {
	log.config(errorMissingMethod(getApiClass(), apiMethodName, method).getMessage());
	return null;
      }

      validateImplMethod(method);

      return new EjbMethod(this, apiMethod, method);
    }
    
    return null;
  }

  /**
   * Introspects an ejb method.
   */
  protected EjbMethod introspectApiMethod(ApiMethod apiMethod)
    throws ConfigException
  {
    String methodName = apiMethod.getName();
    Class []paramTypes = apiMethod.getParameterTypes();

    if (methodName.equals("findByPrimaryKey")) {
      _hasFindByPrimaryKey = true;
      
      ApiClass primKeyClass = ((EjbEntityBean) getBean()).getPrimKeyClass();
    
      if (paramTypes.length != 1 || ! paramTypes[0].equals(primKeyClass))
	throw error(L.l("{0}: '{1}' expected as only argument of {2}. findByPrimaryKey must take the primary key as its only argument.",
			apiMethod.getDeclaringClass().getName(),
			primKeyClass.getName(),
			methodName));

    
      return new EjbEntityFindMethod(this, apiMethod);
    }
    else if (methodName.startsWith("find")) {
      if (isCMP()) {
	EjbMethodPattern pattern = findMethodPattern(apiMethod,
						     getPrefix());

	if (pattern == null)
	  throw error(L.l("{0}: '{1}' expects an ejb-ql query.  All find methods need queries defined in the EJB deployment descriptor.",
			  apiMethod.getDeclaringClass().getName(),
			  getFullMethodName(apiMethod)));
	
	String query = pattern.getQuery();

	EjbAmberFindMethod findMethod;
	findMethod = new EjbAmberFindMethod(this, apiMethod, query,
					    pattern.getQueryLocation());

	findMethod.setQueryLoadsBean(pattern.getQueryLoadsBean());

	return findMethod;
      }

      String name = methodName;
      name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
      
      throw errorMissingMethod(getImplClass(), "ejb" + name, apiMethod);
    }
    else if (methodName.startsWith("create")) {
      String name = apiMethod.getName();

      name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
      
      throw errorMissingMethod(getImplClass(), "ejb" + name, apiMethod);
    }
    else if (methodName.startsWith("ejb")) {
      throw new ConfigException(L.l("{0}: '{1}' forbidden.  ejbXXX methods are reserved by the EJB specification.",
				    apiMethod.getDeclaringClass().getName(),
				    getFullMethodName(apiMethod)));
    }
    else if (methodName.startsWith("remove")) {
      throw new ConfigException(L.l("{0}: '{1}' forbidden.  removeXXX methods are reserved by the EJB specification.",
				    apiMethod.getDeclaringClass().getName(),
				    getFullMethodName(apiMethod)));
    }
    else {
      String name = apiMethod.getName();

      name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
      
      throw errorMissingMethod(getImplClass(), "ejbHome" + name, apiMethod);
    }
  }
}
