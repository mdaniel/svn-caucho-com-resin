/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.gen;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Qualifier;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.SerializeHandle;
import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.candi.HandleAware;
import com.caucho.v5.inject.Module;
import com.caucho.v5.java.JavaWriter;
import com.caucho.v5.java.gen.JavaClassGenerator;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.util.L10N;

/**
 * Generates the skeleton for a session bean.
 */
@Module
public class BeanGeneratorCandi<X> extends BeanGenerator<X> {
  private static final L10N L = new L10N(BeanGeneratorCandi.class);

  private AnnotatedType<X> _beanClass;

  private AspectBeanFactory<X> _aspectFactory;
  private AspectBeanFactory<X> _lifecycleAspectFactory;

  private ArrayList<AspectGenerator<X>> _businessMethods
    = new ArrayList<AspectGenerator<X>>();

  private boolean _isEnhanced;
  private boolean _hasReadResolve;
  private boolean _isSingleton;
  private boolean _isSerializeHandle;

  public BeanGeneratorCandi(CandiManager manager,
                            AnnotatedType<X> beanClass)
  {
    super(beanClass.getJavaClass().getName() + "__CauchoCdi", beanClass);

    setSuperClassName(beanClass.getJavaClass().getName());

    if (beanClass.isAnnotationPresent(SerializeHandle.class)) {
      _isSerializeHandle = true;

      addInterfaceName(Serializable.class.getName());
      addInterfaceName(HandleAware.class.getName());
    }
    
    addInterfaceName(CandiEnhancedBean.class.getName());

    addImport("javax.transaction.*");

    _beanClass = beanClass;
    
    _aspectFactory = new AspectBeanFactoryCandi<X>(manager, beanClass);
    _lifecycleAspectFactory = new AspectBeanFactoryLifecycle<X>(_aspectFactory, manager, beanClass);
  }
  
  public void setSingleton(boolean isSingleton)
  {
    _isSingleton = isSingleton;
  }
  
  public ArrayList<AspectGenerator<X>> getBusinessMethods()
  {
    return _businessMethods;
  }
  
  public AspectBeanFactory<X> getLifecycleAspectFactory()
  {
    return _lifecycleAspectFactory;
  }

  @Override
  public void introspect()
  {
    super.introspect();

    introspectClass(_beanClass);
    
    AspectFactory<X> aspectHeadFactory = _aspectFactory.getHeadAspectFactory();
    ArrayList<AnnotatedMethod<?>> unenhancedMethods
      = new ArrayList<AnnotatedMethod<?>>();

    for (AnnotatedMethod<? super X> method : _beanClass.getMethods()) {
      Method javaMethod = method.getJavaMember();
      
      if (Object.class.equals(method.getBaseType()))
        continue;

      if (javaMethod.getName().equals("readResolve")
          && javaMethod.getParameterTypes().length == 0) {
        _hasReadResolve = true;
      }
      
      int modifiers = method.getJavaMember().getModifiers();
      
      if (method.isStatic())
        continue;
      
      if (method.isAnnotationPresent(PostConstruct.class)
          || method.isAnnotationPresent(PreDestroy.class)) {
        AspectGenerator<X> bizMethod = _lifecycleAspectFactory.create(method);

        _businessMethods.add(bizMethod);
        
        continue;
      }
      
      if (Modifier.isPrivate(modifiers))
        continue;

      boolean isEnhance = false;
      AspectGenerator<X> bizMethod = aspectHeadFactory.create(method, isEnhance);

      if (bizMethod == null) {
        unenhancedMethods.add(method);
        continue;
      }
      
      // ioc/0i10
      if (_businessMethods.contains(bizMethod))
        continue;

      /*
      if (! Modifier.isPublic(modifiers) && ! Modifier.isProtected(modifiers))
        throw new ConfigException(L.l("{0}: Java Injection annotations are not allowed on private or package-private methods.", bizMethod));
        */
      if (method.isStatic())
        throw new ConfigException(L.l("{0}: Java Injection annotations are not allowed on static methods.", bizMethod));
      if (Modifier.isFinal(modifiers))
        throw new DeploymentException(L.l("{0}: Java Injection annotations are not allowed on final methods.", bizMethod));

      _isEnhanced = true;

      _businessMethods.add(bizMethod);
    }

    if (Serializable.class.isAssignableFrom(_beanClass.getJavaClass())
        && ! _hasReadResolve
        && hasTransientInject(_beanClass.getJavaClass())) {
      _isEnhanced = true;
    }
    
    if (_aspectFactory.isEnhanced())
      _isEnhanced = true;
    
    if (_isEnhanced) {
      for (AnnotatedMethod method : unenhancedMethods) {
        AspectGenerator bizMethod = aspectHeadFactory.create(method, true);
        
        if (! _businessMethods.contains(bizMethod))
          _businessMethods.add(bizMethod);
      }
    }
  }

