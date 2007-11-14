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
import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.java.gen.CallChain;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Configuration for a particular view.
 */
public class EjbView {
  private static final Logger log = Log.open(EjbView.class);
  private static final L10N L = new L10N(EjbView.class);

  private EjbBean _bean;

  private ArrayList<ApiClass> _apiList;

  private String _prefix;
  private String _suffix;

  private HashMap<String,EjbMethod> _methodMap =
    new HashMap<String,EjbMethod>();

  /**
   * Creates a new entity bean configuration.
   */
  public EjbView(EjbBean bean, ApiClass apiClass, String prefix, String suffix)
    throws ConfigException
  {
    _bean = bean;
    _apiList = new ArrayList<ApiClass>();
    _apiList.add(apiClass);
    _prefix = prefix;
    _suffix = suffix;
  }

  /**
   * Creates a new entity bean configuration.
   */
  public EjbView(EjbBean bean,
		 ArrayList<ApiClass> apiList,
		 String prefix,
		 String suffix)
    throws ConfigException
  {
    _bean = bean;
    _apiList = apiList;
    _prefix = prefix;
    _suffix = suffix;
  }

  /**
   * Returns the bean.
   */
  protected EjbBean getBean()
  {
    return _bean;
  }

  protected ApiClass getApiClass()
  {
    return _apiList.get(0);
  }

  /**
   * Returns the api class for the view.
   */
  protected ArrayList<ApiClass> getApiList()
  {
    return _apiList;
  }

  /**
   * Returns the implementation class for the view.
   */
  protected ApiClass getImplClass()
  {
    return _bean.getEJBClassWrapper();
  }

  /**
   * Returns the prefix.
   */
  protected String getPrefix()
  {
    return _prefix;
  }

  /**
   * Returns the suffix.
   */
  protected String getSuffix()
  {
    return _suffix;
  }

  /**
   * Returns true for a local view.
   */
  protected boolean isLocal()
  {
    return "Local".equals(_prefix);
  }

  /**
   * Returns the list of methods.
   */
  public ArrayList<EjbMethod> getMethods()
  {
    ArrayList<EjbMethod> methods = new ArrayList<EjbMethod>();

    methods.addAll(_methodMap.values());

    return methods;
  }

  /**
   * Introspects the bean's methods.
   */
  protected void introspect()
    throws ConfigException
  {
    // find API methods matching an implementation method
    for (ApiMethod method : getImplClass().getMethods()) {
      EjbMethod ejbMethod = null;

      String name = method.getName();

      if (ApiClass.OBJECT.getMethod(name, method.getParameterTypes()) != null
          && ! name.equals("toString")) {
      }
      else if (name.startsWith("ejb"))
        ejbMethod = introspectEJBMethod(method);
      else
        ejbMethod = introspectBusinessMethod(method);

      if (ejbMethod != null) {
        _methodMap.put(getFullMethodName(ejbMethod.getApiMethod()), ejbMethod);
      }
    }

    // find API methods with no matching implementation method
    for (ApiMethod method : EjbBean.getMethods(_apiList)) {
      if (method.getDeclaringClass().getName().startsWith("javax.ejb"))
        continue;
      else if (ApiClass.OBJECT.getMethod(method.getName(),
					 method.getParameterTypes()) != null) {
        continue;
      }

      EjbMethod ejbMethod = _methodMap.get(getFullMethodName(method));

      if (ejbMethod != null)
        continue;

      ejbMethod = introspectApiMethod(method);

      if (ejbMethod != null) {
        _methodMap.put(getFullMethodName(ejbMethod.getApiMethod()), ejbMethod);

        validateApiMethod(ejbMethod.getApiMethod());
      }
    }
  }

  /**
   * Introspects an ejb method.
   */
  protected EjbMethod introspectEJBMethod(ApiMethod method)
    throws ConfigException
  {
    return null;
  }

  /**
   * Introspects a business method.
   */
  protected EjbMethod introspectBusinessMethod(ApiMethod implMethod)
    throws ConfigException
  {
    ApiMethod apiMethod = EjbBean.getMethod(_apiList,
					    implMethod.getName(),
					    implMethod.getParameterTypes());

    if (apiMethod == null)
      return null;

    return createBusinessMethod(apiMethod, implMethod);
  }

  /**
   * Creates a new business method.
   */
  protected EjbMethod createBusinessMethod(ApiMethod apiMethod,
                                           ApiMethod implMethod)
    throws ConfigException
  {
    validateImplMethod(implMethod);

    return new EjbMethod(this, apiMethod, implMethod);
  }

