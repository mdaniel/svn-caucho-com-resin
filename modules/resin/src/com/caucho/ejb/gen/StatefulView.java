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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.BusinessMethodGenerator;
import com.caucho.config.gen.View;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents a public interface to a stateful bean, e.g. a stateful view
 */
@Module
public class StatefulView<X,T> extends View<X,T> {
  private static final L10N L = new L10N(StatefulView.class);

  private StatefulGenerator<X> _sessionBean;

  private ArrayList<BusinessMethodGenerator<X,T>> _businessMethods
    = new ArrayList<BusinessMethodGenerator<X,T>>();

  public StatefulView(StatefulGenerator<X> bean, AnnotatedType<T> api)
  {
    super(bean, api);

    _sessionBean = bean;
  }

  public StatefulGenerator<X> getSessionBean()
  {
    return _sessionBean;
  }

  public String getContextClassName()
  {
    return getSessionBean().getClassName();
  }

  /**
   * True if the implementation is a proxy, i.e. an interface stub which
   * calls an instance class.
   */
  @Override
  public boolean isProxy()
  {
    return ! getViewClass().equals(getBeanClass());
  }

  @Override
  public String getViewClassName()
  {
    return getViewClass().getJavaClass().getSimpleName() + "__LocalProxy";
  }

  @Override
  public String getBeanClassName()
  {
    // XXX: 4.0.7
    // return getViewClass().getJavaClass().getSimpleName() + "__Bean";
    return getBeanClass().getJavaClass().getName();
  }

  /**
   * Returns the introspected methods
   */
  @Override
  public ArrayList<BusinessMethodGenerator<X,T>> getMethods()
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

      if (javaMethod.getName().startsWith("ejb")) {
        throw new ConfigException(L.l("{0}: '{1}' must not start with 'ejb'.  The EJB spec reserves all methods starting with ejb.",
                                      javaMethod.getDeclaringClass(),
                                      javaMethod.getName()));
      }

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
   * Generates code to create the provider
   */
  public void generateCreateProvider(JavaWriter out, String var)
    throws IOException
  {
    out.println();
    out.println("if (" + var + " == " + getViewClass().getJavaClass().getName() + ".class)");
    out.println("  return new " + getViewClassName() + "(getStatefulManager(), true);");
  }

