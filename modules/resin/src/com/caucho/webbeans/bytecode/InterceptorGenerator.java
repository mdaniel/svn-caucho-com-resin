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

package com.caucho.webbeans.bytecode;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import com.caucho.bytecode.*;
import com.caucho.loader.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.component.*;
import com.caucho.vfs.*;

/**
 * interceptor generation
 */
public class InterceptorGenerator {
  private final Class _cl;
  private final Constructor _ctor;

  private Method []_methodList;
  private ArrayList<WbInterceptor> []_interceptorList;
  
  private Class _proxyClass;
  private Constructor _proxyCtor;

  private InterceptorGenerator(Class cl,
			       Constructor ctor,
			       HashMap<Method,ArrayList<WbInterceptor>> map)
  {
    _cl = cl;
    _ctor = ctor;

    _methodList = new Method[map.size()];
    _interceptorList = new ArrayList[map.size()];
    
    int i = 0;
    for (Map.Entry<Method,ArrayList<WbInterceptor>> entry : map.entrySet()) {
      _methodList[i] = entry.getKey();
      _interceptorList[i] = entry.getValue();

      i += 1;
    }
  }

  public static Class gen(Class cl,
			  Constructor ctor,
			  HashMap<Method,ArrayList<WbInterceptor>> map)
  {
    InterceptorGenerator gen = new InterceptorGenerator(cl, ctor, map);

    Class proxyClass = gen.generateProxy();

    return gen.initProxy(proxyClass);
  }
  
