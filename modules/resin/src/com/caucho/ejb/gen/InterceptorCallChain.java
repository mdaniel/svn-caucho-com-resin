/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.security.*;
import javax.ejb.*;
import javax.interceptor.*;
import javax.webbeans.*;

/**
 * Represents the interception
 */
public class InterceptorCallChain extends AbstractCallChain {
  private static final L10N L = new L10N(InterceptorCallChain.class);

  private View _view;
  private BusinessMethodGenerator _next;

  private String _uniqueName;
  private Method _implMethod;
  
  private ArrayList<Class> _defaultInterceptors = new ArrayList<Class>();
  private ArrayList<Class> _classInterceptors = new ArrayList<Class>();
  private ArrayList<Class> _methodInterceptors = new ArrayList<Class>();

  private boolean _isExcludeDefaultInterceptors;
  private boolean _isExcludeClassInterceptors;
  
  private ArrayList<Class> _interceptors = new ArrayList<Class>();

  // map from the interceptor class to the local variable for the interceptor
  private HashMap<Class,String> _interceptorVarMap
    = new HashMap<Class,String>();
  
  // interceptors we're responsible for initializing
  private ArrayList<Class> _ownInterceptors = new ArrayList<Class>();

  public InterceptorCallChain(BusinessMethodGenerator next,
			      View view)
  {
    super(next);
    
    _next = next;
    _view = view;
  }
  
  /**
   * Returns true if the business method has any active XA annotation.
   */
  public boolean isEnhanced()
  {
    return (_defaultInterceptors.size() > 0
	    || _classInterceptors.size() > 0
	    || _methodInterceptors.size() > 0
	    || getAroundInvokeMethod() != null);
  }

  public ArrayList<Class> getInterceptors()
  {
    return _interceptors;
  }

  public Method getAroundInvokeMethod()
  {
    return _view.getAroundInvokeMethod();
  }

  /**
   * Introspects the @Interceptors annotation on the method
   * and the class.
   */
  public void introspect(Method apiMethod, Method implMethod)
  {
    if (implMethod == null)
      return;
    
    Class apiClass = apiMethod.getDeclaringClass();
    
    Class implClass = implMethod.getDeclaringClass();

    _implMethod = implMethod;

    Interceptors iAnn;
    
    iAnn = (Interceptors) apiClass.getAnnotation(Interceptors.class);

    if (iAnn != null) {
      for (Class iClass : iAnn.value())
	_classInterceptors.add(iClass);
    }

    if (implClass != null) {
      iAnn = (Interceptors) implClass.getAnnotation(Interceptors.class);

      if (apiMethod != implMethod && iAnn != null) {
	for (Class iClass : iAnn.value())
	  _classInterceptors.add(iClass);
      }
    }
    
    iAnn = (Interceptors) apiMethod.getAnnotation(Interceptors.class);

    if (iAnn != null) {
      for (Class iClass : iAnn.value())
	_methodInterceptors.add(iClass);
    }

    if (implMethod != null) {
      iAnn = (Interceptors) implMethod.getAnnotation(Interceptors.class);

      if (apiMethod != implMethod && iAnn != null) {
	for (Class iClass : iAnn.value())
	  _methodInterceptors.add(iClass);
      }
    }

    if (apiMethod.isAnnotationPresent(ExcludeClassInterceptors.class))
      _isExcludeClassInterceptors = true;

    if (implMethod.isAnnotationPresent(ExcludeClassInterceptors.class))
      _isExcludeClassInterceptors = true;

    if (apiMethod.isAnnotationPresent(ExcludeDefaultInterceptors.class))
      _isExcludeDefaultInterceptors = true;

    if (implMethod.isAnnotationPresent(ExcludeDefaultInterceptors.class))
      _isExcludeDefaultInterceptors = true;

    // webbeans annotations
    WebBeansContainer webBeans = WebBeansContainer.create();
    
    ArrayList<Annotation> interceptorTypes = new ArrayList<Annotation>();
    for (Annotation ann : implMethod.getAnnotations()) {
      Class annType = ann.annotationType();
      
      if (annType.isAnnotationPresent(InterceptorBindingType.class))
	interceptorTypes.add(ann);
    }

    if (interceptorTypes.size() > 0) {
      ArrayList<Class> interceptors
	= webBeans.findInterceptors(interceptorTypes);
      
      if (interceptors != null)
	_methodInterceptors.addAll(interceptors);
    }
  }

