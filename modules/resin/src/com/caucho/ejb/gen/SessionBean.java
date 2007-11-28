/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.config.types.EnvEntry;
import com.caucho.config.types.InjectionTarget;
import com.caucho.config.types.ResourceRef;
import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.ClassComponent;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.io.IOException;

/**
 * Generates the skeleton for a session bean.
 */
public class SessionBean extends ClassComponent {
  private static final L10N L = new L10N(SessionBean.class);

  protected EjbSessionBean _bean;
  private ApiClass _ejbClass;
  protected String _contextClassName;

  public SessionBean(EjbSessionBean bean,
                     ApiClass ejbClass,
                     String contextClassName)
  {
    _bean = bean;
    _ejbClass = ejbClass;
    _contextClassName = contextClassName;
  }

  public boolean isStateless()
  {
    return false;
  }

  public void generate(JavaWriter out)
    throws IOException
  {
    generateContext(out);

    if (_bean.getLocalHome() != null
	&& EJBLocalHome.class.isAssignableFrom(_bean.getLocalHome().getJavaClass()))
      generateNewInstance(out, "21");
    else
      generateNewInstance(out, "");

    if (_bean.isEJB21())
      generateNewRemoteInstance(out, "21");

    if (_bean.isEJB30())
      generateNewRemoteInstance(out, "");

    generateBean(out);
  }

  protected void generateContext(JavaWriter out)
    throws IOException
  {
    String shortContextName = _contextClassName;
    int p = shortContextName.lastIndexOf('.');

    if (p > 0)
      shortContextName = shortContextName.substring(p + 1);

    out.println("protected static final java.util.logging.Logger __caucho_log = java.util.logging.Logger.getLogger(\"" + _contextClassName + "\");");
    out.println("protected static final boolean __caucho_isFiner = __caucho_log.isLoggable(java.util.logging.Level.FINER);");
    out.println();
    out.println("com.caucho.ejb.xa.EjbTransactionManager _xaManager;");
    out.println("private Bean _freeBean;");
    out.println();
    out.println("public " + shortContextName + "(com.caucho.ejb.session.SessionServer server)");
    out.println("{");
    out.println("  super(server);");
    out.println("  _xaManager = server.getTransactionManager();");
    out.println("}");

    out.println();
    out.println("Bean _ejb_begin(com.caucho.ejb.xa.TransactionContext trans)");
    out.println("  throws javax.ejb.EJBException");
    out.println("{");
    out.pushDepth();

    out.println("Bean bean;");
    out.println("synchronized (this) {");
    out.println("  bean = _freeBean;");
    out.println("  if (bean != null) {");
    out.println("    _freeBean = null;");
    if (SessionSynchronization.class.isAssignableFrom(_ejbClass.getJavaClass()))
      out.println("    trans.addSession(bean);");
    out.println("    return bean;");
    out.println("  }");
    out.println("}");
    out.println();

    out.println("throw new EJBException(\"session bean is not reentrant\");");
    out.popDepth();
    out.println("}");

    out.println();
    out.println("void _ejb_free(Bean bean)");
    out.println("  throws javax.ejb.EJBException");
    out.println("{");
    out.pushDepth();
    out.println("if (bean == null)");
    out.println("  return;");
    out.println();
    out.println("synchronized (this) {");
    out.println("  if (_freeBean == null) {");
    out.println("    _freeBean = bean;");
    out.println("    return;");
    out.println("  }");
    out.println("}");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void destroy()");
    out.println("{");
    out.pushDepth();
    out.println("Bean ptr;");
    out.println("synchronized (this) {");
    out.println("  ptr = _freeBean;");
    out.println("  _freeBean = null;");
    out.println("}");

    if (hasMethod("ejbRemove", new Class[0])) {
      out.println();
      out.println("try {");
      out.println("  if (ptr != null)");
      // ejb/0fe0: ejbRemove() can be private, out.println("    ptr.ejbRemove();");
      out.println("    invokeMethod(ptr, \"ejbRemove\", new Class[] {}, new Object[] {});");
      out.println("} catch (Throwable e) {");
      out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
      out.println("}");
    }
    out.popDepth();
    out.println("}");

    generateInvokeMethod(out);
  }

