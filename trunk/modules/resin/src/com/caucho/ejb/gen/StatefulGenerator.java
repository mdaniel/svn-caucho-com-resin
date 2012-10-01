/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.SessionBean;
import javax.ejb.Stateful;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.AspectBeanFactory;
import com.caucho.config.gen.CandiUtil;
import com.caucho.config.gen.LifecycleAspectBeanFactory;
import com.caucho.config.gen.XaCallback;
import com.caucho.config.inject.InjectManager;
import com.caucho.ejb.session.StatefulHandle;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Generates the skeleton for a stateful bean.
 */
@Module
public class StatefulGenerator<X> extends SessionGenerator<X> {
  private final AspectBeanFactory<X> _aspectBeanFactory;
  private final AspectBeanFactory<X> _lifecycleAspectFactory;

  public StatefulGenerator(String ejbName,
                           AnnotatedType<X> beanType,
                           ArrayList<AnnotatedType<? super X>> localApi, 
                           AnnotatedType<X> localBean,
                           ArrayList<AnnotatedType<? super X>> remoteApi)
  {
    super(ejbName, beanType, localApi, localBean, remoteApi, 
          Stateful.class.getSimpleName());

    InjectManager manager = InjectManager.create();

    _aspectBeanFactory
      = new StatefulAspectBeanFactory<X>(manager, getBeanType());
    _lifecycleAspectFactory
      = new LifecycleAspectBeanFactory<X>(_aspectBeanFactory, manager, getBeanType());
  }

  @Override
  protected AspectBeanFactory<X> getAspectBeanFactory()
  {
    return _aspectBeanFactory;
  }

  @Override
  protected AspectBeanFactory<X> getLifecycleAspectFactory()
  {
    return _lifecycleAspectFactory;
  }

  @Override
  public boolean isStateless()
  {
    return false;
  }

  @Override
  protected boolean isTimerSupported()
  {
    return false;
  }

  public String getContextClassName()
  {
    return getClassName();
  }

