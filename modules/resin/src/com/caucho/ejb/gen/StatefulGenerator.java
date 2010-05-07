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

import javax.ejb.Stateful;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Generates the skeleton for a stateful bean.
 */
@Module
public class StatefulGenerator<X> extends SessionGenerator<X> 
{
  private static final L10N L = new L10N(StatefulGenerator.class);
  
  public StatefulGenerator(String ejbName, AnnotatedType<X> beanType,
                           ArrayList<AnnotatedType<? super X>> localApi,
                           ArrayList<AnnotatedType<? super X>> remoteApi)
  {
    super(ejbName, beanType, localApi, remoteApi, 
          Stateful.class.getSimpleName());
    
    InjectManager manager = InjectManager.create();
    
    _aspectBeanFactory = new StatefulAspectBeanFactory<X>(manager, getBeanType());
  }

  @Override
  public boolean isStateless()
  {
    return false;
  }

  public String getContextClassName()
  {
    return getClassName();
  }

  /**
   * True if the implementation is a proxy, i.e. an interface stub which
   * calls an instance class.
   */
  @Override
  public boolean isProxy()
  {
    return true;
  }

  @Override
  public String getViewClassName()
  {
    return "StatefulProxy";
  }

  @Override
  public String getBeanClassName()
  {
    // XXX: 4.0.7
    // return getViewClass().getJavaClass().getSimpleName() + "__Bean";
    return getBeanType().getJavaClass().getName();
  }
  
  //
  // introspection
  //

  /**
   * Scans for the @Local interfaces
   */
  @Override
  protected AnnotatedType<? super X> introspectLocalDefault() 
  {
    return getBeanType();
  }
  
  //
  // Java code generation
  //

  /**
   * Generates the stateful session bean
   */
  @Override
  public void generate(JavaWriter out) throws IOException
  {
    generateTopComment(out);

    out.println();
    out.println("package " + getPackageName() + ";");

    out.println();
    out.println("import com.caucho.config.*;");
    out.println("import com.caucho.ejb.*;");
    out.println("import com.caucho.ejb.session.*;");
    out.println();
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");

    out.println();
    out.println("public class " + getClassName());
    out.println("  extends StatefulContext");
    out.println("{");
    out.pushDepth();
    
    // XXX: 4.0.7 temp for JSR-299 TCK
    /*
    out.println("static java.lang.reflect.Constructor _ctor;");
    out.println("static {");
    out.println("try {");
    out.print("Class cl = Class.forName(\"");
    out.print(getBeanClass().getJavaClass().getName());
    out.println("\");");
    // out.println("cl.setAccessible(true);");
    out.println("java.lang.reflect.Constructor ctor = null;");
    out.println("for (java.lang.reflect.Constructor tCtor : cl.getDeclaredConstructors()) {");
    out.println("  if (tCtor.getParameterTypes().length == 0)");
    out.println("    ctor = tCtor;");
    out.println("}");
    out.println("_ctor = ctor;");
    out.println("_ctor.setAccessible(true);");
    out.println("} catch (Exception e) {");
    out.println("  e.printStackTrace();");
    out.println("}");
    out.println("}");
    */

    out.println();
    out.println("public " + getClassName() + "(StatefulManager server)");
    out.println("{");
    out.pushDepth();

    out.println("super(server);");

    generateContextHomeConstructor(out);

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public " + getClassName() + "(" + getClassName()
                + " context)");
    out.println("{");
    out.pushDepth();

    out.println("super(context.getStatefulManager());");

    generateContextObjectConstructor(out);

    out.popDepth();
    out.println("}");

    generateContextPrologue(out);

    generateCreateProvider(out);
    generateView(out);

    generateDependency(out);

    out.popDepth();
    out.println("}");
  }

  protected void generateCreateProvider(JavaWriter out) throws IOException
  {
    out.println();
    out.println("@Override");
    out.println("public StatefulProvider getProvider()");
    out.println("{");
    out.pushDepth();

    generateCreateProviderView(out);

    out.popDepth();
    out.println("}");
  }

  @Override
  protected void generateContext(JavaWriter out)
  {
  }

  /**
   * Generates code to create the provider
   */
  public void generateCreateProviderView(JavaWriter out)
    throws IOException
  {
    out.println("  return new " + getViewClassName() + "(getStatefulManager(), true);");
  }

  /**
   * Generates the view code.
   */
  public void generateView(JavaWriter out)
    throws IOException
  {
    // generateBean(out);

    out.println();
    out.println("public static class " + getViewClassName());

    if (hasNoInterfaceView())
      out.println("  extends " + getBeanType().getJavaClass().getName());

    out.print("  implements StatefulProvider");

    for (AnnotatedType<? super X> api : getLocalApi()) {
      out.print(", " + api.getJavaClass().getName());
    }
    out.println();

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
    out.println("  extends " + getBeanType().getJavaClass().getName());
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
      out.println("_bean = (" + getBeanClassName() + ") _server.newInstance();");
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

  protected void generateSuper(JavaWriter out, String serverVar)
    throws IOException
  {
  }

  protected void generateExtends(JavaWriter out)
    throws IOException
  {
  }

  protected AnnotatedMethod<? super X> findImplMethod(AnnotatedMethod<? super X> apiMethod)
  {
    AnnotatedMethod<? super X> implMethod 
      = AnnotatedTypeUtil.findMethod(getBeanType(), apiMethod);

    if (implMethod != null)
      return implMethod;

    Method javaMethod = apiMethod.getJavaMember();
    
    throw new ConfigException(L.l("'{0}' method '{1}' has no corresponding implementation in '{2}'",
                                  javaMethod.getDeclaringClass().getSimpleName(),
                                  javaMethod.getName(),
                                  getBeanType().getJavaClass().getName()));
  }
}