  protected void introspectClass(AnnotatedType<X> cl)
  {
  }

  private boolean hasTransientInject(Class<?> cl)
  {
    if (cl == null || Object.class.equals(cl))
      return false;

    for (Field field : cl.getDeclaredFields()) {
      if (! Modifier.isTransient(field.getModifiers()))
        continue;
      if (Modifier.isStatic(field.getModifiers()))
        continue;

      Annotation []annList = field.getDeclaredAnnotations();
      if (annList == null)
        continue;

      for (Annotation ann : annList) {
        if (ann.annotationType().isAnnotationPresent(Qualifier.class))
          return true;

        /*
        if (In.class.equals(ann.annotationType()))
          return true;
        */
      }
    }

    return hasTransientInject(cl.getSuperclass());
  }

  @Override
  public String getViewClassName()
  {
    return getFullClassName();
  }

  /**
   * Returns the introspected methods
   */
  @Override
  public ArrayList<AspectGenerator<X>> getMethods()
  {
    return getBusinessMethods();
  }

  public Class<?> generateClass()
  {
    if (! isEnhanced())
      return _beanClass.getJavaClass();
    
    Class<?> baseClass = _beanClass.getJavaClass();
    int modifiers = baseClass.getModifiers();
    
    ClassLoader baseClassLoader = baseClass.getClassLoader();
    
    boolean isPackageLoader = (baseClassLoader != null
                               && (baseClassLoader instanceof DynamicClassLoader));

    if (Modifier.isFinal(modifiers))
      throw new DeploymentException(L.l("'{0}' is an invalid enhanced class because it is final.",
                                        baseClass.getName()));

    try {
      JavaClassGenerator gen = new JavaClassGenerator();

      Class<?> cl;
      
      if (isPackageLoader)
        cl = gen.preloadClassParentLoader(getFullClassName(), baseClass);
      else
        cl = gen.preload(getFullClassName());

      if (cl == null) {
        gen.generate(this);

        gen.compilePendingJava();
      
        // ioc/0c26

        if (isPackageLoader)
          cl = gen.loadClassParentLoader(getFullClassName(), baseClass);
        else
          cl = gen.loadClass(getFullClassName());
      }

      Method getException = cl.getMethod("__caucho_getException");
      
      RuntimeException exn = (RuntimeException) getException.invoke(null);
      
      if (exn != null)
        throw exn;
      
      return cl;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected boolean isEnhanced()
  {
    return _isEnhanced;
  }

  @Override
  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    generateHeader(out);

    /*
    HashMap map = new HashMap();
    for (BusinessMethodGenerator method : _businessMethods) {
      method.generatePrologueTop(out, map);
    }
    */
    
    boolean isCtor = false;

    for (Constructor<?> ctor
           : _beanClass.getJavaClass().getDeclaredConstructors()) {
      if (Modifier.isPublic(ctor.getModifiers())
          || Modifier.isProtected(ctor.getModifiers())) {
        generateConstructor(out, ctor);
        isCtor = true;
      }
    }
    
    // ioc/0c1d
    if (! isCtor) {
      for (Constructor<?> ctor
             : _beanClass.getJavaClass().getDeclaredConstructors()) {
        if (! Modifier.isPrivate(ctor.getModifiers())) {
          generateConstructor(out, ctor);
        }
      }
    }

    HashMap<String,Object> map = new HashMap<String,Object>();

    generateCauchoProxyInit(out, map);

    generateBeanSetter(out);
    
    generateBeanPrologue(out, map);

    generateBusinessMethods(out, map);

    generateEpilogue(out, map);
    
    generateInject(out, map);
    
    generateDelegate(out, map);
    
    generatePostConstruct(out, map);
    
    generateWriteReplace(out);

    generateDestroy(out, map);
  }

  /**
   * Generates header and prologue data.
   */
  protected void generateHeader(JavaWriter out)
    throws IOException
  {
    generateClassStaticFields(out);
    
    out.println("private static final boolean __caucho_isFiner");
    out.println("  = __caucho_log.isLoggable(java.util.logging.Level.FINER);");
    
    out.println("private " + getBeanClassName() + " _bean;");

    if (_isSerializeHandle) {
      generateSerializeHandle(out);
    }
    else {
      generateSerialize(out);
    }

    /*
    if (_isReadResolveEnhanced)
      generateReadResolve(out);
    */
  }

  protected void generateSerializeHandle(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("private transient Object _serializationHandle;");
    out.println();
    out.println("public void setSerializationHandle(Object handle)");
    out.println("{");
    out.println("  _serializationHandle = handle;");
    out.println("}");
    
    generateSerialize(out, "_serializationHandle");
  }

  protected void generateSerialize(JavaWriter out)
      throws IOException
  {
    generateSerialize(out, "_bean");
    
  }
  protected void generateSerialize(JavaWriter out, String var)
      throws IOException
    {
    if (! findWriteReplace(_beanClass.getJavaClass())) {
      out.println();
      out.println("private Object writeReplace()");
      out.println("{");
      out.println("  return " + var + ";");
      out.println("}");
    }
  }

  protected void generateReadResolve(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("private Object readResolve()");
    out.println("{");
    out.println("  return this;");
    out.println("}");
  }

  protected void generateWriteReplace(JavaWriter out)
    throws IOException
  {
    if (_isSingleton && ! findWriteReplace(_beanClass.getJavaClass())) {
      out.println("private transient Object __caucho_handle;");
      out.println();

      out.println("private Object writeReplace()");
      out.println("{");
      out.println("  return __caucho_handle;");
      out.println("}");
    }
    else {
      // XXX: need a handle or serialize to the base class (?)
    }
  }
  
  protected boolean findWriteReplace(Class<?> cl)
  {
    if (cl == null || Object.class.equals(cl)) {
      return false;
    }
    
    for (Method method : cl.getDeclaredMethods()) {
      if (method.getName().equals("writeReplace")
          && method.getParameterTypes().length == 0) {
        return true;
      }
    }
    
    return findWriteReplace(cl.getSuperclass());
  }

  protected void generateConstructor(JavaWriter out, Constructor<?> ctor)
    throws IOException
  {
    Class<?> []paramTypes = ctor.getParameterTypes();

    out.print("public " + getClassName() + "(");

    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
        out.print(", ");

      out.printClass(paramTypes[i]);
      out.print(" a" + i);
    }

    out.println(")");

    generateThrows(out, ctor.getExceptionTypes());

    out.println("{");
    out.pushDepth();

    out.print("super(");

    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
        out.print(", ");

      out.print("a" + i);
    }
    out.println(");");
    
