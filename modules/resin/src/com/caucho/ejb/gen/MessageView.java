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

import com.caucho.config.gen.*;
import com.caucho.config.*;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.*;
import java.lang.reflect.Method;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

/**
 * Represents a public interface to a stateful bean, e.g. a stateful view
 */
@Module
public class MessageView<X,T> extends View<X,T> {
  private static final L10N L = new L10N(MessageView.class);

  private MessageGenerator<X> _messageBean;
  
  private ArrayList<BusinessMethodGenerator<X,T>> _businessMethods
    = new ArrayList<BusinessMethodGenerator<X,T>>();

  public MessageView(MessageGenerator<X> bean, AnnotatedType<T> api)
  {
    super(bean, api);

    _messageBean = bean;
  }

  public MessageGenerator<X> getMessageBean()
  {
    return _messageBean;
  }

  public String getContextClassName()
  {
    return getMessageBean().getClassName();
  }

  @Override
  public String getViewClassName()
  {
    return getMessageBean().getClassName();
  }

  /**
   * Returns the introspected methods
   */
  @Override
  public ArrayList<? extends BusinessMethodGenerator<X,T>> getMethods()
  {
    return _businessMethods;
  }

  /**
   * Introspects the APIs methods, producing a business method for
   * each.
   */
  @Override
  public void introspect()
  {
    AnnotatedType<T> apiClass = getViewClass();

    for (AnnotatedMethod<? super T> apiMethod : apiClass.getMethods()) {
      Method javaMethod = apiMethod.getJavaMember();
      
      if (javaMethod.getDeclaringClass().equals(Object.class))
	continue;
      if (javaMethod.getDeclaringClass().getName().startsWith("javax.ejb.")
	  && ! javaMethod.getName().equals("remove"))
	continue;

      int index = _businessMethods.size();
      
      BusinessMethodGenerator<X,T> bizMethod = createMethod(apiMethod, index);
      
      if (bizMethod != null) {
	bizMethod.introspect(bizMethod.getApiMethod(),
			     bizMethod.getImplMethod());
	
	_businessMethods.add(bizMethod);
      }
    }
  }

  /**
   * Generates the view code.
   */
  @Override
  public void generate(JavaWriter out)
    throws IOException
  {
    HashMap<String,Object> map = new HashMap<String,Object>();

    map.put("caucho.ejb.xa", "done");

    out.println();
    out.println("private static final com.caucho.ejb.util.XAManager _xa");
    out.println("  = new com.caucho.ejb.util.XAManager();");

    /* ejb/0fbm
    for (BusinessMethodGenerator bizMethod : _businessMethods) {
      bizMethod.generatePrologueTop(out, map);
    }
    */
    
    for (BusinessMethodGenerator<X,T> bizMethod : _businessMethods) {
      bizMethod.generate(out, map);
    }
  }

  protected BusinessMethodGenerator<X,T>
    createMethod(AnnotatedMethod<? super T> apiMethod, int index)
  {
    AnnotatedMethod<? super X> implMethod = findImplMethod(apiMethod);

    if (implMethod == null)
      return null;
    
    BusinessMethodGenerator<X,T> bizMethod
      = new MessageMethod<X,T>(this,
                               apiMethod,
                               implMethod,
                               index);

    return bizMethod;
  }
  
  protected AnnotatedMethod<? super X> findImplMethod(AnnotatedMethod<? super T> apiMethod)
  {
    AnnotatedMethod<? super X> implMethod = getMethod(getBeanClass(), apiMethod.getJavaMember());

    if (implMethod != null)
      return implMethod;
  
    throw ConfigException.create(apiMethod.getJavaMember(),
				 L.l("api method has no corresponding implementation in '{0}'",
				     getBeanClass().getJavaClass().getName()));
  }
  
  protected AnnotatedMethod<? super X> getImplMethod(String name, Class<?> []param)
  {
    return getMethod(getBeanClass(), name, param);
  }
}