  @Override
  public void generatePrologue(JavaWriter out, HashMap map)
    throws IOException
  {
    if (! isEnhanced()) {
      _next.generatePrologue(out, map);
      return;
    }

    if (! _isExcludeDefaultInterceptors)
      _interceptors.addAll(_defaultInterceptors);
    if (! _isExcludeClassInterceptors)
      _interceptors.addAll(_classInterceptors);
    
    _interceptors.addAll(_methodInterceptors);

    if (_interceptors.size() == 0 && getAroundInvokeMethod() == null)
      return;
    
    _uniqueName = "_v" + out.generateId();
    
    out.println();
    out.println("private static java.lang.reflect.Method " + _uniqueName + "_method;");
    out.println("private static java.lang.reflect.Method " + _uniqueName + "_implMethod;");

    boolean isAroundInvokePrologue = false;
    if (getAroundInvokeMethod() != null
	&& map.get("ejb.around-invoke") == null) {
      isAroundInvokePrologue = true;
      map.put("ejb.around-invoke", "_caucho_aroundInvokeMethod");
      
      out.println("private static java.lang.reflect.Method __caucho_aroundInvokeMethod;");
    }
    
    out.println("private static java.lang.reflect.Method []" + _uniqueName + "_methodChain;");
    out.println("private transient Object []" + _uniqueName + "_objectChain;");

    Class cl = _implMethod.getDeclaringClass();
    
    out.println();
    out.println("static {");
    out.pushDepth();
    
    out.println("try {");
    out.pushDepth();
    
    out.print(_uniqueName + "_method = ");
    generateGetMethod(out,
		      _implMethod.getDeclaringClass().getName(),
		      _implMethod.getName(),
		      _implMethod.getParameterTypes());
    out.println(";");
    out.println(_uniqueName + "_method.setAccessible(true);");
    
    out.print(_uniqueName + "_implMethod = ");
    generateGetMethod(out,
		      _next.getView().getViewClassName(),
		      "__caucho_" + _implMethod.getName(),
		      _implMethod.getParameterTypes());
    out.println(";");
    out.println(_uniqueName + "_implMethod.setAccessible(true);");

    if (isAroundInvokePrologue) {
      Method aroundInvoke = getAroundInvokeMethod();
      
      out.print("__caucho_aroundInvokeMethod = ");
      generateGetMethod(out,
			aroundInvoke.getDeclaringClass().getName(),
			aroundInvoke.getName(),
			aroundInvoke.getParameterTypes());
      out.println(";");
      out.println("__caucho_aroundInvokeMethod.setAccessible(true);");
    }

    generateMethodChain(out);
    
    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    out.popDepth();
    out.println("}");

    for (Class iClass : _interceptors) {
      String var = (String) map.get("interceptor-" + iClass.getName());
      if (var == null) {
	var = "__caucho_i" + out.generateId();

	out.println();
	out.print("private static ");
	out.printClass(ComponentFactory.class);
	out.println(" " + var + "_f;");
	
	out.print("private transient ");
	out.printClass(iClass);
	out.println(" " + var + ";");

	map.put("interceptor-" + iClass.getName(), var);

	_ownInterceptors.add(iClass);
      }

      _interceptorVarMap.put(iClass, var);
    }
    
    _next.generatePrologue(out, map);
  }