  protected void generateNewInstance(JavaWriter out, String suffix)
    throws IOException
  {
    // ejb/0g27
    if (_bean.getLocalHome() == null && _bean.getLocalList().size() == 0)
      return;

    if (! isStateless()) {
      out.println();
      out.print("protected Object _caucho_newInstance" + suffix);
      out.println("(com.caucho.webbeans.context.DependentScope scope)");
      out.println("{");
      out.pushDepth();

      out.println(_contextClassName + " cxt = new " + _contextClassName + "(_server);");

      // XXX TCK: bb/session/stateful/cm/allowed/afterBeginSetRollbackOnlyTest (infinite recursion issue)

      if (isStateless())
        out.println("Bean bean = new Bean(cxt);");
      else
        out.println("Bean bean = new Bean(cxt, scope);");

      out.println("cxt._ejb_free(bean);");

      out.println();

      out.println();

      out.println("return cxt.createLocalObject" + suffix + "();");

      out.popDepth();
      out.println("}");
    }

    out.println();
    out.println("protected Object _caucho_newInstance" + suffix + "()");
    out.println("{");
    out.pushDepth();

    out.println(_contextClassName + " cxt = new " + _contextClassName + "(_server);");

    // XXX TCK: bb/session/stateful/cm/allowed/afterBeginSetRollbackOnlyTest (infinite recursion issue)

    if (isStateless())
      out.println("Bean bean = new Bean(cxt);");
    else
      out.println("Bean bean = new Bean(cxt, null);");

    out.println("cxt._ejb_free(bean);");

    out.println();

    out.println("return cxt.createLocalObject" + suffix + "();");

    out.popDepth();
    out.println("}");
  }

  protected void generateNewRemoteInstance(JavaWriter out, String suffix)
    throws IOException
  {
    // ejb/0g27
    if (_bean.getRemoteHome() == null && _bean.getRemoteList().size() == 0)
      return;

    out.println();
    out.println("protected Object _caucho_newRemoteInstance" + suffix + "()");
    out.println("{");
    out.pushDepth();

    out.println(_contextClassName + " cxt = new " + _contextClassName + "(_server);");

    if (isStateless())
      out.println("Bean bean = new Bean(cxt);");
    else
      out.println("Bean bean = new Bean(cxt, null);");

    out.println("cxt._ejb_free(bean);");

    out.println();

    /*
      Class retType = getReturnType();
      if ("RemoteHome".equals(_prefix))
        out.println("return (" + retType.getName() + ") cxt.getRemoteView();");
      else if ("LocalHome".equals(_prefix))
        out.println("return (" + retType.getName() + ") cxt.getLocalView();");
      else
        throw new IOException(L.l("trying to create unknown type {0}",
                              _prefix));
    */

    out.println("return cxt.createRemoteView" + suffix + "();");

    out.popDepth();
    out.println("}");
  }

