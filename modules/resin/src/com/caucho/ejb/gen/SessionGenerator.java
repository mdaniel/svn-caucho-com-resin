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

package com.caucho.ejb.gen;

import com.caucho.config.gen.*;
import com.caucho.config.*;
import com.caucho.config.types.InjectionTarget;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.io.IOException;
import java.util.*;

/**
 * Generates the skeleton for a session bean.
 */
abstract public class SessionGenerator extends BeanGenerator {
  private static final L10N L = new L10N(SessionGenerator.class);

  private ArrayList<ApiClass> _localApi = new ArrayList<ApiClass>();

  private ArrayList<ApiClass> _remoteApi = new ArrayList<ApiClass>();

  private ArrayList<View> _views = new ArrayList<View>();

  protected String _contextClassName = "dummy";

  public SessionGenerator(String ejbName, ApiClass ejbClass,
                          ArrayList<ApiClass> localApi,
                          ArrayList<ApiClass> remoteApi, String beanType) {
    super(toFullClassName(ejbName, ejbClass.getSimpleName(), beanType),
        ejbClass);

    _contextClassName = "dummy";

    _localApi = new ArrayList<ApiClass>(localApi);

    _remoteApi = new ArrayList<ApiClass>(remoteApi);
  }

  public static String toFullClassName(String ejbName, String className,
                                       String beanType) {
    StringBuilder sb = new StringBuilder();

    sb.append("_ejb.");

    if (!Character.isJavaIdentifierStart(ejbName.charAt(0)))
      sb.append('_');

    for (int i = 0; i < ejbName.length(); i++) {
      char ch = ejbName.charAt(i);

      if (ch == '/')
        sb.append('.');
      else if (Character.isJavaIdentifierPart(ch))
        sb.append(ch);
      else
        sb.append('_');
    }

    sb.append(".");
    sb.append(className);
    sb.append("__");
    sb.append(beanType);
    sb.append("Context");

    return sb.toString();
  }

  public boolean isStateless() {
    return false;
  }

  /**
   * Returns the local API list.
   */
  public ArrayList<ApiClass> getLocalApi() {
    return _localApi;
  }

  /**
   * Returns the remote API list.
   */
  public ArrayList<ApiClass> getRemoteApi() {
    return _remoteApi;
  }

  /**
   * Returns the views
   */
  public ArrayList<View> getViews() {
    return _views;
  }

  /**
   * Returns the view matching the given class
   */
  public View getView(Class api) {
    for (View view : _views) {
      if (view.getViewClass().getName().equals(api.getName()))
        return view;
    }

    return null;
  }

  /**
   * Introspects the bean.
   */
  @Override
  public void introspect() {
    super.introspect();

    if (_localApi.size() == 0 && _remoteApi.size() == 0) {
      _localApi = introspectLocalDefault();
    }
  }

  /**
   * Generates the views for the bean
   */
  @Override
  public void createViews() {
    for (ApiClass api : _localApi) {
      View view = createLocalView(api);

      _views.add(view);
    }

    for (ApiClass api : _remoteApi) {
      View view = createRemoteView(api);

      _views.add(view);
    }

    for (View view : _views)
      view.introspect();
  }

  /**
   * Generates the local view for the given class
   */
  protected View createLocalView(ApiClass api) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates the remote view for the given class
   */
  protected View createRemoteView(ApiClass api) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Scans for the @Local interfaces
   */
  protected ArrayList<ApiClass> introspectLocalDefault() {
    throw new ConfigException(L
        .l("'{0}' does not have any interfaces defined.", getBeanClass()
            .getName()));
  }