  @Override
  public void generateConstructor(JavaWriter out, HashMap map)
    throws IOException
  {
    for (Class iClass : _ownInterceptors) {
      String var = _interceptorVarMap.get(iClass);

      out.println("if (" + var + "_f == null)");
      out.println("  " + var + "_f = com.caucho.webbeans.manager.WebBeansContainer.create().createTransient(" + iClass.getName() + ".class);");

      out.print(var + " = (");
      out.printClass(iClass);
      out.println(")" + var + "_f.get();");
    }
    
    _next.generateConstructor(out, map);
  }

  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
    if (_interceptors.size() == 0 && getAroundInvokeMethod() == null) {
      _next.generateCall(out);
      return;
    }

    out.println("try {");
    out.pushDepth();

    out.println("if (" + _uniqueName + "_objectChain == null) {");
    out.pushDepth();
    generateObjectChain(out);
    out.popDepth();
    out.println("}");

    if (! void.class.equals(_implMethod.getReturnType())) {
      out.printClass(_implMethod.getReturnType());
      out.println(" result;");
    }
      
    if (! void.class.equals(_implMethod.getReturnType())) {
      out.print("result = (");
      printCastClass(out, _implMethod.getReturnType());
      out.print(") ");
    }
    
    out.print("new com.caucho.ejb3.gen.InvocationContextImpl(");
    out.print("this, ");
    out.print(_uniqueName + "_method, ");
    out.print(_uniqueName + "_implMethod, ");
    out.print(_uniqueName + "_methodChain, ");
    out.print(_uniqueName + "_objectChain, ");
    out.print("new Object[] { ");
    for (int i = 0; i < _implMethod.getParameterTypes().length; i++) {
      out.print("a" + i + ", ");
    }
    out.println("}).proceed();");
    
    if (! void.class.equals(_implMethod.getReturnType())) {
      out.println("return result;");
    }
    
    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");

    for (Class cl : _implMethod.getExceptionTypes()) {
      if (! RuntimeException.class.isAssignableFrom(cl)) {
	out.println("} catch (" + cl.getName() + " e) {");
	out.println("  throw e;");
      }
    }
    
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
  }

  protected Method findInterceptorMethod(Class cl)
  {
    if (cl == null)
      return null;

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class))
	return method;
    }

    return findInterceptorMethod(cl.getSuperclass());
  }

  protected void generateMethodChain(JavaWriter out)
    throws IOException
  {
    out.println(_uniqueName + "_methodChain = new java.lang.reflect.Method[] {");
    out.pushDepth();

    for (Class iClass : _interceptors) {
      Method method = findInterceptorMethod(iClass);

      if (method == null)
	throw new IllegalStateException(L.l("Can't find @AroundInvoke in '{0}'",
					    iClass.getName()));
      
      generateGetMethod(out, method);
      out.println(", ");
    }

    if (getAroundInvokeMethod() != null) {
      out.println("__caucho_aroundInvokeMethod, ");
    }
    
    out.popDepth();
    out.println("};");
  }

  protected void generateObjectChain(JavaWriter out)
    throws IOException
  {
    out.println(_uniqueName + "_objectChain = new Object[] {");

    for (Class iClass : _interceptors) {
      out.print(_interceptorVarMap.get(iClass) + ", ");
    }

    if (getAroundInvokeMethod() != null) {
      _next.generateThis(out);
      out.print(", ");
    }
    
    out.println("};");
  }
  
  protected void generateGetMethod(JavaWriter out, Method method)
    throws IOException
  {
    generateGetMethod(out,
		      method.getDeclaringClass().getName(),
		      method.getName(),
		      method.getParameterTypes());
  }
  
  protected void generateGetMethod(JavaWriter out,
				   String className,
				   String methodName,
				   Class []paramTypes)
    throws IOException
  {
    out.print("com.caucho.ejb.util.EjbUtil.getMethod(");
    out.print(className + ".class");
    out.print(", \"" + methodName + "\", new Class[] { ");
    
    for (Class type : paramTypes) {
      out.printClass(type);
      out.print(".class, ");
    }
    out.print("})");
  }

  protected void printCastClass(JavaWriter out, Class type)
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
}
