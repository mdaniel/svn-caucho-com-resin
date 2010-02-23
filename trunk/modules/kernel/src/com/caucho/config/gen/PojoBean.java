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

package com.caucho.config.gen;

import com.caucho.config.ConfigException;
import com.caucho.config.SerializeHandle;
import com.caucho.config.inject.HandleAware;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.util.L10N;
import java.io.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;
import javax.decorator.Decorator;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Qualifier;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;

/**
 * Generates the skeleton for a session bean.
 */
public class PojoBean extends BeanGenerator {
  private static final L10N L = new L10N(PojoBean.class);

  private ApiClass _beanClass;

  private PojoView _view;

  private ArrayList<BusinessMethodGenerator> _businessMethods
    = new ArrayList<BusinessMethodGenerator>();

  private boolean _isEnhanced;
  private boolean _hasReadResolve;
  private boolean _isSingleton;
  private boolean _isSerializeHandle;

  public PojoBean(ApiClass beanClass)
  {
    super(beanClass.getName() + "__ResinWebBean", beanClass);

    setSuperClassName(beanClass.getName());

    if (beanClass.isAnnotationPresent(SerializeHandle.class)) {
      _isSerializeHandle = true;

      addInterfaceName(Serializable.class.getName());
      addInterfaceName(HandleAware.class.getName());
    }
    
    addInterfaceName(BeanInjectionTarget.class.getName());

    addImport("javax.transaction.*");

    _view = new PojoView(this, getBeanClass());

    _beanClass = beanClass;
  }

  public void setSingleton(boolean isSingleton)
  {
    _isSingleton = isSingleton;
  }
  
  public ArrayList<BusinessMethodGenerator> getBusinessMethods()
  {
    return _businessMethods;
  }

  @Override
  public void introspect()
  {
    super.introspect();

    introspectClass(_beanClass);

    for (ApiMethod method : _beanClass.getMethods()) {
      if (Object.class.equals(method.getDeclaringClass()))
        continue;

      if (method.getName().equals("readResolve")
          && method.getParameterTypes().length == 0) {
        _hasReadResolve = true;
      }

      int index = _businessMethods.size();
      BusinessMethodGenerator bizMethod
        = new BusinessMethodGenerator(_view, method, method, index);

      // ioc/0i10
      if (_businessMethods.contains(bizMethod))
        continue;

      bizMethod.introspect(method, method);

      if (! bizMethod.isEnhanced())
        continue;

      if (! method.isPublic() && ! method.isProtected())
        throw new ConfigException(L.l("{0}: Java Injection annotations are not allowed on private methods.", bizMethod));
      if (method.isStatic())
        throw new ConfigException(L.l("{0}: Java Injection annotations are not allowed on static methods.", bizMethod));
      if (method.isFinal())
        throw new ConfigException(L.l("{0}: Java Injection annotations are not allowed on final methods.", bizMethod));

      _isEnhanced = true;

      _businessMethods.add(bizMethod);
    }

    if (Serializable.class.isAssignableFrom(_beanClass.getJavaClass())
        && ! _hasReadResolve
        && hasTransientInject(_beanClass.getJavaClass())) {
      _isEnhanced = true;
    }

    if (getDecoratorTypes().size() > 0)
      _isEnhanced = true;
  }

  protected void introspectClass(ApiClass cl)
  {
    if (cl.isAnnotationPresent(Interceptor.class)
        || cl.isAnnotationPresent(Decorator.class)) {
      return;
    }

    ArrayList<Annotation> interceptorBindingList
      = new ArrayList<Annotation>();

    Set<Annotation> xmlInterceptorBindings = getInterceptorBindings();

    if (xmlInterceptorBindings != null) {
      for (Annotation ann : xmlInterceptorBindings) {
        interceptorBindingList.add(ann);
      }
    }
    else {
      for (Annotation ann : cl.getAnnotations()) {
        Class<?> annType = ann.annotationType();

        if (annType.isAnnotationPresent(Stereotype.class)) {
          for (Annotation sAnn : ann.annotationType().getAnnotations()) {
            Class<?> sAnnType = sAnn.annotationType();

            if (sAnnType.isAnnotationPresent(InterceptorBinding.class)) {
              interceptorBindingList.add(sAnn);
            }
          }
        }

        if (annType.isAnnotationPresent(InterceptorBinding.class)) {
          interceptorBindingList.add(ann);
        }
      }
    }

    /*
    if (interceptorBindingList.size() > 0) {
      _view.setInterceptorBindings(interceptorBindingList);
    }
    */
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

  public Class<?> generateClass()
  {
    if (! isEnhanced())
      return _beanClass.getJavaClass();

    try {
      JavaClassGenerator gen = new JavaClassGenerator();

      Class<?> cl = gen.preload(getFullClassName());

      if (cl != null)
        return cl;

      gen.generate(this);

      gen.compilePendingJava();

      return gen.loadClass(getFullClassName());
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

    for (Constructor<?> ctor
           : _beanClass.getJavaClass().getDeclaredConstructors()) {
      if (Modifier.isPublic(ctor.getModifiers()))
        generateConstructor(out, ctor);
    }
    
    _view.generateBeanPrologue(out);

    generatePostConstruct(out);

    HashMap<String,Object> map = new HashMap<String,Object>();
    for (BusinessMethodGenerator method : _businessMethods) {
      method.generate(out, map);
    }

    generateWriteReplace(out);
  }

  /**
   * Generates header and prologue data.
   */
  protected void generateHeader(JavaWriter out)
    throws IOException
  {
    out.println("private static final java.util.logging.Logger __log");
    out.println("  = java.util.logging.Logger.getLogger(\"" + getFullClassName() + "\");");
    out.println("private static final boolean __isFiner");
    out.println("  = __log.isLoggable(java.util.logging.Level.FINER);");

    /*
    if (_hasXA) {
      out.println();
      out.println("private static final com.caucho.ejb.gen.XAManager _xa");
      out.println("  = new com.caucho.ejb.gen.XAManager();");
    }
    */

    if (_isSerializeHandle) {
      generateSerializeHandle(out);
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
    out.println();
    out.println("private Object writeReplace()");
    out.println("{");
    out.println("  return _serializationHandle;");
    out.println("}");
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

  protected void generatePostConstruct(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_postConstruct()");
    out.println("{");
    out.pushDepth();

    HashMap<String,Object> map = new HashMap<String,Object>();
    for (BusinessMethodGenerator method : _businessMethods) {
      method.generatePostConstruct(out, map);
    }

    out.popDepth();
    out.println("}");
  }

  protected void generateWriteReplace(JavaWriter out)
    throws IOException
  {
    if (_isSingleton) {
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

  protected void generateConstructor(JavaWriter out, Constructor ctor)
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

    HashMap<String,Object> map = new HashMap<String,Object>();
    for (BusinessMethodGenerator method : _businessMethods) {
      method.generateBeanConstructor(out, map);
    }

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
}