  /**
   * Scans for the @Local interfaces
   */
  private ArrayList<ApiClass> introspectLocalApi() {
    ArrayList<ApiClass> apiList = new ArrayList<ApiClass>();

    Local local = (Local) getBeanClass().getAnnotation(Local.class);
    Remote remote = (Remote) getBeanClass().getAnnotation(Remote.class);

    if (local != null && local.value().length > 0) {
      for (Class<?> api : local.value()) {
        apiList.add(new ApiClass(api));
      }

      return apiList;
    }

    boolean hasRemote = remote != null;

    for (ApiClass api : getBeanClass().getInterfaces()) {
      if (api.getJavaClass().isAnnotationPresent(Local.class))
        apiList.add(api);
      if (api.getJavaClass().isAnnotationPresent(Remote.class))
        hasRemote = true;
    }

    if (apiList.size() > 0 || hasRemote)
      return apiList;

    ApiClass singleApi = null;
    for (ApiClass api : getBeanClass().getInterfaces()) {
      Class javaApi = api.getJavaClass();

      if (javaApi.equals(java.io.Serializable.class))
        continue;
      if (javaApi.equals(java.io.Externalizable.class))
        continue;
      if (javaApi.equals(javax.ejb.SessionBean.class))
        continue;
      if (javaApi.getName().startsWith("javax.ejb."))
        continue;
      if (javaApi.isAnnotationPresent(Remote.class)) {
        continue;
      }

      if (singleApi != null) {
        throw new ConfigException(
            L
                .l(
                   "{0}: does not have a unique local API.  Both '{1}' and '{2}' are local.",
                   getBeanClass().getName(), singleApi.getName(), api.getName()));
      }

      singleApi = api;
    }

    if (singleApi != null) {
      apiList.add(singleApi);

      return apiList;
    }

    // XXX: only for stateful?
    // apiList.add(getBeanClass());

    return apiList;
  }

  /**
   * Scans for the @Remote interfaces
   */
  private ArrayList<ApiClass> introspectRemoteApi() {
    ArrayList<ApiClass> apiList = new ArrayList<ApiClass>();

    Remote remote = (Remote) getBeanClass().getAnnotation(Remote.class);

    if (remote != null && remote.value().length > 0) {
      for (Class api : remote.value()) {
        apiList.add(new ApiClass(api));
      }

      return apiList;
    }

    for (ApiClass api : getBeanClass().getInterfaces()) {
      Class javaApi = api.getJavaClass();

      if (java.io.Serializable.class.equals(javaApi))
        continue;
      else if (java.io.Externalizable.class.equals(javaApi))
        continue;
      else if (javaApi.getName().startsWith("javax.ejb"))
        continue;

      if (javaApi.isAnnotationPresent(Remote.class) || remote != null)
        apiList.add(api);
    }

    if (apiList.size() > 0)
      return apiList;

    return apiList;
  }

  abstract protected void generateContext(JavaWriter out) throws IOException;

  protected void generateNewInstance(JavaWriter out, String suffix)
      throws IOException {
  }