  /**
   * True if the implementation is a proxy, i.e. an interface stub which calls
   * an instance class.
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
    out.println("import com.caucho.config.inject.CreationalContextImpl;");
    out.println("import com.caucho.ejb.*;");
    out.println("import com.caucho.ejb.session.*;");
    out.println();
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");

    generateClassHeader(out);

    out.println("{");
    out.pushDepth();

    generateClassStaticFields(out);

    generateClassContent(out);

    generateDependency(out);

    out.popDepth();
    out.println("}");
  }

  private void generateClassHeader(JavaWriter out) throws IOException
  {
    out.println();
    out.println("public class " + getClassName() + "<T>");

    if (hasNoInterfaceView())
      out.println("  extends " + getBeanType().getJavaClass().getName());

    out.print("  implements SessionProxyFactory<T>");
    out.print(",\n  com.caucho.ejb.session.StatefulProxy");
    out.print(",\n  com.caucho.config.gen.CandiEnhancedBean");
    out.print(",\n  java.io.Serializable");

    for (AnnotatedType<? super X> api : getLocalApi()) {
      out.print(",\n  ");
      out.printType(api.getBaseType());
    }

    for (AnnotatedType<? super X> apiType : getRemoteApi()) {
      out.print(",\n  ");
      out.printType(apiType.getBaseType());
    }

    out.println();
  }

  @Override
  protected void generateClassContent(JavaWriter out) throws IOException
  {
    out.println("private String _id;");
    out.println("private transient long _lastAccessTime;");
    out.println("private transient StatefulManager _manager;");
    out.println("private transient StatefulContext _context;");

    out.println("private " + getBeanClassName() + " _bean;");

    HashMap<String, Object> map = new HashMap<String, Object>();

    generateContentImpl(out, map);

    generateSerialization(out);
  }

  @Override
  protected void generateContentImpl(JavaWriter out, 
                                     HashMap<String, Object> map)
      throws IOException
  {

    generateConstructor(out, map);

    generateProxyFactory(out);

    generateBeanPrologue(out, map);

    generateBusinessMethods(out, map);
    
    generateXa(out, map);

    generateEpilogue(out, map);
    generateInject(out, map);
    generateDelegate(out, map);
    generatePostConstruct(out, map);
    generateValidate(out, map);
    generateDestroy(out, map);
  }

  private void generateConstructor(JavaWriter out, HashMap<String, Object> map)
      throws IOException
  {
    // generateProxyConstructor(out);

    // proxy factory constructor
    out.println();
    out.print("public " + getClassName() + "(StatefulManager manager, ");
    out.println("StatefulContext context)");
    out.println("{");
    out.pushDepth();

    out.println("_manager = manager;");
    out.println("_context = context;");

    out.println("if (__caucho_exception != null)");
    out.println("  throw __caucho_exception;");

    out.popDepth();
    out.println("}");
    
    // proxy constructor

    out.println();
    out.println("private " + getClassName() + "(StatefulManager manager"
        + ", StatefulContext context" + ", CreationalContextImpl<T> env)");
    out.println("{");
    out.pushDepth();

    out.println("_id = manager.generateKey();");
    out.println("_lastAccessTime = com.caucho.util.CurrentTime.getCurrentTime();");
    out.println("_manager = manager;");
    out.println("_context = context;");

    out.println("_bean = (" + getBeanClassName()
        + ") _manager.newInstance(env);");

    // ejb/5011
    if (SessionBean.class.isAssignableFrom(getBeanType().getJavaClass())) {
      out.println("_bean.setSessionContext(context);");
    }

    generateContextObjectConstructor(out);

    out.popDepth();
    out.println("}");
    
    out.println();
    out.println("@Override");
    out.println("public String __caucho_getId()");
    out.println("{");
    out.println("  return _id;");
    out.println("}");
  }

  private void generateProxyFactory(JavaWriter out) throws IOException
  {
    out.println();
    out.println("@Override");
    out.println("public T __caucho_createProxy(CreationalContextImpl<T> env)");
    out.println("{");
    out.println("  return (T) new " + getClassName()
                + "(_manager, _context, env);");
    out.println("}");
  }

  public void generateValidate(JavaWriter out, HashMap<String, Object> map)
      throws IOException
  {
    out.println();
    out.println("public void __caucho_validate()");
    out.println("{");
    out.pushDepth();
    
    out.println("long now = com.caucho.util.CurrentTime.getCurrentTime();");
    
    out.println("if (_manager.getIdleTimeout() < now - _lastAccessTime) {");
    out.println("  __caucho_destroy(null);");
    out.println("}");
    out.println();
    out.println("if (_bean == null)");
    out.println("  throw new javax.ejb.NoSuchEJBException(\"Stateful instance "
                + getClassName() + " is no longer valid\");");
    
    out.println();
    out.println("_lastAccessTime = now;");

    out.popDepth();
    out.println("}");
  }

  @Override
  public void generateDestroy(JavaWriter out, HashMap<String, Object> map)
      throws IOException
  {
    super.generateDestroy(out, map);

    out.println();
    out.println("@Override");
    out.println("public void __caucho_destroy()");
    out.println("{");
    out.pushDepth();
    
    out.println("try {");
    out.println("  __caucho_preDestroyImpl();");
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    
    out.popDepth();
    out.println("}");
  }

  @Override
  protected void generateDestroyImpl(JavaWriter out) throws IOException
  {
    super.generateDestroyImpl(out);

    out.println("_manager.destroy(_bean, env);");
    out.println("_bean = null;");
  }

  public void generateXa(JavaWriter out, HashMap<String, Object> map)
      throws IOException
  {
    AnnotatedType<X> beanType = getBeanType();
    
    if (! beanType.isAnnotationPresent(XaCallback.class)) {
      return;
    }
    
    generateXaCallbackReflection(out);
    
    out.println("class __caucho_synchronization");
    out.println("  implements javax.ejb.SessionSynchronization {");
    out.pushDepth();
    
    out.println("Object _syncBean = _bean;");
    
    out.println("@Override");
    out.println("public void afterBegin()");
    out.println("  throws javax.ejb.EJBException, java.rmi.RemoteException");
    out.println("{");
    out.pushDepth();
    
    generateXaCallbackMethods(out, AfterBegin.class);
    
    out.popDepth();
    out.println("}");
    
    out.println("@Override");
    out.println("public void beforeCompletion()");
    out.println("  throws javax.ejb.EJBException, java.rmi.RemoteException");
    out.println("{");
    out.pushDepth();
    
    generateXaCallbackMethods(out, BeforeCompletion.class);
    
    out.popDepth();
    out.println("}");
    
    out.println("@Override");
    out.println("public void afterCompletion(boolean isCommitted)");
    out.println("  throws javax.ejb.EJBException, java.rmi.RemoteException");
    out.println("{");
    out.pushDepth();
    
    generateXaAfterCompletion(out, AfterCompletion.class);

    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }
  
  private void generateXaCallbackMethods(JavaWriter out,
                                         Class<? extends Annotation> annType)
    throws IOException
  {
    for (AnnotatedMethod<?> m : getBeanType().getMethods()) {
      if (! m.isAnnotationPresent(annType))
        continue;
      
      Method javaMethod = m.getJavaMember();
      Class<?> declClass = javaMethod.getDeclaringClass();
      
      out.println("try {");
      out.pushDepth();
      
      String name = ("__caucho_xa_" +  declClass.getSimpleName()
                     + "_" + javaMethod.getName());
      
      out.println(name + ".invoke(_syncBean);");

      out.popDepth();
      out.println("} catch (RuntimeException e) {");
      out.println("  throw e;");
      out.println("} catch (java.lang.reflect.InvocationTargetException e) {");
      out.println("  if (e.getCause() instanceof RuntimeException)");
      out.println("    throw (RuntimeException) e.getCause();");
      out.println("  else");
      out.println("    throw new javax.ejb.EJBException(e);");
      out.println("} catch (Exception e) {");
      out.println("  throw new javax.ejb.EJBException(e);");
      out.println("}");
    }
  }
  
  private void generateXaCallbackReflection(JavaWriter out)
    throws IOException
  {
    for (AnnotatedMethod<?> m : getBeanType().getMethods()) {
      if (! m.isAnnotationPresent(AfterBegin.class)
          && ! m.isAnnotationPresent(BeforeCompletion.class)
          && ! m.isAnnotationPresent(AfterCompletion.class)) {
        continue;
      }
      
      Method javaMethod = m.getJavaMember();
      Class<?> declClass = javaMethod.getDeclaringClass();
      
      String name = ("__caucho_xa_" +  declClass.getSimpleName()
                     + "_" + javaMethod.getName());
   
      out.print("static final java.lang.reflect.Method");
      out.println("  " + name);
      out.print("  = " + CandiUtil.class.getName() + ".findAccessibleMethod(");
      out.print(declClass.getName() + ".class");
      out.print(", \"" + javaMethod.getName() + "\"");
      
      for (Class<?> param : javaMethod.getParameterTypes()) {
        out.print(", ");
        out.printClass(param);
        out.print(".class");
      }
      
      out.println(");");
    }
  }
  
  private void generateXaAfterCompletion(JavaWriter out,
                                         Class<? extends Annotation> annType)
    throws IOException
  {
    for (AnnotatedMethod<?> m : getBeanType().getMethods()) {
      if (! m.isAnnotationPresent(annType))
        continue;
      
      
      Method javaMethod = m.getJavaMember();
      Class<?> declClass = javaMethod.getDeclaringClass();
      
      out.println("try {");
      out.pushDepth();
      
      String name = ("__caucho_xa_" +  declClass.getSimpleName()
                     + "_" + javaMethod.getName());
      
      out.println(name + ".invoke(_syncBean, isCommitted);");
      
      out.popDepth();
      out.println("} catch (RuntimeException e) {");
      out.println("  throw e;");
      out.println("} catch (java.lang.reflect.InvocationTargetException e) {");
      out.println("  if (e.getCause() instanceof RuntimeException)");
      out.println("    throw (RuntimeException) e.getCause();");
      out.println("  else");
      out.println("    throw new javax.ejb.EJBException(e);");
      out.println("} catch (Exception e) {");
      out.println("  throw new javax.ejb.EJBException(e);");
      out.println("}");
    }
  }

  private void generateSerialization(JavaWriter out) throws IOException
  {
    out.println("private Object writeReplace()");
    out.println("{");
    out.pushDepth();

    out.print("return new ");
    out.printClass(StatefulHandle.class);
    out.println("(_manager.getEJBName(), null);");

    out.popDepth();
    out.println("}");
  }
}
