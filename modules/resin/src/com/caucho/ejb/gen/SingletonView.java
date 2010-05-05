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
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents a public interface to a singleton bean, e.g. a singleton view
 */
@Module
public class SingletonView<X> extends SessionView<X> {
  private static final L10N L = new L10N(SessionView.class);

  private SingletonGenerator<X> _sessionBean;
  
  private SingletonAspectBeanFactory<X> _aspectBeanFactory;

  public SingletonView(SingletonGenerator<X> bean)
  {
    super(bean);

    _sessionBean = bean;
    
    InjectManager manager = InjectManager.create();
    
    _aspectBeanFactory = new SingletonAspectBeanFactory<X>(manager, bean.getBeanType());
  }

  public SingletonGenerator<X> getSessionBean()
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
  public boolean isProxy()
  {
    return true;
  }

  @Override
  public String getViewClassName()
  {
    return "SingletonView";
  }

  @Override
  public String getBeanClassName()
  {
    return "Bean";
  }

  /**
   * Introspects the APIs methods, producing a business method for
   * each.
   */
  @Override
  public void addBusinessMethod(AnnotatedMethod<? super X> method)
  {
    AspectGenerator<X> bizMethod
    = _aspectBeanFactory.create(method);

    if (bizMethod != null) {
      _businessMethods.add(bizMethod);
    }
  }

  /**
   * Generates code to create the provider
   */
  public void generateCreateProvider(JavaWriter out)
    throws IOException
  {
    out.println("return new " + getViewClassName() + "(getServer());");
  }

  /**
   * Generates the view code.
   */
  @Override
  public void generate(JavaWriter out)
    throws IOException
  {
    generateBean(out);

    out.println();
    out.println("public static class " + getViewClassName());

    if (isNoInterfaceView())
      out.println("  extends " + getBeanType().getJavaClass().getName());
    
    out.print("  implements SingletonProxyFactory");

    for (AnnotatedType<? super X> apiType : getGenerator().getLocalApi()) {
      out.print(", " + apiType.getJavaClass().getName());
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
    out.println("private transient SingletonContext _context;");
    out.println("private transient SingletonManager _manager;");

    /*
    if (isProxy()) {
      out.println("private " + getBeanClassName() + " _bean;");
    }
    */
    String beanClassName = getBeanType().getJavaClass().getName();
    
    out.println("private " + beanClassName + " _bean;");

    out.println("private transient boolean _isValid;");
    out.println("private transient boolean _isActive;");

    /*
    out.println();
    out.println("private static final com.caucho.ejb.gen.XAManager _xa");
    out.println("  = new com.caucho.ejb.gen.XAManager();");
*/
    //generateBusinessPrologue(out);

    out.println();
    out.println(getViewClassName() + "(SingletonManager manager)");
    out.println("{");
    out.pushDepth();

    generateSuper(out, "manager");

    out.println("_manager = manager;");
    out.println("_isValid = true;");

    /*
    // ejb/1143
    if (isProxy()) {
      out.println("_bean = (" + beanClassName + ") manager.newInstance();");
    }
    */
    // out.println("_bean = (" + beanClassName + ") manager.newInstance();");

    out.popDepth();
    out.println("}");

    /*
    out.println();
    out.println(getViewClassName()
                + "(SingletonManager manager, boolean isProxyFactory)");
    out.println("{");
    out.pushDepth();

    generateSuper(out, "manager");

    out.println("_manager = manager;");
    out.println("_isValid = true;");

    out.popDepth();
    out.println("}");
    */

    generateSessionProvider(out);

    out.println();
    out.println("void __caucho_setContext(SingletonContext context)");
    out.println("{");
    out.println("  _context = context;");
    out.println("}");

    out.println();
    out.println("public void __caucho_postConstruct()");
    out.println("{");
    out.println("  _bean = (" + beanClassName + ") _manager.newInstance();");
    out.println("}");

    generateBusinessMethods(out);
  }

  protected void generateSessionProvider(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public Object __caucho_createNew(javax.enterprise.inject.spi.InjectionTarget injectBean, javax.enterprise.context.spi.CreationalContext env)");
    out.println("{");

    /*
    out.println("  " + getViewClassName() + " bean"
                + " = new " + getViewClassName() + "(_manager);");

    if (isProxy())
      out.println("  _manager.initInstance(bean._bean, injectBean, bean, env);");
    else
      out.println("  _manager.initInstance(bean, injectBean, bean, env);");
    out.println("  return bean;");
    */
    
    out.println("  return this;");

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
    AnnotatedMethod<? super X> implMethod = getMethod(apiMethod);

    if (implMethod != null)
      return implMethod;
    
    Method javaApiMethod = apiMethod.getJavaMember();

    throw new ConfigException(L.l("'{0}' method '{1}' has no corresponding implementation in '{2}'",
                                  javaApiMethod.getDeclaringClass().getSimpleName(),
                                  javaApiMethod.getName(),
                                  getBeanType().getJavaClass().getName()));
  }
}
