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

package com.caucho.config.bytecode;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.*;

import javax.enterprise.inject.spi.Bean;

import com.caucho.bytecode.*;
import com.caucho.config.*;
import com.caucho.inject.Module;
import com.caucho.loader.*;
import com.caucho.config.cfg.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Scope adapting
 */
@Module
public class ScopeAdapter {
  private static final L10N L = new L10N(ScopeAdapter.class);
  private static final Logger log 
    = Logger.getLogger(ScopeAdapter.class.getName());

  private final Class<?> _cl;

  private Class<?> _proxyClass;
  private Constructor<?> _proxyCtor;

  private ScopeAdapter(Class<?> cl)
  {
    _cl = cl;

    generateProxy(cl);
  }

  public static ScopeAdapter create(Class<?> cl)
  {
    ScopeAdapter adapter = new ScopeAdapter(cl);

    return adapter;
  }

  public Object wrap(InjectManager manager, Bean<?> comp)
  {
    try {
      Object v = _proxyCtor.newInstance(manager, comp);
      return v;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
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
      
      zeroCtor.setAccessible(true);
      
      String typeClassName = cl.getName().replace('.', '/');
      
      String thisClassName = typeClassName + "__ResinScopeProxy";
      String cleanName = thisClassName.replace('/', '.');
      
      DynamicClassLoader loader = (DynamicClassLoader) Thread.currentThread().getContextClassLoader();
      
      try {
        _proxyClass = Class.forName(cleanName, false, loader);
      } catch (ClassNotFoundException e) {
        log.log(Level.FINEST, e.toString(), e);
      }
      
      if (_proxyClass == null) {
        JavaClassLoader jLoader = new JavaClassLoader(cl.getClassLoader());

        JavaClass jClass = new JavaClass(jLoader);
        jClass.setAccessFlags(Modifier.PUBLIC);

        jClass.setWrite(true);

        jClass.setMajor(49);
        jClass.setMinor(0);

        String superClassName;

        if (!cl.isInterface())
          superClassName = typeClassName;
        else
          superClassName = "java/lang/Object";

        jClass.setSuperClass(superClassName);
        jClass.setThisClass(thisClassName);

        if (cl.isInterface())
          jClass.addInterface(typeClassName);

        JavaField managerField =
          jClass.createField("_manager",
                             "Lcom/caucho/config/inject/InjectManager;");
        managerField.setAccessFlags(Modifier.PRIVATE);

        JavaField beanField =
          jClass.createField("_bean", "Ljavax/enterprise/inject/spi/Bean;");
        beanField.setAccessFlags(Modifier.PRIVATE);

        JavaMethod ctor =
          jClass.createMethod("<init>",
                              "(Lcom/caucho/config/inject/InjectManager;"
                                  + "Ljavax/enterprise/inject/spi/Bean;)V");
        ctor.setAccessFlags(Modifier.PUBLIC);

        CodeWriterAttribute code = ctor.createCodeWriter();
        code.setMaxLocals(3);
        code.setMaxStack(4);

        code.pushObjectVar(0);
        code.invokespecial(superClassName, "<init>", "()V", 1, 0);
        code.pushObjectVar(0);
        code.pushObjectVar(1);
        code.putField(thisClassName, managerField.getName(), managerField
            .getDescriptor());
        code.pushObjectVar(0);
        code.pushObjectVar(2);
        code.putField(thisClassName, beanField.getName(), beanField
            .getDescriptor());
        code.addReturn();
        code.close();

        for (Method method : _cl.getMethods()) {
          if (Modifier.isStatic(method.getModifiers()))
            continue;
          if (Modifier.isFinal(method.getModifiers()))
            continue;

          createProxyMethod(jClass, method, cl.isInterface());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WriteStream out = Vfs.openWrite(bos);

        jClass.write(out);

        out.close();

        byte[] buffer = bos.toByteArray();

        /*
         * try { out = Vfs.lookup("file:/tmp/caucho/qa/temp.class").openWrite();
         * out.write(buffer, 0, buffer.length); out.close(); } catch
         * (IOException e) { }
         */

        // ioc/0517
        _proxyClass = loader.loadClass(cleanName, buffer);
      }
      
      _proxyCtor = _proxyClass.getConstructors()[0];
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void createProxyMethod(JavaClass jClass,
                                 Method method,
                                 boolean isInterface)
  {
    String descriptor = createDescriptor(method);

    JavaMethod jMethod = jClass.createMethod(method.getName(),
                                             descriptor);
    jMethod.setAccessFlags(Modifier.PUBLIC);

    Class<?> []parameterTypes = method.getParameterTypes();

    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(1 + 2 * parameterTypes.length);
    code.setMaxStack(3 + 2 * parameterTypes.length);

    code.pushObjectVar(0);
    code.getField(jClass.getThisClass(), "_manager",
                  "Lcom/caucho/config/inject/InjectManager;");

    code.pushObjectVar(0);
    code.getField(jClass.getThisClass(), "_bean",
                  "Ljavax/enterprise/inject/spi/Bean;");

    code.invoke("com/caucho/config/inject/InjectManager",
                "getInstance",
                "(Ljavax/enterprise/inject/spi/Bean;)Ljava/lang/Object;",
                1, 1);

    code.cast(method.getDeclaringClass().getName().replace('.', '/'));

    int stack = 1;
    int index = 1;
    for (Class<?> type : parameterTypes) {
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

    if (isInterface) {
      code.invokeInterface(method.getDeclaringClass().getName().replace('.', '/'),
                           method.getName(),
                           createDescriptor(method),
                           stack, 1);
    }
    else {
      code.invoke(method.getDeclaringClass().getName().replace('.', '/'),
                  method.getName(),
                  createDescriptor(method),
                  stack, 1);
    }

    Class<?> retType = method.getReturnType();

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

    code.close();
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
