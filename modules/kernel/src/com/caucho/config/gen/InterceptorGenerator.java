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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InterceptorBinding;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents the interception
 */
@Module
public class InterceptorGenerator<X>
  extends AbstractAspectGenerator<X>
{
  private InterceptorFactory<X> _factory;

  private String _uniqueName;
  private String _chainName;

  private boolean _isExcludeDefaultInterceptors;
  private boolean _isExcludeClassInterceptors;

  private InterceptionType _interceptionType = InterceptionType.AROUND_INVOKE;

  private ArrayList<Annotation> _interceptorBinding
    = new ArrayList<Annotation>();

  private ArrayList<Class<?>> _interceptors
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

  public InterceptorGenerator(InterceptorFactory<X> factory,
                              AnnotatedMethod<? super X> method,
                              AspectGenerator<X> next,
                              HashSet<Class<?>> methodInterceptors,
                              HashMap<Class<?>, Annotation> methodInterceptorMap,
                              HashSet<Class<?>> decoratorSet,
                              boolean isExcludeClassInterceptors)
  {
    super(factory, method, next);

    _factory = factory;
    
    if (factory.getClassInterceptors() != null
        && ! isExcludeClassInterceptors) {
      _interceptors.addAll(factory.getClassInterceptors());
    }

    if (methodInterceptors != null)
      _interceptors.addAll(methodInterceptors);

    if (methodInterceptorMap != null)
      _interceptorBinding.addAll(methodInterceptorMap.values());
    
    if (factory.getClassInterceptorBinding() != null
        && ! _isExcludeClassInterceptors) {
      _interceptorBinding.addAll(factory.getClassInterceptorBinding());
    }

    _decoratorSet = decoratorSet;

    introspect();
  }

  public ArrayList<Class<?>> getInterceptors()
  {
    return _interceptors;
  }

  private AnnotatedMethod<? super X> getAroundInvokeMethod()
  {
    return _factory.getAroundInvokeMethod();
  }

  private boolean isProxy()
  {
    return _factory.getAspectBeanFactory().isProxy();
  }

  //
  // introspection
  //

  /**
   * Introspects the @Interceptors annotation on the method and the class.
   */
  private void introspect()
  {
    /*
    if (implMethod.isAnnotationPresent(Inject.class)
        || implMethod.isAnnotationPresent(PostConstruct.class)) {
      // ioc/0a23, ioc/0c57
      return;
    }
    */

    introspectInterceptors();

    // introspectDefaults();
  }

  private void introspectInterceptors()
  {
    // if (apiMethod.isAnnotationPresent(ExcludeClassInterceptors.class))
    //   _isExcludeClassInterceptors = true;

    //if (apiMethod.isAnnotationPresent(ExcludeDefaultInterceptors.class))
    //  _isExcludeDefaultInterceptors = true;

    /*
    if (interceptorTypes.size() > 0) {
      _interceptionType = InterceptionType.AROUND_INVOKE;
      _interceptorBinding.addAll(interceptorTypes.values());
    }
    */
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

    if (map.get("__caucho_interceptor_objects") == null) {
      map.put("__caucho_interceptor_objects", true);

      out.println();
      out.print("private transient Object []");
      out.println("__caucho_interceptor_objects;");

      out.println("Object []_caucho_getInterceptorObjects()");
      out.println("{ return __caucho_interceptor_objects; }");
    }

    generateBeanInterceptorChain(out, map);
    
    generateTail(out);

    // generateInterceptorTarget(out);
  }

  @Override
  public void generateBeanConstructor(JavaWriter out,
                                  HashMap<String,Object> map)
    throws IOException
  {
    super.generateBeanConstructor(out, map);

    /*
    if (hasInterceptor()) {
      generateInterceptorBeanConstructor(out, map);
    }
    */

    if (hasDecorator()) {
      generateDecoratorBeanConstructor(out, map);
    }
  }

  @Override
  public void generateInject(JavaWriter out,
                             HashMap<String,Object> map)
    throws IOException
  {
    super.generateInject(out, map);

    if (hasInterceptor()) {
      generateInterceptorBeanInject(out, map);
    }

    if (hasDecorator()) {
      generateDecoratorBeanInject(out, map);
    }
  }

  @Override
  public void generateProxyConstructor(JavaWriter out,
                                       HashMap<String,Object> map)
    throws IOException
  {
    super.generateProxyConstructor(out, map);

    if (hasDecorator()) {
     // generateDecoratorProxyConstructor(out, map);
    }
  }

  private void generateInterceptorBeanInject(JavaWriter out,
                                             HashMap<String,Object> map)
    throws IOException
  {
    if (map.get("interceptor_object_init") != null)
      return;

    map.put("interceptor_object_init", true);

    out.println("int size = __caucho_interceptor_beans.size();");
    out.println("__caucho_interceptor_objects = new Object[size];");

    out.println();
    // XXX: should be parent bean
    // out.println("  = __caucho_manager.createCreationalContext(null);");
    out.println();
    out.println("for (int i = 0; i < size; i++) {");
    out.pushDepth();

    out.println("javax.enterprise.inject.spi.Bean bean");
    out.println("  = __caucho_interceptor_beans.get(i);");
    
    out.println("javax.enterprise.context.spi.CreationalContext env");
    out.println("  = new " + CreationalContextImpl.class.getName() + "(bean, parentEnv);");
    
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

  // XXX This map is really a Map<String, Object>, so putting in an
  // InterceptionBinding seems wrong.  Also, it doesn't discriminate the 
  // chainName -- see the generated code for test1() in ejb/10a5
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

      // int []indexChain = new int[_interceptors.size()];
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
    // ejb/12a0
    super.generatePreTry(out);
    
    if (hasDecorator()) {
      generateDecoratorPreTry(out);
    }
  }

  @Override
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
    if (hasDecorator()) {
      generateDecoratorPreCall(out);
    }
  }

  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
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
    // server/12a0
    super.generateFinally(out);
    
    if (hasDecorator()) {
      generateDecoratorFinally(out);
    }
  }

  //
  // interceptor
  //

  private void generateInterceptorMethodPrologue(JavaWriter out,
                                                 HashMap<String,Object> map)
    throws IOException
  {
    if (map.get("__caucho_interceptor_beans") == null) {
      map.put("__caucho_interceptor_beans", true);

      out.println();
      out.print("private static final ");
      out.print("java.util.ArrayList<");
      out.printClass(Interceptor.class);
      out.println("<?>> __caucho_interceptor_beans");
      out.print("  = new java.util.ArrayList<");
      out.printClass(Interceptor.class);
      out.println("<?>>();");
    }

    generateInterceptorMethod(out, map);
    generateInterceptorChain(out, map);
  }

  private void generateInterceptorMethod(JavaWriter out,
                                         HashMap<String,Object> map)
    throws IOException
  {
    Method javaMethod = getJavaMethod();

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
                      javaMethod.getDeclaringClass().getName(),
                      javaMethod.getName(),
                      javaMethod.getParameterTypes());
    out.println(";");
    out.println(getUniqueName(out) + "_method.setAccessible(true);");

    String superMethodName;

    if (hasDecorator())
      superMethodName = "__caucho_" + javaMethod.getName() + "_decorator";
    else if (isProxy())
      superMethodName = javaMethod.getName();
    else {
      superMethodName = "__caucho_" + javaMethod.getName();
    }

    out.print(getUniqueName(out) + "_implMethod = ");
    generateGetMethod(out,
                      getBeanFactory().getGeneratedClassName(),
                      superMethodName,
                      javaMethod.getParameterTypes());
    out.println(";");
    out.println(getUniqueName(out) + "_implMethod.setAccessible(true);");

    if (isAroundInvokePrologue) {
      AnnotatedMethod<? super X> aroundInvoke = getAroundInvokeMethod();

      out.print("__caucho_aroundInvokeMethod = ");
      generateGetMethod(out,
                        aroundInvoke.getJavaMember().getDeclaringClass().getName(),
                        aroundInvoke.getJavaMember().getName(),
                        aroundInvoke.getJavaMember().getParameterTypes());
      out.println(";");
      out.println("__caucho_aroundInvokeMethod.setAccessible(true);");
    }

    out.popDepth();
    out.println("} catch (Exception e) {");
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
    out.println("  com.caucho.config.gen.CandiUtil.createInterceptors(");
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
    out.println("  com.caucho.config.gen.CandiUtil.createMethods(");
    out.println("    __caucho_interceptor_beans,");
    out.println("    " + InterceptionType.class.getName()
                + "." + _interceptionType + ",");
    out.println("    " + _chainName + "_objectIndexChain);");
  }

  private void generateInterceptorCall(JavaWriter out)
    throws IOException
  {
    Method javaMethod = getJavaMethod();

    String uniqueName = getUniqueName(out);

    out.println("try {");
    out.pushDepth();

    if (! void.class.equals(javaMethod.getReturnType())) {
      out.print("result = (");
      printCastClass(out, javaMethod.getReturnType());
      out.print(") ");
    }

    out.print("new com.caucho.config.gen.InvocationContextImpl(");
    out.print(_factory.getAspectBeanFactory().getBeanInstance());

    out.print(", ");
    // generateThis(out);
    out.print(uniqueName + "_method, ");
    // generateThis(out);
    out.print(uniqueName + "_implMethod, ");
    // generateThis(out);
    out.print(_chainName + "_methodChain, ");

    out.print(_factory.getAspectBeanFactory().getBeanInfo());
    out.print("._caucho_getInterceptorObjects(), ");

    // generateThis(out);
    out.print(_chainName + "_objectIndexChain, ");

    Class<?>[] paramTypes = javaMethod.getParameterTypes();

    if (paramTypes.length == 0) {
      out.print("com.caucho.config.gen.CandiUtil.NULL_OBJECT_ARRAY");
    }
    else {
      out.print("new Object[] { ");
      for (int i = 0; i < javaMethod.getParameterTypes().length; i++) {
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
    Class<?>[] exnList = javaMethod.getExceptionTypes();
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
  }

  private void generateDecoratorBeanInject(JavaWriter out,
                                           HashMap<String,Object> map)
    throws IOException
  {
    if (_decoratorSet == null)
      return;

    out.println("__caucho_delegates = delegates;");
  }

  private void generateDecoratorMethodPrologue(JavaWriter out,
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
      
      out.println();
      out.println("final Object []__caucho_getDelegates()");
      out.println("{");
      out.println("  return " + _decoratorIndexVar + ";");
      out.println("}");
      
      generateDecoratorInit(out);
    }
  }
  
  private void generateDecoratorInit(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public static Object __caucho_decorator_init()");
    out.println("{");
    out.pushDepth();
    
    out.println("if (__caucho_delegate == null)");
    out.println("  __caucho_delegate = new __caucho_decorator_class(0, null, null);");
    
    out.println();
    out.println("return __caucho_delegate;");
    
    out.popDepth();
    out.println("}");
  }

  private void generateDecoratorClass(JavaWriter out,
                                      HashMap<String,Object> map)
    throws IOException
  {
    if (map.get("decorator_class_decl") != null)
      return;

    map.put("decorator_class_decl", true);

    String className = _decoratorClass;

    ArrayList<Method> methodList = new ArrayList<Method>();
    HashMap<ArrayList<Class<?>>, String> apiMap
    = new HashMap<ArrayList<Class<?>>, String>();

    for (Class<?> decoratorClass : _factory.getDecoratorClasses()) {
      for (Method method : decoratorClass.getMethods()) {
        if (Modifier.isFinal(method.getModifiers())
            || Modifier.isStatic(method.getModifiers())
            || Modifier.isPrivate(method.getModifiers())
            || (method.getDeclaringClass() == Object.class)) {
          //|| (method.getDeclaringClass() == Object.class
            //    && ! method.getName().equals("toString"))) {
          continue;
        }

        if (! containsMethod(methodList, method)) {
          methodList.add(method);
        }
      }
    }
    
    for (Method method : methodList) {
      generateDecoratorMethodDecl(out, method);
    }
    
    out.println();
    out.println("public static void __caucho_init_decorators(java.util.List<javax.enterprise.inject.spi.Decorator<?>> decoratorList)");
    out.println("{");
    out.pushDepth();
    
    for (Method method : methodList) {
      out.print("_caucho_decorator_" + method.getName() + "_m");
      out.print(" = com.caucho.config.gen.CandiUtil.createDecoratorMethods(decoratorList, ");
      out.print("\"" + method.getName() + "\"");
      
      for (int i = 0; i < method.getParameterTypes().length; i++) {
        out.print(", ");
        out.printClass(method.getParameterTypes()[i]);
        out.print(".class");
      }
      
      out.println(");");
    }
    
    out.popDepth();
    out.println("}");

    out.println();
    out.print("static class ");
    out.print(className);
    out.print(" ");

    for (Class<?> cl : _factory.getDecoratorClasses()) {
      if (! cl.isInterface()) {
        out.print(" extends ");

        out.printClass(cl);
      }
    }

    boolean isFirst = true;
    for (Class<?> cl : _factory.getDecoratorClasses()) {
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

    // String beanClassName = getBeanType().getJavaClass().getName();
    String beanClassName = getFactory().getAspectBeanFactory().getGeneratedClassName();

    out.println("private int _index;");
    out.println("private " + beanClassName + " _bean;");
    out.println("private Object [] _delegates;");

    out.println();
    out.print(className + "(int index, " + beanClassName + " bean, Object []delegates)");
    out.println("{");
    out.println("  _index = index;");
    out.println("  _bean = bean;");
    out.println("  _delegates = delegates;");
    out.println("}");

    String generatedClassName = _factory.getAspectBeanFactory().getGeneratedClassName();
    
    out.println();
    out.println("final " + generatedClassName + " __caucho_getBean()");
    out.println("{");
    /*
    out.println("  return " + getBusinessMethod().getView().getViewClassName()
                + ".this;");
                */
    out.println("  return _bean;");
    out.println("}");

    for (Method method : methodList) {
      generateDecoratorMethod(out, method, apiMap);
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
  
  private void generateDecoratorMethodDecl(JavaWriter out, Method method)
    throws IOException
  {
      out.println("static java.lang.reflect.Method []_caucho_decorator_" + method.getName() + "_m;");
  }

  private void generateDecoratorMethod(JavaWriter out,
                                       Method method,
                                       HashMap<ArrayList<Class<?>>, String> apiMap)
    throws IOException
  {
    AnnotatedMethod<? super X> annMethod
      = AnnotatedTypeUtil.findMethod(getBeanType(),
                                     method.getName(),
                                     method.getParameterTypes());

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

    if (annMethod != null && annMethod.isAnnotationPresent(Inject.class)) {
      out.println(_decoratorClass + " var = " + _decoratorLocalVar + ".get();");
      
      printDecoratorSuperCall(out, method, annMethod);
    }
    else {
      out.println(_decoratorClass + " var = " + _decoratorLocalVar + ".get();");

      // out.println("Object []delegates = var.__caucho_getBean()." + _decoratorIndexVar + ";");
      // out.println("Object []delegates = _bean != null ? _bean." + _decoratorIndexVar + " : null;");
      // out.println("Object []delegates = var._bean.__caucho_getDelegates();");
      
      out.println("Object []delegates = var._delegates;");

      out.println();
      out.print("var._index = com.caucho.config.gen.CandiUtil.nextDelegate(");
      out.print("delegates, ");
      /*
      if (apis.size() > 1) {
        out.print(apiName);
      }
      else {
        out.printClass(decoratorType);
        out.print(".class");
      }
      */
      out.print("_caucho_decorator_" + method.getName() + "_m");
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

        if (! void.class.equals(method.getReturnType())) {
          out.print("return (");
          out.printClass(method.getReturnType());
          out.print(") ");
        }

        /*
        out.print("((");
        out.printClass(apis.get(j));
        out.print(") delegate)." + method.getName());
        */
        
        out.print("com.caucho.config.gen.CandiUtil.invoke(");
        out.print("_caucho_decorator_" + method.getName() + "_m[");
        out.print("var._index");
        out.print("], delegate");
        
        for (int i = 0; i < paramTypes.length; i++) {
          out.print(", ");

          out.print("a" + i);
        }
        out.println(");");
      }

      out.popDepth();
      out.println("}");

      out.println("else");
      out.pushDepth();
      
      printDecoratorSuperCall(out, method, annMethod);
    }

    out.popDepth();
    out.println("}");
  }
  
  private void printDecoratorSuperCall(JavaWriter out,
                                       Method method,
                                       AnnotatedMethod<? super X> annMethod)
    throws IOException
  {
    Class<?>[] paramTypes = method.getParameterTypes();

    if (! void.class.equals(method.getReturnType()))
      out.print("return ");

    if (isProxy()
        || annMethod != null && annMethod.isAnnotationPresent(Inject.class)) {
      out.print("var.__caucho_getBean().");
      out.print(method.getName());
    }
    else {
      out.print("var.__caucho_getBean().");
      out.print("__caucho_" + method.getName());
    }

    out.print("(");
    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
        out.print(", ");

      out.print("a" + i);
    }
    out.println(");");

    out.popDepth();
  }

  //
  // method generators
  //

  private void generateDecoratorMethod(JavaWriter out)
    throws IOException
  {
    Method javaMethod = getJavaMethod();

    out.println();
    out.print("private ");
    out.printClass(javaMethod.getReturnType());
    out.print(" __caucho_");
    out.print(javaMethod.getName());
    out.print("_decorator(");

    Class<?>[] types = javaMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      Class<?> type = types[i];

      if (i != 0)
        out.print(", ");

      out.printClass(type);
      out.print(" a" + i);
    }

    out.println(")");
    AspectGeneratorUtil.generateThrows(out, javaMethod.getExceptionTypes());

    out.println("{");
    out.pushDepth();

    if (! void.class.equals(javaMethod.getReturnType())) {
      out.printClass(javaMethod.getReturnType());
      out.println(" result;");
    }

    generateDecoratorCall(out);

    if (! void.class.equals(javaMethod.getReturnType())) {
      out.println("return result;");
    }

    out.popDepth();
    out.println("}");
  }

  private void generateDecoratorPreTry(JavaWriter out)
    throws IOException
  {
    out.print(_decoratorClass + " oldDecorator = ");
    out.println(_decoratorLocalVar + ".get();");
  }

  private void generateDecoratorPreCall(JavaWriter out)
    throws IOException
  {
    String decoratorSetName = _decoratorSetName;

    assert(decoratorSetName != null);

    out.println();
    out.print(_decoratorClass + " delegate = ");
    out.print("new " + _decoratorClass + "(");
    out.print(_decoratorIndexVar + ".length, ");
    out.print(_factory.getAspectBeanFactory().getBeanInstance() + ", ");
    
    out.print(getBeanFactory().getBeanInfo() + ".__caucho_getDelegates()");
    
    out.println(");");

    out.print(_decoratorLocalVar);
    out.println(".set(delegate);");
  }

  /**
   * Generates the low-level decorator call at the end of the chain.
   */
  private void generateDecoratorCall(JavaWriter out)
    throws IOException
  {
    String decoratorSetName = _decoratorSetName;

    assert(decoratorSetName != null);

    Method javaMethod = getJavaMethod();

    //_bizMethod.generateTailCall(out);
    if (! void.class.equals(javaMethod.getReturnType()))
      out.print("result = ");

    out.print("__caucho_delegate.");
    out.print(javaMethod.getName());
    out.print("(");

    for (int i = 0; i < javaMethod.getParameterTypes().length; i++) {
      if (i != 0)
        out.print(", ");

      out.print("a" + i);
    }

    out.println(");");
  }

  private void generateDecoratorFinally(JavaWriter out)
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
    Method javaMethod = getJavaMethod();

    if (_uniqueName == null)
      _uniqueName = "_" + javaMethod.getName() + "_v" + out.generateId();

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

  private void generateAnnotation(JavaWriter out,
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
        method.setAccessible(true);
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

    for (Class<?> decoratorClass : _factory.getDecoratorClasses()) {
      if (containsMethod(decoratorClass.getMethods(), method)
          && ! apis.contains(decoratorClass))
        apis.add(decoratorClass);
    }

    return apis;
  }

  private boolean hasInterceptor()
  {
    return (_interceptors != null && _interceptors.size() > 0
            || _interceptorBinding != null && _interceptorBinding.size() > 0
            || getAroundInvokeMethod() != null);
  }

  private boolean hasDecorator()
  {
    return _decoratorSet != null;
  }

  private void generateTail(JavaWriter out)
    throws IOException
  {
    Method javaMethod = getJavaMethod();
    
    out.println();
    out.print("private ");
    out.printClass(javaMethod.getReturnType());
    out.print(" __caucho_");
    out.print(javaMethod.getName());
    out.print("(");

    Class<?>[] types = javaMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      Class<?> type = types[i];

      if (i != 0)
        out.print(", ");

      out.printClass(type);
      out.print(" a" + i);
    }

    out.println(")");
    AspectGeneratorUtil.generateThrows(out, javaMethod.getExceptionTypes());
    out.println();
    out.println("{");
    out.pushDepth();

    if (! void.class.equals(javaMethod.getReturnType())) {
      out.printClass(javaMethod.getReturnType());
      out.println(" result;");
    }

    generateTailCall(out, getFactory().getAspectBeanFactory().getBeanSuper());

    if (! void.class.equals(javaMethod.getReturnType()))
      out.println("return result;");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the call to the implementation bean.
   *
   * @param superVar java code to reference the implementation
   */
  public void generateTailCall(JavaWriter out, String superVar)
    throws IOException
  {
    Method javaMethod = getJavaMethod();
    
    out.println();

    if (! void.class.equals(javaMethod.getReturnType())) {
      out.print("result = ");
    }

    out.print(superVar + "." + javaMethod.getName() + "(");

    Class<?>[] types = javaMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
        out.print(", ");

      out.print(" a" + i);
    }

    out.println(");");

    /*
    // ejb/12b0
    if (! "super".equals(superVar))
      generatePostCall(out);
    */
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

  private Method findInterceptorMethod(Class<?> cl)
  {
    if (cl == null)
      return null;

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class))
        return method;
    }

    return findInterceptorMethod(cl.getSuperclass());
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

  private void generateGetMethod(JavaWriter out,
                                 String className,
                                 String methodName,
                                 Class<?>[] paramTypes)
    throws IOException
  {
    out.print("com.caucho.config.gen.CandiUtil.getMethod(");
    out.print(className);
    out.print(".class, \"" + methodName + "\", new Class[] { ");

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
      int hashCode = _type.hashCode() * 65521;
      
      if (_binding != null)
        return hashCode + _binding.hashCode();
      else
        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
      if (o == this)
        return true;
      else if (! (o instanceof InterceptionBinding))
        return false;

      InterceptionBinding binding = (InterceptionBinding) o;

      if (! _type.equals(binding._type))
        return false;
      
      if (_binding == binding._binding)
        return true;
      
      return _binding != null && _binding.equals(binding._binding);
    }
  }
}
