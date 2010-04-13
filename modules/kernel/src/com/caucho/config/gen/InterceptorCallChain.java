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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.Interceptors;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents the interception
 */
@Module
public class InterceptorCallChain
  extends AbstractCallChain
{
  private static final L10N L = new L10N(InterceptorCallChain.class);

  private static final Annotation[] NULL_ANN_LIST = new Annotation[0];

  private View _view;
  private BusinessMethodGenerator _bizMethod;

  private String _uniqueName;
  private String _chainName;
  private ApiMethod _implMethod;

  private ArrayList<Interceptor<?>> _defaultInterceptors
    = new ArrayList<Interceptor<?>>();

  private ArrayList<Class<?>> _classInterceptors
    = new ArrayList<Class<?>>();

  private boolean _isExcludeDefaultInterceptors;
  private boolean _isExcludeClassInterceptors;

  private InterceptionType _interceptionType = InterceptionType.AROUND_INVOKE;

  private ArrayList<Annotation> _interceptorBinding
    = new ArrayList<Annotation>();

  private ArrayList<Class<?>> _interceptors
    = new ArrayList<Class<?>>();

  private ArrayList<Class<?>> _methodInterceptors
    = new ArrayList<Class<?>>();

  private InterceptionBinding _bindingEntry;

  // map from the interceptor class to the local variable for the interceptor
  private HashMap<Interceptor<?>, String> _interceptorVarMap
    = new HashMap<Interceptor<?>, String>();

  // interceptors we're responsible for initializing
  private ArrayList<Class<?>> _ownInterceptors = new ArrayList<Class<?>>();

  // decorators
  private HashSet<Class<?>> _decoratorSet;

  private final String _decoratorClass = "__caucho_decorator_class";
  private final String _decoratorBeansVar = "__caucho_decorator_beans";
  private final String _decoratorIndexVar = "__caucho_delegates";
  private final String _decoratorLocalVar = _decoratorClass + "_tl";
  private final String _delegateVar = "__caucho_delegate";
  private String _decoratorSetName;
  
  public InterceptorCallChain(EjbCallChain next,
                              BusinessMethodGenerator bizMethod,
                              View view)
  {
    super(next);

    _bizMethod = bizMethod;
    _view = view;
  }

  /**
   * Returns true if the method is an interceptor
   */
  public boolean isEnhanced()
  {
    if (_view.getViewClass().isAnnotationPresent(Interceptor.class)
        || _view.getViewClass().isAnnotationPresent(Decorator.class)) {
      return false;
    }
    else {
      return (_defaultInterceptors.size() > 0
              /*
              || (! _isExcludeDefaultInterceptors
                  && _view.getBean().getDefaultInterceptors().size() > 0)
              */
              || _classInterceptors.size() > 0
              || _interceptorBinding.size() > 0
              || _interceptors.size() > 0
              || _methodInterceptors.size() > 0
              || _decoratorSet != null
              || getAroundInvokeMethod() != null);
    }
  }

  public ArrayList<Class<?>> getInterceptors()
  {
    return _interceptors;
  }

  public ApiMethod getAroundInvokeMethod()
  {
    return _view.getAroundInvokeMethod();
  }
  
  //
  // introspection
  //

  /**
   * Introspects the @Interceptors annotation on the method and the class.
   */
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
    if (implMethod == null)
      return;

    if (! isValidMethod(implMethod)) {
      return;
    }

    ApiClass apiClass = apiMethod.getDeclaringClass();

    ApiClass implClass = implMethod.getDeclaringClass();

    _implMethod = implMethod;

    // interceptors aren't intercepted
    if (_view.getBeanClass().isAnnotationPresent(Interceptor.class)
        || _view.getBeanClass().isAnnotationPresent(Decorator.class)) {
      return;
    }
    
    if (implMethod.isAnnotationPresent(Inject.class)
        || implMethod.isAnnotationPresent(PostConstruct.class)) {
      // ioc/0a23, ioc/0c57
      return;
    }

    introspectInterceptors(apiClass, implClass, apiMethod, implMethod);
    
    introspectDecorators(apiMethod);
   
    introspectDefaults();
  }
  
  private void introspectInterceptors(ApiClass apiClass,
                                      ApiClass implClass,
                                      ApiMethod apiMethod,
                                      ApiMethod implMethod)
  {
    if (apiMethod.isAnnotationPresent(ExcludeClassInterceptors.class))
      _isExcludeClassInterceptors = true;

    if (implMethod.isAnnotationPresent(ExcludeClassInterceptors.class))
      _isExcludeClassInterceptors = true;

    if (apiMethod.isAnnotationPresent(ExcludeDefaultInterceptors.class))
      _isExcludeDefaultInterceptors = true;

    if (implMethod.isAnnotationPresent(ExcludeDefaultInterceptors.class))
      _isExcludeDefaultInterceptors = true;

    Interceptors iAnn;

    if (! _isExcludeClassInterceptors) {
      iAnn = apiClass.getAnnotation(Interceptors.class);

      if (iAnn != null) {
        for (Class<?> iClass : iAnn.value()) {
          if (! _classInterceptors.contains(iClass))
            _classInterceptors.add(iClass);
        }
      }

      if (implClass != null) {
        iAnn = implClass.getAnnotation(Interceptors.class);

        if (apiMethod != implMethod && iAnn != null) {
          for (Class<?> iClass : iAnn.value()) {
            if (! _classInterceptors.contains(iClass))
              _classInterceptors.add(iClass);
          }
        }
      }
    }

    iAnn = apiMethod.getAnnotation(Interceptors.class);

    if (iAnn != null) {
      for (Class<?> iClass : iAnn.value()) {
        if (! _methodInterceptors.contains(iClass))
          _methodInterceptors.add(iClass);
      }
    }

    iAnn = implMethod.getAnnotation(Interceptors.class);

    if (apiMethod != implMethod && iAnn != null) {
      for (Class<?> iClass : iAnn.value()) {
        if (! _methodInterceptors.contains(iClass))
          _methodInterceptors.add(iClass);
      }
    }

    HashMap<Class<?>, Annotation> interceptorTypes
      = new HashMap<Class<?>, Annotation>();

    addInterceptorBindings(interceptorTypes, apiClass.getAnnotations());
    if (implClass != apiClass)
      addInterceptorBindings(interceptorTypes, implClass.getAnnotations());

    addInterceptorBindings(interceptorTypes, apiMethod.getAnnotations());
    addInterceptorBindings(interceptorTypes, implMethod.getAnnotations());

    if (interceptorTypes.size() > 0) {
      _interceptionType = InterceptionType.AROUND_INVOKE;
      _interceptorBinding.addAll(interceptorTypes.values());
    }
  }

  private void introspectDecorators(ApiMethod apiMethod)
  {
    if (apiMethod.getMethod().getDeclaringClass().equals(Object.class))
      return;
    
    ArrayList<Type> decorators = _view.getBean().getDecoratorTypes();
    
    HashSet<Class<?>> decoratorSet = new HashSet<Class<?>>();

    for (Type decorator : decorators) {
      Class<?> decoratorClass = (Class<?>) decorator;

      for (Method method : decoratorClass.getMethods()) {
        if (isMatch(method, apiMethod.getMethod()))
          decoratorSet.add(decoratorClass); 
      }
    }
    
    if (decoratorSet.size() > 0)
      _decoratorSet = decoratorSet;
  }

  public void introspectDefaults()
  {
    // XXX: this code should be a pre-generation
    // ejb/0fb6
    if (! _isExcludeClassInterceptors && _interceptors.size() == 0) {
      for (Class<?> iClass : _classInterceptors) {
        if (_interceptors.indexOf(iClass) < 0)
          _interceptors.add(iClass);
      }
    }

    for (Class<?> iClass : _methodInterceptors) {
      if (_interceptors.indexOf(iClass) < 0)
        _interceptors.add(iClass);
    }
  }
  
  //
  // bean instance interception
  //

  /**
   * Generates the prologue for the bean instance.
   */
  @Override
  public void generateBeanPrologue(JavaWriter out,
                                   HashMap<String,Object> map)
    throws IOException
  {
    super.generateBeanPrologue(out, map);
    
    if (! isEnhanced()) {
      return;
    }

    if (map.get("__caucho_interceptor_objects") == null) {
      map.put("__caucho_interceptor_objects", true);

      out.println();
      out.print("private transient Object []");
      out.println("__caucho_interceptor_objects;");
    }

    generateBeanInterceptorChain(out, map);
    
    _bizMethod.generateInterceptorTarget(out);
  }

  @Override
  public void generateBeanConstructor(JavaWriter out, 
                                  HashMap<String,Object> map)
    throws IOException
  {
    super.generateBeanConstructor(out, map);
    
    if (! isEnhanced())
      return;
    
    if (hasInterceptor()) {
      generateInterceptorConstructor(out, map);
    }
    
    if (hasDecorator()) {
      generateDecoratorBeanConstructor(out, map);
    }
  }

  private void generateInterceptorConstructor(JavaWriter out,
                                              HashMap<String,Object> map)
    throws IOException
  {
    if (map.get("interceptor_object_init") != null)
      return;
    
    map.put("interceptor_object_init", true);

    out.println("int size = __caucho_interceptor_beans.size();");
    out.println("__caucho_interceptor_objects = new Object[size];");

    out.println();
    out.println("javax.enterprise.context.spi.CreationalContext env");
    // XXX: should be parent bean
    out.println("  = __caucho_manager.createCreationalContext(null);");
    out.println();
    out.println("for (int i = 0; i < size; i++) {");
    out.pushDepth();

    out.println("javax.enterprise.inject.spi.Bean bean");
    out.println("  = __caucho_interceptor_beans.get(i);");
    out.print("__caucho_interceptor_objects[i] = ");
    out.println("__caucho_manager.getReference(bean, bean.getBeanClass(), env);");

    out.popDepth();
    out.println("}");
    
    for (Class<?> iClass : _ownInterceptors) {
      String var = _interceptorVarMap.get(iClass);

      out.println("if (" + var + "_f == null)");
      out.println("  " + var + "_f = __caucho_manager.createTransient("
                  + iClass.getName() + ".class);");

      out.print(var + " = (");
      out.printClass(iClass);
      out.println(") __caucho_manager.getInstance(" + var + "_f);");
    }
  }

  private void generateBeanInterceptorChain(JavaWriter out, 
                                            HashMap map)
    throws IOException
  {
    _bindingEntry
      = new InterceptionBinding(_interceptionType, _interceptorBinding);

    _chainName = (String) map.get(_bindingEntry);

    if (_chainName != null) {
      return;
    }

    _chainName = getUniqueName(out);
    map.put(_bindingEntry, _chainName);

    if (_interceptors.size() > 0) {
      List<Class<?>> interceptors = (List<Class<?>>) map.get("@Interceptors");

      if (interceptors == null)
        interceptors = new ArrayList<Class<?>>();

      int []indexChain = new int[_interceptors.size()];
    }
  }
  
  //
  // business method interception
  //

  /**
   * Generates the prologue for a method.
   */
  @Override
  public void generateMethodPrologue(JavaWriter out,
                                    HashMap<String,Object> map)
    throws IOException
  {
    super.generateMethodPrologue(out, map);
    
    if (! isEnhanced()) {
      return;
    }

    if (map.get("__caucho_manager") == null) {
      map.put("__caucho_manager", true);

      out.println();
      out.print("private static ");
      out.printClass(InjectManager.class);
      out.println(" __caucho_manager");
      out.print(" = ");
      out.printClass(InjectManager.class);
      out.println(".create();");
    }

    /* XXX:
    if (! _isExcludeDefaultInterceptors)
      _interceptors.addAll(_view.getBean().getDefaultInterceptors());
    */

    if (hasInterceptor())
      generateInterceptorMethodPrologue(out, map);

    if (hasDecorator())
      generateDecoratorMethodPrologue(out, map);
    
    // _bizMethod.generateInterceptorTarget(out);
  }

  @Override
  public void generatePreTry(JavaWriter out)
    throws IOException
  {
    if (! isEnhanced()) {
      super.generatePreTry(out);
      return;
    }
    
    if (hasDecorator()) {
      generateDecoratorPreTry(out);
    }
  }

  @Override
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
    if (! isEnhanced()) {
      super.generatePreCall(out);
      return;
    }
    
    if (hasDecorator()) {
      generateDecoratorPreCall(out);
    }
  }

  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
    if (! isEnhanced()) {
      super.generateCall(out);
      return;
    }
    
    if (hasInterceptor()) {
      generateInterceptorCall(out);
    }
    else if (hasDecorator()) {
      generateDecoratorCall(out);
    }
    else {
      throw new IllegalStateException();
    }
  }

  @Override
  public void generateFinally(JavaWriter out)
    throws IOException
  {
    if (! isEnhanced()) {
      super.generateFinally(out);
      return;
    }
    
    if (hasDecorator()) {
      generateDecoratorFinally(out);
    }
  }
  
  //
  // interceptor
  //

  public void generateInterceptorMethodPrologue(JavaWriter out, 
                                          HashMap<String,Object> map)
    throws IOException
  {

    if (map.get("__caucho_interceptor_beans") == null) {
      map.put("__caucho_interceptor_beans", true);

      out.println();
      out.print("private static final ");
      out.print("java.util.ArrayList<");
      out.printClass(Interceptor.class);
      out.println("> __caucho_interceptor_beans");
      out.print("  = new java.util.ArrayList<");
      out.printClass(Interceptor.class);
      out.println(">();");
    }

    generateInterceptorMethod(out, map);
    generateInterceptorChain(out, map);
  }

  public void generateInterceptorMethod(JavaWriter out, 
                                        HashMap<String,Object> map)
    throws IOException
  {
    out.println();
    out.println("private static java.lang.reflect.Method "
                + getUniqueName(out) + "_method;");
    out.println("private static java.lang.reflect.Method "
                + getUniqueName(out) + "_implMethod;");

    boolean isAroundInvokePrologue = false;
    if (getAroundInvokeMethod() != null
        && map.get("ejb.around-invoke") == null) {
      isAroundInvokePrologue = true;
      map.put("ejb.around-invoke", "_caucho_aroundInvokeMethod");

      out.println(
        "private static java.lang.reflect.Method __caucho_aroundInvokeMethod;");
    }

    out.println();
    out.println("static {");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    out.print(getUniqueName(out) + "_method = ");
    generateGetMethod(out,
                      _implMethod.getDeclaringClass().getName(),
                      _implMethod.getName(),
                      _implMethod.getParameterTypes());
    out.println(";");
    out.println(getUniqueName(out) + "_method.setAccessible(true);");

    String superMethodName;

    if (hasDecorator())
      superMethodName = "__caucho_" + _implMethod.getName() + "_decorator";
    else
      superMethodName = "__caucho_" + _implMethod.getName();

    out.print(getUniqueName(out) + "_implMethod = ");
    generateGetMethod(out,
                      _bizMethod.getView().getBeanClassName(),
                      superMethodName,
                      _implMethod.getParameterTypes());
    out.println(";");
    out.println(getUniqueName(out) + "_implMethod.setAccessible(true);");

    if (isAroundInvokePrologue) {
      ApiMethod aroundInvoke = getAroundInvokeMethod();

      out.print("__caucho_aroundInvokeMethod = ");
      generateGetMethod(out,
                        aroundInvoke.getDeclaringClass().getName(),
                        aroundInvoke.getName(),
                        aroundInvoke.getParameterTypes());
      out.println(";");
      out.println("__caucho_aroundInvokeMethod.setAccessible(true);");
    }

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  e.printStackTrace();");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    out.popDepth();
    out.println("}");
  }

  private void generateInterceptorChain(JavaWriter out, 
                                        HashMap map)
    throws IOException
  {
    _bindingEntry
      = new InterceptionBinding(_interceptionType, _interceptorBinding);

    _chainName = (String) map.get(_bindingEntry);

    if (_chainName != null) {
      return;
    }

    _chainName = getUniqueName(out);
    map.put(_bindingEntry, _chainName);

    if (_interceptors.size() > 0) {
      List<Class<?>> interceptors = (List<Class<?>>) map.get("@Interceptors");

      if (interceptors == null)
        interceptors = new ArrayList<Class<?>>();

      int []indexChain = new int[_interceptors.size()];

      out.println("static {");
      out.pushDepth();
      for (int i = 0; i < _interceptors.size(); i++) {
        Class<?> iClass = _interceptors.get(i);
        int index = interceptors.indexOf(iClass);

        if (index > -1) {
          indexChain[i] = index;
        }
        else {
          indexChain[i] = interceptors.size();

          interceptors.add(iClass);
          out.print("__caucho_interceptor_beans.add(new com.caucho.config.inject.InterceptorBean(");
          out.printClass(iClass);
          out.println(".class));");
        }
      }
      out.popDepth();
      out.println("}");
      
      out.println();

      out.print("private static int []" + _chainName
                + "_objectIndexChain = new int[] {");

      for (int i : indexChain) {
        out.print(i);
        out.print(',');
      }

      out.println("};");
    } else {

      out.println("private static int []"
                  + _chainName + "_objectIndexChain;");
    }

    out.println("private static java.lang.reflect.Method []"
                + _chainName + "_methodChain;");

    out.println();
    out.println("static {");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    generateMethodChain(out);

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    out.popDepth();
    out.println("}");
  }

  private void generateMethodChain(JavaWriter out)
    throws IOException
  {
    out.println(_chainName + "_objectIndexChain =");
    out.println("  com.caucho.config.gen.EjbUtil.createInterceptors(");
    out.println("    __caucho_manager,");
    out.println("    __caucho_interceptor_beans,");
    out.println(_chainName+ "_objectIndexChain,");
    out.print("    " + InterceptionType.class.getName()
              + "." + _interceptionType);

    for (int i = 0; i < _interceptorBinding.size(); i++) {
      out.println(",");

      out.pushDepth();
      out.pushDepth();
      generateAnnotation(out, _interceptorBinding.get(i));
      out.popDepth();
      out.popDepth();
    }

    out.println(");");

    out.println();
    out.println(_chainName + "_methodChain = ");
    out.println("  com.caucho.config.gen.EjbUtil.createMethods(");
    out.println("    __caucho_interceptor_beans,");
    out.println("    " + InterceptionType.class.getName()
                + "." + _interceptionType + ",");
    out.println("    " + _chainName + "_objectIndexChain);");
  }
  
  private void generateInterceptorCall(JavaWriter out)
    throws IOException
  {
    String uniqueName = getUniqueName(out);
    
    out.println("try {");
    out.pushDepth();

    if (! void.class.equals(_implMethod.getReturnType())) {
      out.print("result = (");
      printCastClass(out, _implMethod.getReturnType());
      out.print(") ");
    }

    out.print("new com.caucho.config.gen.InvocationContextImpl(");
    generateThis(out);
    out.print(", ");
    // generateThis(out);
    out.print(uniqueName + "_method, ");
    // generateThis(out);
    out.print(uniqueName + "_implMethod, ");
    // generateThis(out);
    out.print(_chainName + "_methodChain, ");
    generateThis(out);
    out.print(".__caucho_interceptor_objects, ");
    // generateThis(out);
    out.print(_chainName + "_objectIndexChain, ");

    Class<?>[] paramTypes = _implMethod.getParameterTypes();

    if (paramTypes.length == 0) {
      out.print("com.caucho.config.gen.EjbUtil.NULL_OBJECT_ARRAY");
    }
    else {
      out.print("new Object[] { ");
      for (int i = 0; i < _implMethod.getParameterTypes().length; i++) {
        out.print("a" + i + ", ");
      }
      out.print("}");
    }

    out.println(").proceed();");

    /*
    // super.generatePostCall(out);

    if (! void.class.equals(_implMethod.getReturnType())) {
      out.println("return result;");
    }
    */

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");

    boolean isException = false;
    Class<?>[] exnList = _implMethod.getExceptionTypes();
    for (Class<?> cl : exnList) {
      if (RuntimeException.class.isAssignableFrom(cl))
        continue;

      if (! isMostGeneralException(exnList, cl))
        continue;

      if (cl.isAssignableFrom(Exception.class))
        isException = true;

      out.println("} catch (" + cl.getName() + " e) {");
      out.println("  throw e;");
    }

    if (! isException) {
      out.println("} catch (Exception e) {");
      out.println("  throw new RuntimeException(e);");
    }

    out.println("}");
  }
  
  //
  // decorator method
  //
  
  private void generateDecoratorBeanConstructor(JavaWriter out,
                                                HashMap<String,Object> map)
    throws IOException
  {
    if (_decoratorSet == null)
      return;
    
    if (map.get("decorator_beans_new") != null)
      return;
    
    map.put("decorator_beans_new", true);

    out.println();
    out.println("if (" + _decoratorBeansVar + " == null) {");
    out.pushDepth();
    
    out.println(_delegateVar + " = new " + _decoratorClass + "(0);");
    
    out.println();
    out.print(_decoratorBeansVar);
    out.print(" = __caucho_manager.resolveDecorators(");
    out.printClass(_view.getBean().getBeanClass().getJavaClass());
    out.println(".class);");

    out.println();
    out.print(_decoratorIndexVar + " = ");
    out.print("com.caucho.config.gen.EjbUtil.generateProxyDelegate(");
    out.print("__caucho_manager, ");
    out.println(_decoratorBeansVar + ", " + _delegateVar + ");");
    
    out.popDepth();
    out.println("}");
  }

  public void generateDecoratorMethodPrologue(JavaWriter out, 
                                              HashMap<String,Object> map)
    throws IOException
  {
    generateDecoratorClass(out, map);

    String decoratorSetName = getDecoratorSetName(out, map);

    if (hasInterceptor())
      generateDecoratorMethod(out);
        
    if (map.get("decorator_bean_" + decoratorSetName) != null)
      return;
    
    map.put("decorator_bean_" + decoratorSetName, true);

    if (map.get("decorator_delegate_decl") == null) {
      map.put("decorator_delegate_decl", true);
    
      out.print("private static ");
      out.print(_decoratorClass);
      out.println(" " + _delegateVar + ";");
      
      out.println();
      out.println("private static "
                  + "java.util.List<javax.enterprise.inject.spi.Decorator<?>> "
                  + _decoratorBeansVar + ";");
    
      out.println();
      out.println("static final ThreadLocal<" + _decoratorClass + "> "
                  + _decoratorLocalVar);
      out.println("  = new ThreadLocal<" + _decoratorClass + ">();");

      out.println();
      out.println("private transient Object [] " + _decoratorIndexVar + ";");
    }
  }

  private void generateDecoratorClass(JavaWriter out,
                                      HashMap<String,Object> map)
    throws IOException
  {
    if (map.get("decorator_class_decl") != null)
      return;
    
    map.put("decorator_class_decl", true);
    
    String className = _decoratorClass;

    ArrayList<Type> decorators = _view.getBean().getDecoratorTypes();

    out.println();
    out.print("class ");
    out.print(className);
    out.print(" ");

    for (int i = 0; i < decorators.size(); i++) {
      Class <?> cl = (Class<?>) decorators.get(i);
      
      if (! cl.isInterface()) {
        out.print(" extends ");

        out.printClass(cl);
      }
    }

    boolean isFirst = true;
    for (int i = 0; i < decorators.size(); i++) {
      Class <?> cl = (Class<?>) decorators.get(i);
      
      if (! cl.isInterface())
        continue;
      
      if (isFirst)
        out.print(" implements ");
      else
        out.print(", ");
      
      isFirst = false;

      out.printClass(cl);
    }

    out.println(" {");
    out.pushDepth();

    out.println("private int _index;");

    out.println();
    out.println(className + "(int index)");
    out.println("{");
    out.println("  _index = index;");
    out.println("}");
    
    out.println();
    out.print("final ");
    out.print(_bizMethod.getView().getViewClassName());
    out.println(" __caucho_getBean()");
    out.println("{");
    out.println("  return " + _bizMethod.getView().getViewClassName()
                + ".this;");
    out.println("}");

    ArrayList<Method> methodList = new ArrayList<Method>();
    HashMap<ArrayList<Class<?>>, String> apiMap
    = new HashMap<ArrayList<Class<?>>, String>();

    for (Type decorator : decorators) {
      Class<?> decoratorClass = (Class<?>) decorator;

      for (Method method : decoratorClass.getMethods()) {
        if (Modifier.isFinal(method.getModifiers())
            || Modifier.isStatic(method.getModifiers())
            || Modifier.isPrivate(method.getModifiers())
            || (method.getDeclaringClass() == Object.class
                && ! method.getName().equals("toString"))) {
          continue;
        }
        
        if (! containsMethod(methodList, method)) {
          methodList.add(method);

          generateDecoratorMethod(out, method, apiMap);
        }
      }
    }

    out.popDepth();
    out.println("}");

    for (Map.Entry<ArrayList<Class<?>>, String> entry : apiMap.entrySet()) {
      ArrayList<Class<?>> apis = entry.getKey();
      String name = entry.getValue();

      out.println();
      out.println("static final Class []" + name + " = new Class[] {");
      out.pushDepth();
      
      for (int i = 0; i < apis.size(); i++) {
        out.printClass(apis.get(i));
        out.println(".class,");
      }
      
      out.popDepth();
      out.println("};");
    }
  }

  private void generateDecoratorMethod(JavaWriter out,
                                       Method method,
                                       HashMap<ArrayList<Class<?>>, String> apiMap)
    throws IOException
  {
    String decoratorSetName = _decoratorSetName;

    String uniqueName = getUniqueName(out);

    ArrayList<Class<?>> apis = getMethodApis(method);

    String apiName = apiMap.get(apis);
    if (apiName == null && apis.size() > 1) {
      apiName = uniqueName + "_api_" + apiMap.size();

      apiMap.put(apis, apiName);
    }

    Class<?> decoratorType = apis.get(0);

    out.println();
    out.print("public ");
    out.printClass(method.getReturnType());
    out.print(" ");

    out.print(method.getName());

    out.print("(");

    Class<?>[] paramTypes = method.getParameterTypes();
    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
        out.print(", ");

      out.printClass(paramTypes[i]);
      out.print(" a" + i);
    }
    out.println(")");
    Class<?>[] exnTypes = method.getExceptionTypes();

    if (exnTypes.length > 0) {
      out.print("  throws ");
      for (int i = 0; i < exnTypes.length; i++) {
        if (i != 0)
          out.print(", ");
        out.printClass(exnTypes[i]);
      }
    }

    out.println("{");
    out.pushDepth();

    out.println(_decoratorClass + " var = " + _decoratorLocalVar + ".get();");

    out.println("Object []delegates = var.__caucho_getBean()." + _decoratorIndexVar + ";");
    
    out.println();
    out.print("var._index = com.caucho.config.gen.EjbUtil.nextDelegate(");
    out.print("delegates, ");
    if (apis.size() > 1) {
      out.print(apiName);
    }
    else {
      out.printClass(decoratorType);
      out.print(".class");
    }
    out.println(", var._index);");

    out.println();
    out.println("if (var._index >= 0) {");
    out.pushDepth();

    out.println("Object delegate = delegates[var._index];");

    for (int j = 0; j < apis.size(); j++) {
      if (j > 0)
        out.print("else ");

      if (j + 1 < apis.size()) {
        out.print("if (delegate instanceof ");
        out.printClass(apis.get(j));
        out.print(")");
      }

      if (apis.size() > 1) {
        out.println();
        out.print("  ");
      }

      if (! void.class.equals(method.getReturnType()))
        out.print("return ");

      out.print("((");
      out.printClass(apis.get(j));
      out.print(") delegate)." + method.getName() + "(");
      for (int i = 0; i < paramTypes.length; i++) {
        if (i != 0)
          out.print(", ");

        out.print("a" + i);
      }
      out.println(");");
    }

    out.popDepth();
    out.println("}");

    out.println("else");
    out.pushDepth();

    if (! void.class.equals(method.getReturnType()))
      out.print("return ");

    out.print("var.__caucho_getBean().");
    out.print("__caucho_" + method.getName());
    out.print("(");
    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
        out.print(", ");

      out.print("a" + i);
    }
    out.println(");");

    out.popDepth();
    out.popDepth();
    out.println("}");
  }
  
  //
  // method generators
  //

  protected void generateDecoratorMethod(JavaWriter out)
    throws IOException
  {
    out.println();
    out.print("private ");
    out.printClass(_implMethod.getReturnType());
    out.print(" __caucho_");
    out.print(_implMethod.getName());
    out.print("_decorator(");

    Class<?>[] types = _implMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      Class<?> type = types[i];

      if (i != 0)
        out.print(", ");

      out.printClass(type);
      out.print(" a" + i);
    }

    out.println(")");
    _bizMethod.generateThrows(out, _implMethod.getExceptionTypes());

    out.println("{");
    out.pushDepth();

    if (! void.class.equals(_implMethod.getReturnType())) {
      out.printClass(_implMethod.getReturnType());
      out.println(" result;");
    }

    generateDecoratorCall(out);

    if (! void.class.equals(_implMethod.getReturnType())) {
      out.println("return result;");
    }

    out.popDepth();
    out.println("}");
  }
  
  protected void generateDecoratorPreTry(JavaWriter out)
    throws IOException
  {
    out.print(_decoratorClass + " oldDecorator = ");
    out.println(_decoratorLocalVar + ".get();");
  }
  
  protected void generateDecoratorPreCall(JavaWriter out)
    throws IOException
  {
    String decoratorSetName = _decoratorSetName;
    
    assert(decoratorSetName != null);

    out.println();
    out.print(_decoratorClass + " delegate = ");
    out.print("new " + _decoratorClass + "(");
    out.println(_decoratorIndexVar + ".length);");
 
    out.print(_decoratorLocalVar);
    out.println(".set(delegate);");
  }

  /**
   * Generates the low-level decorator call at the end of the chain.
   */
  protected void generateDecoratorCall(JavaWriter out)
    throws IOException
  {
    String decoratorSetName = _decoratorSetName;
    
    assert(decoratorSetName != null);

    //_bizMethod.generateTailCall(out);
    if (! void.class.equals(_implMethod.getReturnType()))
      out.print("result = ");

    out.print("__caucho_delegate.");
    out.print(_implMethod.getName());
    out.print("(");

    for (int i = 0; i < _implMethod.getParameterTypes().length; i++) {
      if (i != 0)
        out.print(", ");

      out.print("a" + i);
    }

    out.println(");");
  }
  
  protected void generateDecoratorFinally(JavaWriter out)
    throws IOException
  {
    out.print(_decoratorLocalVar);
    out.println(".set(oldDecorator);");
  }
  
  //
  // utilities
  //
  
  private String getUniqueName(JavaWriter out)
  {
    if (_uniqueName == null)
      _uniqueName = "_" + _implMethod.getName() + "_v" + out.generateId();

    return _uniqueName;
  }
  
  private String getDecoratorSetName(JavaWriter out,
                                     Map<String,Object> map)
  {
    if (_decoratorSetName != null)
      return _decoratorSetName;
    
    HashMap<HashSet<Class<?>>,String> nameMap;
    
    nameMap = (HashMap<HashSet<Class<?>>,String>) map.get("decorator_name_map");
    
    if (nameMap == null) {
      nameMap = new HashMap<HashSet<Class<?>>,String>();
      map.put("decorator_name_map", nameMap);
    }
    
    String name = nameMap.get(_decoratorSet);
    
    if (name == null) {
      name = "__caucho_decorator_" + out.generateId();
      nameMap.put(_decoratorSet, name);
    }
    
    _decoratorSetName = name;
    
    return name;
  }

  protected void generateAnnotation(JavaWriter out,
                                    Annotation ann)
    throws IOException
  {
    out.print("new javax.enterprise.util.AnnotationLiteral<");
    out.printClass(ann.annotationType());
    out.print(">() {");

    boolean isFirst = true;
    for (Method method : ann.annotationType().getMethods()) {
      if (method.getDeclaringClass().equals(Object.class))
        continue;

      if (method.getDeclaringClass().equals(Annotation.class))
        continue;

      if (method.getName().equals("annotationType"))
        continue;

      if (method.getParameterTypes().length > 0)
        continue;

      if (void.class.equals(method.getReturnType()))
        continue;

      if (method.isAnnotationPresent(Nonbinding.class))
        continue;

      out.pushDepth();

      if (! isFirst)
        out.print(",");
      isFirst = false;

      out.println();

      out.print("public ");
      out.printClass(method.getReturnType());
      out.print(" " + method.getName() + "() { return ");

      Object value = null;

      try {
        value = method.invoke(ann);
      }
      catch (Exception e) {
        throw ConfigException.create(e);
      }

      printValue(out, value);

      out.println("; }");

      out.popDepth();
    }

    out.print("}");
  }

  private void printValue(JavaWriter out, Object value)
    throws IOException
  {
    if (value == null)
      out.print("null");
    else if (value instanceof String) {
      out.print("\"");
      out.printJavaString((String) value);
      out.print("\"");
    }
    else if (value instanceof Character) {
      out.print("\'");
      out.printJavaString(String.valueOf(value));
      out.print("\'");
    }
    else if (value instanceof Enum<?>) {
      out.printClass(value.getClass());
      out.print("." + value);
    }
    else
      out.print(value);
  }

  private ArrayList<Class<?>> getMethodApis(Method method)
  {
    ArrayList<Class<?>> apis = new ArrayList<Class<?>>();

    for (Type decorator : _view.getBean().getDecoratorTypes()) {
      Class<?> decoratorClass = (Class<?>) decorator;

      if (containsMethod(decoratorClass.getMethods(), method)
          && ! apis.contains(decoratorClass))
        apis.add(decoratorClass);
    }

    return apis;
  }

  private boolean hasInterceptor()
  {
    return (_interceptors.size() > 0
            || _interceptorBinding.size() > 0
            || getAroundInvokeMethod() != null);
  }

  private boolean hasDecorator()
  {
    return _decoratorSet != null;
  }

  protected void generateThis(JavaWriter out)
    throws IOException
  {
    _bizMethod.generateThis(out);
  }

  private boolean containsMethod(ArrayList<Method> methodList, Method method)
  {
    for (Method oldMethod : methodList) {
      if (isMatch(oldMethod, method))
        return true;
    }

    return false;
  }

  private void addInterceptorBindings(HashMap<Class<?>,Annotation> interceptorTypes,
                                      Set<Annotation> annotations)
  {
    for (Annotation ann : annotations) {
      addInterceptorBindings(interceptorTypes, ann);
    }
  }

  private void addInterceptorBindings(HashMap<Class<?>,Annotation> interceptorTypes,
                                      Annotation ann)
  {
    Class<?> annType = ann.annotationType();

    if (annType.isAnnotationPresent(InterceptorBinding.class)) {
      interceptorTypes.put(ann.annotationType(), ann);
    }

    if (annType.isAnnotationPresent(Stereotype.class)) {
      for (Annotation subAnn : annType.getAnnotations()) {
        addInterceptorBindings(interceptorTypes, subAnn);
      }
    }
  }

  private boolean containsMethod(Method[] methodList, Method method)
  {
    for (Method oldMethod : methodList) {
      if (isMatch(oldMethod, method))
        return true;
    }

    return false;
  }

  private boolean isMostGeneralException(Class<?>[] exnList, Class<?> cl)
  {
    for (Class<?> exn : exnList) {
      if (exn != cl && exn.isAssignableFrom(cl))
        return false;
    }

    return true;
  }

  protected Method findInterceptorMethod(Class<?> cl)
  {
    if (cl == null)
      return null;

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class))
        return method;
    }

    return findInterceptorMethod(cl.getSuperclass());
  }

  /*
  protected void generateObjectChain(JavaWriter out)
    throws IOException
  {
    generateThis(out);
    out.print("." + _uniqueName + "_objectChain = new Object[] { ");

    for (Interceptor interceptor : _interceptors) {
      generateThis(out);
      out.print("." + _interceptorVarMap.get(interceptor) + ", ");
    }

    if (getAroundInvokeMethod() != null) {
      generateThis(out);
      out.print(", ");
    }

    out.println("};");
  }
  */

  protected void generateGetMethod(JavaWriter out, Method method)
    throws IOException
  {
    generateGetMethod(out,
                      method.getDeclaringClass().getName(),
                      method.getName(),
                      method.getParameterTypes());
  }

  protected boolean isValidMethod(ApiMethod implMethod)
  {
    if (implMethod == null) {
      return false;
    }

    if (implMethod.isStatic()) {
      return false;
    }

    if (implMethod.isFinal()) {
      return false;
    }

    if (! implMethod.isPublic() && ! implMethod.isProtected()) {
      return false;
    }

    return true;
  }

  private boolean isMatch(Method methodA, Method methodB)
  {
    if (! methodA.getName().equals(methodB.getName()))
      return false;

    Class<?>[] paramA = methodA.getParameterTypes();
    Class<?>[] paramB = methodB.getParameterTypes();

    if (paramA.length != paramB.length)
      return false;

    for (int i = 0; i < paramA.length; i++) {
      if (! paramA[i].equals(paramB[i]))
        return false;
    }

    return true;
  }

  protected void generateGetMethod(JavaWriter out,
                                   String className,
                                   String methodName,
                                   Class<?>[] paramTypes)
    throws IOException
  {
    out.print("com.caucho.config.gen.EjbUtil.getMethod(");
    out.print(className + ".class");
    out.print(", \"" + methodName + "\", new Class[] { ");

    for (Class<?> type : paramTypes) {
      out.printClass(type);
      out.print(".class, ");
    }
    out.print("})");
  }

  protected void printCastClass(JavaWriter out, Class<?> type)
    throws IOException
  {
    if (! type.isPrimitive())
      out.printClass(type);
    else if (boolean.class.equals(type))
      out.print("Boolean");
    else if (char.class.equals(type))
      out.print("Character");
    else if (byte.class.equals(type))
      out.print("Byte");
    else if (short.class.equals(type))
      out.print("Short");
    else if (int.class.equals(type))
      out.print("Integer");
    else if (long.class.equals(type))
      out.print("Long");
    else if (float.class.equals(type))
      out.print("Float");
    else if (double.class.equals(type))
      out.print("Double");
    else
      throw new IllegalStateException(type.getName());
  }

  static class InterceptionBinding {
    private final InterceptionType _type;
    private final ArrayList<Annotation> _binding;

    public InterceptionBinding(InterceptionType type,
                               ArrayList<Annotation> binding)
    {
      _type = type;
      _binding = binding;
    }

    @Override
    public int hashCode()
    {
      return _type.hashCode() * 65521 + _binding.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
      if (o == this)
        return true;
      else if (! (o instanceof InterceptionBinding))
        return false;

      InterceptionBinding binding = (InterceptionBinding) o;

      return (_type.equals(binding._type)
              && _binding.equals(binding._binding));
    }
  }
}