  private void generateBean(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public static class Bean extends " + _ejbClass.getName() + " {");
    out.pushDepth();

    out.println();
    out.println("protected final static java.util.logging.Logger __caucho_log = com.caucho.log.Log.open(" + _ejbClass.getName() + ".class);");
    out.println("private static int __caucho_dbg_id;");
    out.println("private String __caucho_id;");

    out.println(_contextClassName + " _ejb_context;");
    out.println("boolean _ejb_isActive;");

    int i = 0;

    for (Interceptor interceptor : _bean.getInterceptors()) {
      out.println("Object _interceptor" + i++ + ";");
    }

    out.println();
    if (isStateless())
      out.println("Bean(" + _contextClassName + " context)");
    else
      out.println("Bean(" + _contextClassName + " context, com.caucho.webbeans.context.DependentScope scope)");
    out.println("{");
    out.pushDepth();

    out.println("if (__caucho_isFiner) {");
    out.pushDepth();

    out.println("synchronized (" + _ejbClass.getName() + ".class) {");
    out.println("  __caucho_id = \"" + _ejbClass.getName() + "[\" + __caucho_dbg_id++ + \"]\";");
    out.println("}");
    out.println("__caucho_log.fine(__caucho_id + \":new()\");");
    out.popDepth();
    out.println("}");


    out.println("try {");
    out.pushDepth();

    out.println("_ejb_context = context;");

    if (hasMethod("setSessionContext", new Class[] { SessionContext.class })) {
      // TCK: ejb30/bb/session/stateless/annotation/resource/dataSourceTest
      // ejb/0f55 setSessionContext() can be private, out.println("setSessionContext(context);");
      out.println("invokeMethod(this, \"setSessionContext\", new Class[] { javax.ejb.SessionContext.class }, new Object[] { context });");
    }

    /*
    // ejb/0fd0
    out.println();
    out.println("__caucho_initInjection();");
    */

    out.println();
    if (isStateless())
      out.println("context.getServer().initInstance(this, null);");
    else
      out.println("context.getServer().initInstance(this, scope);");

    out.println();
    out.println("__caucho_callInterceptorsPostConstruct();");

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println("}");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public Object __caucho_callInterceptors(Object target, Object []args, String methodName, Class paramTypes[])");
    out.println("  throws java.lang.reflect.InvocationTargetException");
    out.println("{");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    out.println("Class cl;");
    out.println("Class methodParamTypes[] = new Class[] { javax.interceptor.InvocationContext.class };");
    out.println();

    StringBuilder interceptors = new StringBuilder();
    StringBuilder interceptorMethods = new StringBuilder();

    boolean hasAroundInvoke = false;

    i = 0;

    for (Interceptor interceptor : _bean.getInterceptors()) {
      int j = i++;

      String aroundInvokeMethodName = interceptor.getAroundInvokeMethodName();

      // ejb/0fbi
      if (aroundInvokeMethodName == null)
        continue;

      if (! hasAroundInvoke) {
        hasAroundInvoke = true;
      } else {
        interceptors.append(", ");
        interceptorMethods.append(", ");
      }

      interceptors.append("_interceptor");
      interceptors.append(j);
      interceptorMethods.append("method");
      interceptorMethods.append(j);

      String clName = interceptor.getInterceptorClass();

      out.println("Class cl" + j + " = Class.forName(\"" + clName + "\");");

      out.println("if (_interceptor" + j + " == null) {");
      out.println("  _interceptor" + j + " = cl" + j + ".newInstance();");

      out.println("}");

      out.println();

      generateCallReflectionGetMethod(out, "method" + j, aroundInvokeMethodName, "methodParamTypes", "cl" + j);

      out.println();
    }

    out.println("javax.interceptor.InvocationContext invocationContext;");

    String aroundInvokeMethodName = _bean.getAroundInvokeMethodName();

    // ejb/0fb8
    if (aroundInvokeMethodName != null) {
      if (i > 0) {
        interceptors.append(", ");
        interceptorMethods.append(", ");
      }

      interceptors.append("this");
      interceptorMethods.append(aroundInvokeMethodName);

      out.println();

      generateCallReflectionGetMethod(out, aroundInvokeMethodName, aroundInvokeMethodName, "methodParamTypes", "getClass()");
    }

    // XXX: invocation context pool ???
    out.println();
    out.println("invocationContext = new com.caucho.ejb.interceptor.InvocationContextImpl(this,");
    out.println("  target, methodName, paramTypes, new Object[] { " + interceptors + " }, new java.lang.reflect.Method[] { " + interceptorMethods + " });");
    out.println("invocationContext.setParameters(args);");

    out.println("return invocationContext.proceed();");

    out.popDepth();

    /* ejb/0fba

    // ejb/0f66
    if (_bean.getInterceptors().size() > 0) {
      out.println("} catch (java.lang.reflect.InvocationTargetException e) {");
      out.println("  throw e;");
    }
    */

    out.println("} catch (java.lang.reflect.InvocationTargetException e) {");
    out.println("  throw e;");
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Throwable e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println("}");

    // out.println();
    // out.println("return null;");
    //out.println("return invocationContext;");

    out.popDepth();
    out.println("}");

    // ejb/0fbh
    out.println();
    out.println("private void __caucho_callInterceptorsPostConstruct()");
    out.println("{");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    out.println("Class methodParamTypes[] = new Class[] { javax.interceptor.InvocationContext.class };");

    i = 0;

    for (Interceptor interceptor : _bean.getInterceptors()) {
      int j = i++;

      String postConstructMethodName = interceptor.getPostConstructMethodName();

      if (postConstructMethodName == null)
        continue;

      String clName = interceptor.getInterceptorClass();

      out.println("Class cl" + j + " = Class.forName(\"" + clName + "\");");

      out.println("if (_interceptor" + j + " == null) {");
      out.println("  _interceptor" + j + " = cl" + j + ".newInstance();");

      out.println("}");

      out.println();

      generateCallReflectionGetMethod(out, "method" + j, postConstructMethodName, "methodParamTypes", "cl" + j);

      out.println();
    }

    interceptors.setLength(0);
    interceptorMethods.setLength(0);

    boolean isFirst = true;

    i = 0;

    for (Interceptor interceptor : _bean.getInterceptors()) {
      int j = i++;

      if (interceptor.getPostConstructMethodName() == null)
        continue;

      if (isFirst) {
        isFirst = false;
      } else {
        interceptors.append(", ");
        interceptorMethods.append(", ");
      }

      interceptors.append("_interceptor");
      interceptors.append(j);
      interceptorMethods.append("method");
      interceptorMethods.append(j);
    }

    out.println("javax.interceptor.InvocationContext invocationContext;");

    // XXX: invocation context pool ???
    out.println();
    out.println("invocationContext = new com.caucho.ejb.interceptor.InvocationContextImpl(this,");
    out.println("  this, null, null, new Object[] { " + interceptors + " }, new java.lang.reflect.Method[] { " + interceptorMethods + " });");

    out.println("invocationContext.proceed();");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println("}");

    out.popDepth();
    out.println("}");

    generateInitInjection(out);

    generateReflectionGetMethod(out);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates injection initialization.
   */
  protected void generateInitInjection(JavaWriter out)
    throws IOException
  {
    // ejb/0fd0
    out.println();
    out.println("private void __caucho_initInjection()");
    out.println("{");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    out.println("java.lang.reflect.Field field;");
    out.println();

    for (EnvEntry envEntry : _bean.getEnvEntries()) {
      InjectionTarget injectionTarget = envEntry.getInjectionTarget();

      if (injectionTarget == null)
        continue;

      String value = envEntry.getEnvEntryValue();

      // ejb/0fd4
      if (value == null)
        continue;

      Class cl = envEntry.getEnvEntryType();

      generateInjection(out, injectionTarget, value, cl, true);
    }

    // ejb/0f54
    for (ResourceRef resourceRef : _bean.getResourceRefs()) {
      InjectionTarget injectionTarget = resourceRef.getInjectionTarget();

      if (injectionTarget == null)
        continue;

      String value = "com.caucho.naming.Jndi.lookup(\"java:comp/env/" + resourceRef.getResRefName() + "\")";

      if (value == null)
        continue;

      Class cl = resourceRef.getResType();

      generateInjection(out, injectionTarget, value, cl, false);
    }

    out.popDepth();
    out.println("} catch (Throwable e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates an individual injection.
   */
  protected void generateInjection(JavaWriter out,
                                   InjectionTarget injectionTarget,
                                   String value,
                                   Class cl,
                                   boolean isEscapeString)
    throws IOException
  {
    // ejb/0fd1, ejb/0fd3
    value = generateTypeCasting(value, cl, isEscapeString);

    String s = injectionTarget.getInjectionTargetName();

    out.println("try {");
    out.pushDepth();

    String methodName = "set" + Character.toUpperCase(s.charAt(0));

    if (s.length() > 1)
      methodName += s.substring(1);

    generateCallReflectionGetMethod(out,
                                    "method",
                                    methodName,
                                    "new Class[] { " + cl.getName() + ".class }",
                                    "getClass().getSuperclass()");

    out.println("method.setAccessible(true);");

    out.print("method.invoke(this, ");
    out.print(value);
    out.println(");");

    out.popDepth();
    out.println("} catch (NoSuchMethodException e1) {");
    out.pushDepth();

    java.lang.reflect.Field field = null;

    try {
      field = cl.getDeclaredField("TYPE");
    } catch (NoSuchFieldException e) {
    }

    boolean isPrimitiveWrapper = false;

    if (field != null && Class.class.isAssignableFrom(field.getType())) { //if (cl.isPrimitive())
      isPrimitiveWrapper = true;
    }

    // ejb/0fd2
    if (isPrimitiveWrapper) {
      out.println("try {");
      out.pushDepth();

      // ejb/0fd2 vs ejb/0fd3
      generateCallReflectionGetMethod(out,
                                      "method",
                                      methodName,
                                      "new Class[] { " + cl.getName() + ".TYPE }",
                                      "getClass().getSuperclass()");

      out.println("method.setAccessible(true);");

      out.print("method.invoke(this, ");
      out.print(value);
      out.println(");");

      out.popDepth();
      out.println("} catch (NoSuchMethodException e2) {");
      out.pushDepth();
    }

    out.print("field  = getClass().getSuperclass().getDeclaredField(\"");
    out.print(s);
    out.println("\");");

    out.println("field.setAccessible(true);");

    out.print("field.set(this, ");
    out.print(value);
    out.println(");");

    // ejb/0fd2 vs ejb/0fd3
    if (isPrimitiveWrapper) { // if (! cl.equals(String.class)) {
      out.popDepth();
      out.println("}");
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates a call to get a class method.
   */
  protected void generateCallReflectionGetMethod(JavaWriter out,
                                                 String methodVar,
                                                 String methodName,
                                                 String paramVar,
                                                 String classVar)
    throws IOException
  {
    out.print("java.lang.reflect.Method ");
    out.print(methodVar);
    out.print(" = com.caucho.ejb.util.EjbUtil.getMethod(");
    out.print(classVar);
    out.print(", \"");
    out.print(methodName);
    out.print("\", ");
    out.print(paramVar);
    out.println(");");
  }

  /**
   * Generates reflection to access a class method.
   */
  protected void generateReflectionGetMethod(JavaWriter out)
    throws IOException
  {
    // moved to EjbUtil
  }

  /**
   * Makes private methods accessible before invoking them.
   */
  protected void generateInvokeMethod(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("private static void invokeMethod(Bean bean, String methodName, Class paramTypes[], Object paramValues[])");
    out.println("{");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    out.println("java.lang.reflect.Method m = com.caucho.ejb.util.EjbUtil.getMethod(bean.getClass(), methodName, paramTypes);");
    out.println("m.setAccessible(true);");
    out.println("m.invoke(bean, paramValues);");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.pushDepth();

    out.println("__caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("throw com.caucho.ejb.EJBExceptionWrapper.create(e);");

    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Returns true if the method is implemented.
   */
  protected boolean hasMethod(String methodName, Class []paramTypes)
  {
    return _ejbClass.hasMethod(methodName, paramTypes);
  }

  private String generateTypeCasting(String value, Class cl, boolean isEscapeString)
  {
    if (cl.equals(String.class)) {
      if (isEscapeString)
        value = "\"" + value + "\"";
    } else if (cl.equals(Character.class))
      value = "'" + value + "'";
    else if (cl.equals(Byte.class))
      value = "(byte) " + value;
    else if (cl.equals(Short.class))
      value = "(short) " + value;
    else if (cl.equals(Integer.class))
      value = "(int) " + value;
    else if (cl.equals(Long.class))
      value = "(long) " + value;
    else if (cl.equals(Float.class))
      value = "(float) " + value;
    else if (cl.equals(Double.class))
      value = "(double) " + value;

    return value;
  }
}
