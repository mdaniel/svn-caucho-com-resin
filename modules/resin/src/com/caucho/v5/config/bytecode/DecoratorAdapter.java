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

package com.caucho.v5.config.bytecode;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.bytecode.JavaClass;
import com.caucho.v5.bytecode.JavaClassLoader;
import com.caucho.v5.bytecode.JavaMethod;
import com.caucho.v5.bytecode.attr.CodeWriterAttribute;
import com.caucho.v5.config.AbstractClassPadding;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.inject.Module;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.ProxyClassLoader;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.vfs.WriteStream;

/**
 * Extends a decorator, implementing the missing methods.
 */
@Module
public class DecoratorAdapter<T> {
  private static final L10N L = new L10N(DecoratorAdapter.class);
  private static final Logger log 
    = Logger.getLogger(DecoratorAdapter.class.getName());

  private final Class<T> _cl;

  private Class<T> _proxyClass;
  private Constructor<?> _proxyCtor;
  
  private DecoratorAdapter(Class<T> cl)
  {
    _cl = cl;

    generateProxy(_cl);
    
    // _proxyCtor = _proxyClass.getConstructors()[0];
  }
  
  public static <T> Class<T> create(Class<T> cl)
  {
    if (! Modifier.isAbstract(cl.getModifiers()))
      return cl;
    
    DecoratorAdapter<T> adapter = new DecoratorAdapter<T>(cl);
    
    return adapter.getProxyClass();
  }
  
  public Class<T> getProxyClass()
  {
    return _proxyClass;
  }

  private void generateProxy(Class<?> cl)
  {
    try {
      Constructor<?> zeroCtor = null;

      for (Constructor<?> ctorItem : cl.getDeclaredConstructors()) {
        if (ctorItem.getParameterTypes().length == 0) {
          zeroCtor = ctorItem;
          break;
        }
      }

      if (zeroCtor == null && ! cl.isInterface()) {
        throw new ConfigException(L.l("'{0}' does not have a zero-arg public or protected constructor.  Scope adapter components need a zero-arg constructor, e.g. @RequestScoped stored in @ApplicationScoped.",
                                      cl.getName()));
      }

      if (zeroCtor != null)
        zeroCtor.setAccessible(true);
      
      String typeClassName = cl.getName().replace('.', '/');
      
      String thisClassName = typeClassName + "__CauchoDecoratorAdapter";
      
      if (thisClassName.startsWith("java"))
        thisClassName = "cdi/" + thisClassName;
      
      String cleanName = thisClassName.replace('/', '.');
      
      boolean isPackagePrivate = false;
      
      DynamicClassLoader loader;
      
      if (! Modifier.isPublic(cl.getModifiers()) 
          && ! Modifier.isProtected(cl.getModifiers())) {
        isPackagePrivate = true;
      }

      if (isPackagePrivate)
        loader = (DynamicClassLoader) cl.getClassLoader();
      else
        loader = (DynamicClassLoader) Thread.currentThread().getContextClassLoader();
      
      try {
        _proxyClass = (Class<T>) Class.forName(cleanName, false, loader);
      } catch (ClassNotFoundException e) {
        log.log(Level.FINEST, e.toString(), e);
      }
      
      if (_proxyClass != null)
        return;
      
      JavaClassLoader jLoader = new JavaClassLoader(cl.getClassLoader());

      JavaClass jClass = new JavaClass(jLoader);
      jClass.setAccessFlags(Modifier.PUBLIC);

      jClass.setWrite(true);

      jClass.setMajor(49);
      jClass.setMinor(0);

      String superClassName;

      if (! cl.isInterface())
        superClassName = typeClassName;
      else
        superClassName = "java/lang/Object";

      jClass.setSuperClass(superClassName);
      jClass.setThisClass(thisClassName);
      jClass.addInterface(AbstractClassPadding.class.getName().replace('.', '/'));

      JavaMethod ctor =
        jClass.createMethod("<init>", "()V");
      ctor.setAccessFlags(Modifier.PUBLIC);

      CodeWriterAttribute code = ctor.createCodeWriter();
      code.setMaxLocals(3);
      code.setMaxStack(4);

      code.pushObjectVar(0);
      code.invokespecial(superClassName, "<init>", "()V", 1, 0);
      code.addReturn();
      code.close();

      for (Method method : _cl.getMethods()) {
        if (Modifier.isStatic(method.getModifiers()))
          continue;
        if (Modifier.isFinal(method.getModifiers()))
          continue;
        if (! Modifier.isAbstract(method.getModifiers()))
          continue;

        createStubMethod(jClass, method);
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      WriteStream out = Vfs.openWrite(bos);

      jClass.write(out);

      out.close();

      byte[] buffer = bos.toByteArray();


/*
     try { out = Vfs.lookup("file:/tmp/caucho/qa/temp.class").openWrite();
      out.write(buffer, 0, buffer.length); out.close(); } catch
      (java.io.IOException e) { }
*/


      if (isPackagePrivate) {
        // ioc/0517
        _proxyClass = (Class<T>) loader.loadClass(cleanName, buffer);
      }
      else {
        ProxyClassLoader proxyLoader = new ProxyClassLoader(loader);

        _proxyClass = (Class<T>) proxyLoader.loadClass(cleanName, buffer);
      }
      
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void createStubMethod(JavaClass jClass,
                                Method method)
  {
    if (! Modifier.isAbstract(method.getModifiers()))
      return;
    
    String descriptor = createDescriptor(method);

    JavaMethod jMethod = jClass.createMethod(method.getName(),
                                             descriptor);
    jMethod.setAccessFlags(Modifier.PUBLIC);

    Class<?> []parameterTypes = method.getParameterTypes();

    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(1 + 2 * parameterTypes.length);
    code.setMaxStack(3 + 2 * parameterTypes.length);
    
    String exnClass = UnsupportedOperationException.class.getName();
    exnClass = exnClass.replace('.', '/');
    
    code.newInstance(exnClass);
    code.dup();
    
    code.invokespecial(exnClass, "<init>", "()V", 1, 1);

    code.addThrow();

    code.close();
  }

  private String createDescriptor(Method method)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("(");

    for (Class<?> param : method.getParameterTypes()) {
      sb.append(createDescriptor(param));
    }

    sb.append(")");
    sb.append(createDescriptor(method.getReturnType()));

    return sb.toString();
  }

  private String createDescriptor(Class<?> cl)
  {
    if (cl.isArray())
      return "[" + createDescriptor(cl.getComponentType());

    String primValue = _prim.get(cl);

    if (primValue != null)
      return primValue;

    return "L" + cl.getName().replace('.', '/') + ";";
  }

  private static HashMap<Class<?>,String> _prim
    = new HashMap<Class<?>,String>();

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
  }

}
