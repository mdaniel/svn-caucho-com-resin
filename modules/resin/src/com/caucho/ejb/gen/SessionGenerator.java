/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.gen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.gen.View;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.config.types.InjectionTarget;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Generates the skeleton for a session bean.
 */
@Module
abstract public class SessionGenerator<X> extends BeanGenerator<X> {
  private static final L10N L = new L10N(SessionGenerator.class);
  
  private ArrayList<AnnotatedType<? super X>> _localApi;
  private ArrayList<AnnotatedType<? super X>> _remoteApi;
  
  private ArrayList<AnnotatedMethod<? super X>> _annotatedMethods
    = new ArrayList<AnnotatedMethod<? super X>>();

  protected String _contextClassName = "dummy";

  public SessionGenerator(String ejbName, 
                          AnnotatedType<X> beanType,
                          ArrayList<AnnotatedType<? super X>> localApi,
                          ArrayList<AnnotatedType<? super X>> remoteApi, 
                          String beanTypeName)
  {
    super(toFullClassName(ejbName, beanType.getJavaClass().getName(), beanTypeName),
          beanType);

    _contextClassName = "dummy";

    _localApi = new ArrayList<AnnotatedType<? super X>>(localApi);

    _remoteApi = new ArrayList<AnnotatedType<? super X>>(remoteApi);
  }

  public static String toFullClassName(String ejbName, String className,
                                       String beanType)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(className);
    
    sb.append("__");
    
    // XXX: restore this to distinguish similar beans
    /*
    if (!Character.isJavaIdentifierStart(ejbName.charAt(0)))
      sb.append('_');

    for (int i = 0; i < ejbName.length(); i++) {
      char ch = ejbName.charAt(i);

      if (ch == '/')
        sb.append('.');
      else if (Character.isJavaIdentifierPart(ch))
        sb.append(ch);
      else
        sb.append('_');
    }

    sb.append(".");
    sb.append(className);
    sb.append("__");
    */
    
    sb.append(beanType);
    sb.append("Context");

    return sb.toString();
  }

  public boolean isStateless() 
  {
    return false;
  }
  
  @Override
  public SessionView<X> getView()
  {
    return (SessionView<X>) super.getView();
  }

  /**
   * Returns the local API list.
   */
  public ArrayList<AnnotatedType<? super X>> getLocalApi()
  {
    return _localApi;
  }

  /**
   * Returns the remote API list.
   */
  public ArrayList<AnnotatedType<? super X>> getRemoteApi()
  {
    return _remoteApi;
  }
  
  /**
   * Returns the merged annotated methods
   */
  protected ArrayList<AnnotatedMethod<? super X>> getAnnotatedMethods()
  {
    return _annotatedMethods;
  }

  /**
   * Introspects the bean.
   */
  @Override
  public void introspect()
  {
    super.introspect();

    // no interface view
    if (_localApi.size() == 0 && _remoteApi.size() == 0) {
      AnnotatedType<? super X> localDefault = introspectLocalDefault();
      
      if (localDefault.getJavaClass().isInterface())
        _localApi.add(localDefault); 
    }
    
    for (AnnotatedType<? super X> type : _localApi) {
      for (AnnotatedMethod<? super X> method : type.getMethods()) {
        introspectMethod(method);
      }
    }
    
    for (AnnotatedType<? super X> type : _remoteApi) {
      for (AnnotatedMethod<? super X> method : type.getMethods()) {
        introspectMethod(method);
      }
    }
    
    getView().introspect();
  }
  
  private void introspectMethod(AnnotatedMethod<? super X> method)
  {
    AnnotatedMethod<? super X> oldMethod 
      = findMethod(_annotatedMethods, method);
    
    if (oldMethod != null) {
      // XXX: merge annotations
      return;
    }
    
    AnnotatedMethod<? super X> baseMethod
      = findMethod(getBeanType().getMethods(), method);
    
    if (baseMethod == null)
      throw new IllegalStateException(L.l("{0} does not have a matching base method in {1}",
                                          method, getBeanType()));
    
    // XXX: merge annotations
    
    _annotatedMethods.add(baseMethod);
  }
  
  private AnnotatedMethod<? super X> 
  findMethod(Collection<AnnotatedMethod<? super X>> methodList,
             AnnotatedMethod<? super X> method)
  {
    for (AnnotatedMethod<? super X> oldMethod : methodList) {
      if (AnnotatedTypeUtil.isMatch(oldMethod, method)) {
        return oldMethod;
      }
    }
    
    return null;
  }

  protected AnnotatedType<? super X> introspectLocalDefault()
  {
    return getBeanType();
  }
  /**
   * Generates the local view for the given class
   */
  abstract protected View<X> createView();

  abstract protected void generateContext(JavaWriter out) throws IOException;
}