  protected void generateNewRemoteInstance(JavaWriter out, String suffix)
      throws IOException {
    // ejb/0g27
    /*
     * if (_bean.getRemoteHome() == null && _bean.getRemoteList().size() == 0)
     * return;
     */
    out.println();
    out.println("protected Object _caucho_newRemoteInstance" + suffix + "()");
    out.println("{");
    out.pushDepth();

    out.println(_contextClassName + " cxt = new " + _contextClassName
        + "(_server);");

    if (isStateless())
      out.println("Bean bean = new Bean(cxt);");
    else
      out.println("Bean bean = new Bean(cxt, null);");

    out.println("cxt._ejb_free(bean);");

    out.println();

    /*
     * Class retType = getReturnType(); if ("RemoteHome".equals(_prefix))
     * out.println("return (" + retType.getName() + ") cxt.getRemoteView();");
     * else if ("LocalHome".equals(_prefix)) out.println("return (" +
     * retType.getName() + ") cxt.getLocalView();"); else throw new
     * IOException(L.l("trying to create unknown type {0}", _prefix));
     */

    out.println("return cxt.createRemoteView" + suffix + "();");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates injection initialization.
   */
  protected void generateInitInjection(JavaWriter out) throws IOException {
    /*
     * // ejb/0fd0 out.println();
     * out.println("private void __caucho_initInjection()"); out.println("{");
     * out.pushDepth();
     * 
     * out.println("try {"); out.pushDepth();
     * 
     * out.println("java.lang.reflect.Field field;"); out.println();
     * 
     * for (EnvEntry envEntry : _bean.getEnvEntries()) { InjectionTarget
     * injectionTarget = envEntry.getInjectionTarget();
     * 
     * if (injectionTarget == null) continue;
     * 
     * String value = envEntry.getEnvEntryValue();
     * 
     * // ejb/0fd4 if (value == null) continue;
     * 
     * Class cl = envEntry.getEnvEntryType();
     * 
     * generateInjection(out, injectionTarget, value, cl, true); }
     * 
     * // ejb/0f54 for (ResourceRef resourceRef : _bean.getResourceRefs()) {
     * InjectionTarget injectionTarget = resourceRef.getInjectionTarget();
     * 
     * if (injectionTarget == null) continue;
     * 
     * String value = "com.caucho.naming.Jndi.lookup(\"java:comp/env/" +
     * resourceRef.getResRefName() + "\")";
     * 
     * if (value == null) continue;
     * 
     * Class cl = resourceRef.getResType();
     * 
     * generateInjection(out, injectionTarget, value, cl, false); }
     * 
     * out.popDepth(); out.println("} catch (Throwable e) {");out.println(
     * "  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
     * out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
     * out.println("}");
     * 
     * out.popDepth(); out.println("}");
     */
  }

  /**
   * Generates an individual injection.
   */
  protected void generateInjection(JavaWriter out,
                                   InjectionTarget injectionTarget,
                                   String value, Class cl,
                                   boolean isEscapeString) throws IOException {
    // ejb/0fd1, ejb/0fd3
    value = generateTypeCasting(value, cl, isEscapeString);

    String s = injectionTarget.getInjectionTargetName();

    out.println("try {");
    out.pushDepth();

    String methodName = "set" + Character.toUpperCase(s.charAt(0));

    if (s.length() > 1)
      methodName += s.substring(1);

    generateCallReflectionGetMethod(out, "method", methodName, "new Class[] { "
        + cl.getName() + ".class }", "getClass().getSuperclass()");

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

    if (field != null && Class.class.isAssignableFrom(field.getType())) { // if
      // (cl.isPrimitive())
      isPrimitiveWrapper = true;
    }

    // ejb/0fd2
    if (isPrimitiveWrapper) {
      out.println("try {");
      out.pushDepth();

      // ejb/0fd2 vs ejb/0fd3
      generateCallReflectionGetMethod(out, "method", methodName,
                                      "new Class[] { " + cl.getName()
                                          + ".TYPE }",
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
      throws IOException {
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
  protected void generateReflectionGetMethod(JavaWriter out) throws IOException {
    // moved to EjbUtil
  }

  /**
   * Makes private methods accessible before invoking them.
   */
  protected void generateInvokeMethod(JavaWriter out) throws IOException {
    out.println();
    out
        .println("private static void invokeMethod(Bean bean, String methodName, Class paramTypes[], Object paramValues[])");
    out.println("{");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();

    out
        .println("java.lang.reflect.Method m = com.caucho.ejb.util.EjbUtil.getMethod(bean.getClass(), methodName, paramTypes);");
    out.println("m.setAccessible(true);");
    out.println("m.invoke(bean, paramValues);");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.pushDepth();

    out
        .println("__caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("throw com.caucho.ejb.EJBExceptionWrapper.create(e);");

    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Returns true if the method is implemented.
   */
  public boolean hasMethod(String methodName, Class[] paramTypes) {
    return getBeanClass().hasMethod(methodName, paramTypes);
  }

  private String generateTypeCasting(String value, Class<?> cl,
                                     boolean isEscapeString) {
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