  /**
   * Validate impl method.
   */
  protected void validateImplMethod(ApiMethod implMethod)
    throws ConfigException
  {
    if (! implMethod.isPublic()) {
      throw error(L.l("{0}: '{1}' must be public.  Business method implementations must be public.",
                      implMethod.getDeclaringClass().getName(),
                      getFullMethodName(implMethod)));
    }

    if (implMethod.isStatic()) {
      throw error(L.l("{0}: '{1}' must not be static.  Business method implementations must not be static.",
                      implMethod.getDeclaringClass().getName(),
                      getFullMethodName(implMethod)));
    }

    if (implMethod.isAbstract()) {
      throw error(L.l("{0}: '{1}' must not be abstract.  Business methods must be implemented.",
                      implMethod.getDeclaringClass().getName(),
                      getFullMethodName(implMethod)));
    }
  }

  /**
   * Introspects a method in the view api which does not exist in
   * implementation bean.
   */
  protected EjbMethod introspectApiMethod(ApiMethod apiMethod)
    throws ConfigException
  {
    /*
      if (apiMethod.getName().startsWith("ejbPostCreate")) {
      // XXX: properly checked?
      return null;
      }
      else
    */
    if (apiMethod.getName().startsWith("ejb")) {
      throw error(L.l("{0}: '{1}' must not start with 'ejb'. The EJB spec reserves all methods starting with ejb.",
                      apiMethod.getDeclaringClass().getName(),
                      getFullMethodName(apiMethod)));
    }
    else
      throw errorMissingMethod(getImplClass(), apiMethod.getName(), apiMethod);
  }

  /**
   * Validates an API method.
   */
  protected void validateApiMethod(ApiMethod apiMethod)
    throws ConfigException
  {
    if ("Remote".equals(_prefix))
      validateException(apiMethod, RemoteException.class);
  }

  protected ConfigException errorMissingMethod(ApiClass expectedClass,
                                               String expectedName,
                                               ApiMethod matchMethod)
  {
    return error(L.l("{0}: missing '{1}' method needed to match {2}.{3}",
                     expectedClass.getName(),
                     getFullMethodName(expectedName,
                                       matchMethod.getParameterTypes()),
                     matchMethod.getDeclaringClass().getSimpleName(),
                     getFullMethodName(matchMethod)));
  }

  /**
   * Assembles the generator.
   */
  protected void assembleView(BeanAssembler assembler,
                              String fullClassName)
    throws ConfigException
  {
  }

  /**
   * Assembles the generator methods.
   */
  protected void assembleMethods(BeanAssembler assembler,
                                 ViewClass viewClass,
                                 String fullClassName)
    throws ConfigException
  {
    ArrayList<EjbMethod> methods = getMethods();

    for (int i = 0; i < methods.size(); i++) {
      EjbMethod method = methods.get(i);

      method.assembleBean(assembler, fullClassName);

      viewClass.addMethod(method.assemble(viewClass, fullClassName));
    }
  }

  /**
   * Finds the matching method pattern.
   */
  protected EjbMethodPattern findMethodPattern(ApiMethod apiMethod,
                                               String prefix)
  {
    return _bean.getMethodPattern(apiMethod, prefix);
  }

  /**
   * Returns the transaction chain.
   */
  protected CallChain getTransactionChain(CallChain callChain,
                                          ApiMethod apiMethod,
                                          ApiMethod implMethod,
                                          String prefix)
  {
    return _bean.getTransactionChain(callChain, apiMethod, implMethod, prefix);
  }

  /**
   * Returns the transaction chain.
   */
  protected CallChain getSecurityChain(CallChain callChain,
                                       ApiMethod apiMethod,
                                       String prefix)
  {
    return _bean.getSecurityChain(callChain, apiMethod, prefix);
  }

  /**
   * Validate the exceptions.
   */
  protected void validateException(ApiMethod method, Class exn)
    throws ConfigException
  {
    _bean.validateException(method, exn);
  }

  /**
   * Validate the exceptions.
   */
  protected void validateExceptions(ApiMethod method, Class []exn)
    throws ConfigException
  {
    _bean.validateExceptions(method, exn);
  }

  /**
   * Returns a printable class name.
   */
  static String getClassName(Class cl)
  {
    if (cl != null)
      return cl.getName();
    else
      return null;
  }

  static String getFullMethodName(ApiMethod method)
  {
    return EjbBean.getFullMethodName(method);
  }

  static String getFullMethodName(String methodName, Class []paramTypes)
  {
    return EjbBean.getFullMethodName(methodName, paramTypes);
  }

  static String getShortClassName(ApiClass cl)
  {
    return cl.getJavaClass().getSimpleName();
  }

  /**
   * Returns an error.
   */
  public ConfigException error(String msg)
  {
    return new ConfigException(msg);
  }
}