  private Class initProxy(Class proxyClass)
  {
    Method []methods = new Method[_methodList.length];
    Method [][]methodChains = new Method[_methodList.length][];
    Object [][]objectChains = new Object[_methodList.length][];

    for (int i = 0; i < _methodList.length; i++) {
      ArrayList<WbInterceptor> chain = _interceptorList[i];
	
      methods[i] = findMethod(proxyClass.getDeclaredMethods(),
			      "__caucho_" + i);

      Method []methodChain = new Method[chain.size()];
      methodChains[i] = methodChain;
      
      Object []objectChain = new Object[chain.size()];
      objectChains[i] = objectChain;

      for (int j = 0; j < chain.size(); j++) {
	methodChain[j] = chain.get(j).getMethod();
	objectChain[j] = chain.get(j).getObject();
      }
    }

    try {
      Method method = proxyClass.getMethod("__caucho_init",
					   new Class[] { Method[].class,
							 Method[][].class,
							 Object[][].class });

      method.invoke(null, methods, methodChains, objectChains);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    return proxyClass;
  }

  private Method findMethod(Method []methods, String name)
  {
    for (Method method : methods) {
      if (method.getName().equals(name)) {
	method.setAccessible(true);
	
	return method;
      }
    }

    throw new IllegalStateException(name + " does not exist");
  }

  private Class generateProxy()
  {
    try {
      JavaClassLoader jLoader = new JavaClassLoader(_cl.getClassLoader());
      
      JavaClass jClass = new JavaClass(jLoader);
      jClass.setAccessFlags(Modifier.PUBLIC);
      ConstantPool cp = jClass.getConstantPool();

      jClass.setWrite(true);
      
      jClass.setMajor(49);
      jClass.setMinor(0);

      String superClassName = _cl.getName().replace('.', '/');
      String thisClassName = superClassName + "$BeanProxy";

      jClass.setSuperClass(superClassName);
      jClass.setThisClass(thisClassName);

      generateConstructor(jClass, superClassName);

      for (int i = 0; i < _methodList.length; i++) {
	createProxyMethod(jClass, _methodList[i], _interceptorList[i], i);
	createTargetMethod(jClass, _methodList[i], i);
      }

      createInitMethod(jClass, _methodList.length);
    
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      WriteStream out = Vfs.openWrite(bos);

      jClass.write(out);
    
      out.close();

      byte []buffer = bos.toByteArray();

      if (true) {
	out = Vfs.lookup("file:/tmp/caucho/qa/temp.class").openWrite();
	out.write(buffer, 0, buffer.length);
	out.close();
      }
      
      String cleanName = thisClassName.replace('/', '.');
      _proxyClass = new ProxyClassLoader().loadClass(cleanName, buffer);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return _proxyClass;
  }

  private void generateConstructor(JavaClass jClass, String superClassName)
  {
    JavaMethod ctor
      = jClass.createMethod("<init>", "()V");
    ctor.setAccessFlags(Modifier.PUBLIC);
      
    CodeWriterAttribute code = ctor.createCodeWriter();
    code.setMaxLocals(1);
    code.setMaxStack(4);

    code.pushObjectVar(0);
    // args
    code.invokespecial(superClassName, "<init>", "()V", 1, 0);
    code.addReturn();
    code.close();
  }

  private void createProxyMethod(JavaClass jClass,
				 Method method,
				 ArrayList<WbInterceptor> interceptors,
				 int i)
  {
    JavaField jField
      = jClass.createField("_method" + i, "Ljava/lang/reflect/Method;");
    jField.setAccessFlags(Modifier.PRIVATE|Modifier.STATIC);
    
    jField
      = jClass.createField("_methodChain" + i, "[Ljava/lang/reflect/Method;");
    jField.setAccessFlags(Modifier.PRIVATE|Modifier.STATIC);
    
    jField
      = jClass.createField("_objectChain" + i, "[Ljava/lang/Object;");
    jField.setAccessFlags(Modifier.PRIVATE|Modifier.STATIC);
      
    String descriptor = createDescriptor(method);
    
    JavaMethod jMethod = jClass.createMethod(method.getName(),
					     descriptor);
    jMethod.setAccessFlags(Modifier.PUBLIC);

    Class []parameterTypes = method.getParameterTypes();

    int local0 = 1 + parameterCount(parameterTypes);
      
    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(5 + local0);
    code.setMaxStack(10 + local0);

    // new InvocationContextImpl
    code.newInstance("com/caucho/webbeans/bytecode/InvocationContextImpl");
    code.dup();
    
    code.pushObjectVar(0); // target
    code.getStatic(jClass.getThisClass(), "_method" + i,
		   "Ljava/lang/reflect/Method;");
    code.getStatic(jClass.getThisClass(), "_methodChain" + i,
		   "[Ljava/lang/reflect/Method;");
    code.getStatic(jClass.getThisClass(), "_objectChain" + i,
		   "[Ljava/lang/Object;");

    marshalInterceptor(code, method);

    code.invokespecial("com/caucho/webbeans/bytecode/InvocationContextImpl",
		       "<init>",
		       "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/reflect/Method;[Ljava/lang/Object;[Ljava/lang/Object;)V", 5, 1);
    
    code.invoke("com/caucho/webbeans/bytecode/InvocationContextImpl",
		"proceed",
		"()Ljava/lang/Object;", 1, 1);

    unmarshalInterceptor(code, method);
    
    code.close();
  }

  private void createTargetMethod(JavaClass jClass,
				  Method method,
				  int i)
  {
    String descriptor = createDescriptor(method);
    
    JavaMethod jMethod = jClass.createMethod("__caucho_" + i,
					     descriptor);
    jMethod.setAccessFlags(Modifier.PRIVATE);

    Class []parameterTypes = method.getParameterTypes();
      
    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(2 + 2 * parameterTypes.length);
    code.setMaxStack(2 + 2 * parameterTypes.length);
    
    marshal(code, method);
    
    code.invokespecial(method.getDeclaringClass().getName().replace('.', '/'),
		       method.getName(),
		       descriptor, 0, 0);

    unmarshal(code, method);
    
    code.close();
  }

  private void createInitMethod(JavaClass jClass, int size)
  {
    JavaMethod jMethod
      = jClass.createMethod("__caucho_init",
			    "([Ljava/lang/reflect/Method;[[Ljava/lang/reflect/Method;[[Ljava/lang/Object;)V");
    
    jMethod.setAccessFlags(Modifier.PUBLIC|Modifier.STATIC);
      
    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(5);
    code.setMaxStack(5);

    for (int i = 0; i < size; i++) {
      code.pushObjectVar(0); // methods
      code.pushInt(i);
      code.getArrayObject();
      code.putStatic(jClass.getThisClass(), "_method" + i,
		     "Ljava/lang/reflect/Method;");
      
      code.pushObjectVar(1); // methodChain
      code.pushInt(i);
      code.getArrayObject();
      code.putStatic(jClass.getThisClass(), "_methodChain" + i,
		     "[Ljava/lang/reflect/Method;");
      
      code.pushObjectVar(2); // objectChain
      code.pushInt(i);
      code.getArrayObject();
      code.putStatic(jClass.getThisClass(), "_objectChain" + i,
		     "[Ljava/lang/Object;");
    }

    code.addReturn();
    
    code.close();
  }

  private void marshalInterceptor(CodeWriterAttribute code, Method method)
  {
    Class []param = method.getParameterTypes();
    
    code.pushInt(param.length);
    code.newObjectArray("java/lang/Object");
    
    int stack = 1;
    int index = 1;
    
    for (int i = 0; i < param.length; i++) {
      Class type = param[i];
      
      if (boolean.class.equals(type)
	  || byte.class.equals(type)
	  || short.class.equals(type)
	  || int.class.equals(type)) {
	code.dup();
	code.pushInt(i);
	code.newInstance(_boxClass.get(type));
	code.dup();
	code.pushIntVar(index);
	code.invokespecial(_boxClass.get(type), "<init>",
			   "(" + _prim.get(type) + ")V", 2, 0);
	code.setArrayObject();
	index += 1;
	stack += 1;
      }
      else if (long.class.equals(type)) {
	code.dup();
	code.pushInt(i);
	code.newInstance("java/lang/Long");
	code.dup();
	code.pushLongVar(index);
	code.invokespecial("java/lang/Long", "<init>", "(J)V", 3, 0);
	code.setArrayObject();
	index += 2;
	stack += 2;
      }
      else if (float.class.equals(type)) {
	code.dup();
	code.pushInt(i);
	code.newInstance("java/lang/Float");
	code.dup();
	code.pushFloatVar(index);
	code.invokespecial("java/lang/Float", "<init>", "(F)V", 2, 0);
	code.setArrayObject();
	index += 1;
	stack += 1;
      }
      else if (double.class.equals(type)) {
	code.dup();
	code.pushInt(i);
	code.newInstance("java/lang/Double");
	code.dup();
	code.pushDoubleVar(index);
	code.invokespecial("java/lang/Double", "<init>", "(D)V", 3, 0);
	code.setArrayObject();
	index += 2;
	stack += 2;
      }
      else {
	code.dup();
	code.pushInt(i);
	code.pushObjectVar(index);
	code.setArrayObject();
	index += 1;
	stack += 1;
      }
    }
  }

  private void unmarshalInterceptor(CodeWriterAttribute code, Method method)
  {
    Class retType = method.getReturnType();
    
    if (boolean.class.equals(retType)) {
      code.cast("java/lang/Boolean");
      code.invoke("java/lang/Boolean", "booleanValue", "()Z", 1, 1);
      code.addIntReturn();
    }
    else if (byte.class.equals(retType)) {
      code.cast("java/lang/Byte");
      code.invoke("java/lang/Byte", "byteValue", "()B", 1, 1);
      code.addIntReturn();
    }
    else if (short.class.equals(retType)) {
      code.cast("java/lang/Short");
      code.invoke("java/lang/Short", "shortValue", "()S", 1, 1);
      code.addIntReturn();
    }
    else if (int.class.equals(retType)) {
      code.cast("java/lang/Integer");
      code.invoke("java/lang/Integer", "intValue", "()I", 1, 1);
      code.addIntReturn();
    }
    else if (long.class.equals(retType)) {
      code.cast("java/lang/Long");
      code.invoke("java/lang/Long", "longValue", "()J", 1, 2);
      code.addLongReturn();
    }
    else if (float.class.equals(retType)) {
      code.cast("java/lang/Float");
      code.invoke("java/lang/Float", "floatValue", "()F", 1, 1);
      code.addFloatReturn();
    }
    else if (double.class.equals(retType)) {
      code.cast("java/lang/Double");
      code.invoke("java/lang/Double", "doubleValue", "()D", 1, 2);
      code.addDoubleReturn();
    }
    else if (void.class.equals(retType)) {
      code.addReturn();
    }
    else {
      code.cast(retType.getName().replace('.', '/'));
      code.addObjectReturn();
    }
  }

  private void marshal(CodeWriterAttribute code, Method method)
  {
    Class []param = method.getParameterTypes();
    
    code.pushObjectVar(0);
    
    int stack = 1;
    int index = 1;
    
    for (int i = 0; i < param.length; i++) {
      Class type = param[i];
      
      if (boolean.class.equals(type)
	  || byte.class.equals(type)
	  || short.class.equals(type)
	  || int.class.equals(type)) {
	code.pushIntVar(index);
	index += 1;
	stack += 1;
      }
      else if (long.class.equals(type)) {
	code.pushLongVar(index);
	index += 2;
	stack += 2;
      }
      else if (float.class.equals(type)) {
	code.pushFloatVar(index);
	index += 1;
	stack += 1;
      }
      else if (double.class.equals(type)) {
	code.pushDoubleVar(index);
	index += 2;
	stack += 2;
      }
      else {
	code.pushObjectVar(index);
	index += 1;
	stack += 1;
      }
    }
  }

  private void unmarshal(CodeWriterAttribute code, Method method)
  {
    Class retType = method.getReturnType();
    
    if (boolean.class.equals(retType)
	|| byte.class.equals(retType)
	|| short.class.equals(retType)
	|| int.class.equals(retType)) {
      code.addIntReturn();
    }
    else if (long.class.equals(retType)) {
      code.addLongReturn();
    }
    else if (float.class.equals(retType)) {
      code.addFloatReturn();
    }
    else if (double.class.equals(retType)) {
      code.addDoubleReturn();
    }
    else if (void.class.equals(retType)) {
      code.addReturn();
    }
    else {
      code.addObjectReturn();
    }
  }

  private int parameterCount(Class []parameters)
  {
    int count = 0;

    for (Class param : parameters) {
      if (long.class.equals(param) || double.class.equals(param))
	count += 2;
      else
	count += 1;
    }

    return count;
  }

  private String createDescriptor(Method method)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("(");
    
    for (Class param : method.getParameterTypes()) {
      sb.append(createDescriptor(param));
    }
    
    sb.append(")");
    sb.append(createDescriptor(method.getReturnType()));

    return sb.toString();
  }

  private String createDescriptor(Class cl)
  {
    if (cl.isArray())
      return "[" + createDescriptor(cl.getComponentType());

    String primValue = _prim.get(cl);

    if (primValue != null)
      return primValue;

    return "L" + cl.getName().replace('.', '/') + ";";
  }

  private static HashMap<Class,String> _prim = new HashMap<Class,String>();
  private static HashMap<Class,String> _boxClass = new HashMap<Class,String>();

  static {
    _prim.put(boolean.class, "Z");
    _prim.put(byte.class, "B");
    _prim.put(char.class, "C");
    _prim.put(short.class, "S");
    _prim.put(int.class, "I");
    _prim.put(long.class, "J");
    _prim.put(float.class, "F");
    _prim.put(double.class, "D");
    _prim.put(void.class, "V");
    
    _boxClass.put(boolean.class, "java/lang/Boolean");
    _boxClass.put(byte.class, "java/lang/Byte");
    _boxClass.put(char.class, "java/lang/Character");
    _boxClass.put(short.class, "java/lang/Short");
    _boxClass.put(int.class, "java/lang/Integer");
    _boxClass.put(long.class, "java/lang/Long");
    _boxClass.put(float.class, "java/lang/Float");
    _boxClass.put(double.class, "java/lang/Double");
  }
}