    // ioc/0c5b
    /*out.print("_bean = new " + getBeanClassName() + "(");
    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
        out.print(", ");

      out.print("a" + i);
    }
    out.println(");");*/
    
    out.println();
    out.println("if (__caucho_exception != null)");
    out.println("  throw __caucho_exception;");

    generateBeanConstructor(out);
    generateProxyConstructor(out);

    out.popDepth();
    out.println("}");
  }

  protected void generateThrows(JavaWriter out, Class<?> []exnCls)
    throws IOException
  {
    if (exnCls.length == 0)
      return;

    out.print(" throws ");

    for (int i = 0; i < exnCls.length; i++) {
      if (i != 0)
        out.print(", ");

      out.printClass(exnCls[i]);
    }
  }

  private void generateBeanSetter(JavaWriter out) throws IOException
  {
    out.println("@Override");
    out.println("public void __caucho_setBean(Object bean)");
    out.println("{");
    out.pushDepth();
    out.print("_bean = (");
    out.print(getBeanClassName());
    out.println(")bean;");
    out.popDepth();
    out.println("}");
   }

  private void generateCauchoProxyInit(JavaWriter out,
                                       HashMap<String,Object> map)
                                           throws IOException
  {
    out.println();
    out.println("@Override");
    out.println("public void __caucho_init()");
    out.println("{");
    out.pushDepth();

    generateCauchoDefaultInit(out, map);

    out.popDepth();
    out.println("}");
  }

  @Override
  protected AspectBeanFactory<X> getAspectBeanFactory()
  {
    return _aspectFactory;
  }
}