  /**
   * Generates the view code.
   */
  @Override
  public void generate(JavaWriter out)
    throws IOException
  {
    // generateBean(out);

    out.println();
    out.println("public static class " + getViewClassName());

    if (isProxy()) {
      generateExtends(out);
      out.print("  implements StatefulProvider, ");
      out.println(getViewClass().getJavaClass().getName());
    }
    else {
      out.println("  extends " + getBeanClass().getJavaClass().getName());
      out.println("  implements StatefulProvider");
    }

    out.println("{");
    out.pushDepth();

    generateClassContent(out);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the view code.
   */
  public void generateBean(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public static class " + getBeanClassName());
    out.println("  extends " + getBeanClass().getJavaClass().getName());
    out.println("{");
    out.pushDepth();
    
    out.println("private transient " + getViewClassName() + " _context;");

    HashMap<String,Object> map = new HashMap<String,Object>();
    
    generateBeanPrologue(out, map);

    generatePostConstruct(out);
    //_postConstructInterceptor.generatePrologue(out, map);
    //_preDestroyInterceptor.generatePrologue(out, map);

    out.println();
    out.println(getBeanClassName() + "(" + getViewClassName() + " context)");
    out.println("{");
    out.pushDepth();
    out.println("_context = context;");

    map = new HashMap<String,Object>();
    generateBeanConstructor(out, map);    
    //_postConstructInterceptor.generateConstructor(out, map);
    //_preDestroyInterceptor.generateConstructor(out, map);

    //_postConstructInterceptor.generateCall(out);

    out.popDepth();
    out.println("}");

    // generateBusinessMethods(out);
    
    out.popDepth();
    out.println("}");
  }

  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private transient StatefulContext _context;");
    out.println("private transient StatefulManager _server;");

    if (isProxy()) {
      out.println("private " + getBeanClassName() + " _bean;");
    }

    out.println("private transient boolean _isValid;");
    out.println("private transient boolean _isActive;");

    /*
    out.println();
    out.println("private static final com.caucho.ejb.gen.XAManager _xa");
    out.println("  = new com.caucho.ejb.gen.XAManager();");
*/
    //generateBusinessPrologue(out);

    out.println();
    out.println(getViewClassName() + "(StatefulManager server)");
    out.println("{");
    out.pushDepth();

    generateSuper(out, "server");

    out.println("_server = server;");
    out.println("_isValid = true;");
    
    generateProxyConstructor(out);

    // ejb/1143
    if (isProxy()) {
      // XXX: 4.0.7
      out.println("_bean = (" + getBeanClassName() + ") _server.getProducer().newInstance();");
      /*
      out.println("try {");
      out.println("_bean = (" + getBeanClassName() + ") _ctor.newInstance();");
      out.println("} catch (Exception e) {");
      out.println("  throw new RuntimeException(e);");
      out.println("}");
      */
    }

    out.popDepth();
    out.println("}");

    out.println();
    out.println(getViewClassName() + "(StatefulManager server, boolean isProvider)");
    out.println("{");
    out.pushDepth();

    generateSuper(out, "server");

    out.println("_server = server;");
    out.println("_isValid = true;");

    out.popDepth();
    out.println("}");

    /*
    out.println();
    out.println("public " + getViewClassName() + "(StatefulManager server, javax.enterprise.context.spi.CreationalContext env)");
    out.println("{");
    out.pushDepth();

    generateSuper(out, "server");
    out.println("_server = server;");
    out.println("_bean = new " + getBeanClassName() + "(this);");

    out.popDepth();
    out.println("}");
    */

    generateSessionProvider(out);

    /*
    out.println();
    out.println("public " + getViewClassName()
                + "(StatefulManager server, "
                + getBeanClassName() + " bean)");
    out.println("{");
    generateSuper(out, "server");
    out.println("  _server = server;");
    out.println("  _bean = bean;");

    // generateBusinessConstructor(out);

    out.println("}");
    */

    out.println();
    out.println("public StatefulManager getStatefulManager()");
    out.println("{");
    out.println("  return _server;");
    out.println("}");
    out.println();

    out.println();
    out.println("void __caucho_setContext(StatefulContext context)");
    out.println("{");
    out.println("  _context = context;");
    out.println("}");

    generateBusinessMethods(out);
  }

  protected void generateSessionProvider(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public Object __caucho_createNew(javax.enterprise.inject.spi.InjectionTarget injectBean, javax.enterprise.context.spi.CreationalContext env)");
    out.println("{");
    out.println("  " + getViewClassName() + " bean"
                + " = new " + getViewClassName() + "(_server);");

    if (isProxy())
      out.println("  _server.initInstance(bean._bean, injectBean, bean, env);");
    else
      out.println("  _server.initInstance(bean, injectBean, bean, env);");
    out.println("  return bean;");
    out.println("}");
  }

  protected BusinessMethodGenerator<X,T>
    createMethod(AnnotatedMethod<? super T> apiMethod, int index)
  {
    AnnotatedMethod<? super X> implMethod = getMethod(apiMethod);

    if (implMethod == null)
      return null;

    StatefulMethod<X,T> bizMethod
      = new StatefulMethod<X,T>(this,
                                apiMethod,
                                implMethod,
                                index);

    return bizMethod;
  }

  protected void generateSuper(JavaWriter out, String serverVar)
    throws IOException
  {
  }

  protected void generateExtends(JavaWriter out)
    throws IOException
  {
  }

  protected AnnotatedMethod<? super X> findImplMethod(AnnotatedMethod<? super T> apiMethod)
  {
    AnnotatedMethod<? super X> implMethod = getMethod(apiMethod);

    if (implMethod != null)
      return implMethod;

    Method javaMethod = apiMethod.getJavaMember();
    
    throw new ConfigException(L.l("'{0}' method '{1}' has no corresponding implementation in '{2}'",
                                  javaMethod.getDeclaringClass().getSimpleName(),
                                  javaMethod.getName(),
                                  getBeanClass().getJavaClass().getName()));
  }
}
