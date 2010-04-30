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

import java.io.IOException;
import java.util.ArrayList;

import javax.ejb.Stateless;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.View;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Generates the skeleton for a session bean.
 */
@Module
public class StatelessGenerator<X> extends SessionGenerator<X> {
  public StatelessGenerator(String ejbName, AnnotatedType<X> ejbClass,
                            ArrayList<AnnotatedType<?>> localApi,
                            ArrayList<AnnotatedType<?>> remoteApi)
  {
    super(ejbName, ejbClass, localApi, remoteApi, 
          Stateless.class.getSimpleName());
  }

  @Override
  public boolean isStateless()
  {
    return true;
  }

  /**
   * Returns the interface itself for the no-interface view
   */
  @Override
  protected ArrayList<AnnotatedType<?>> introspectLocalDefault() 
  {
    ArrayList<AnnotatedType<?>> apiClass = new ArrayList<AnnotatedType<?>>();

    apiClass.add(getBeanClass());

    return apiClass;
  }

  @Override
  protected <T> View<X,T> createLocalView(AnnotatedType<T> api)
  {
    return new StatelessView<X,T>(this, api);
  }

  @Override
  protected <T> View<X,T> createRemoteView(AnnotatedType<T> api)
  {
    return new StatelessView<X,T>(this, api);
  }

  /**
   * Generates the stateful session bean
   */
  @Override
  public void generate(JavaWriter out) throws IOException
  {
    generateTopComment(out);

    out.println();
    out.println("package " + getPackageName() + ";");

    out.println();
    out.println("import com.caucho.config.*;");
    out.println("import com.caucho.ejb.session.*;");
    out.println();
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");

    out.println();
    out.println("public class " + getClassName());
    out.println("  extends StatelessContext");
    out.println("{");
    out.pushDepth();

    generateContext(out);

    generateCreateProvider(out);

    generateViews(out);

    generateDependency(out);

    out.popDepth();
    out.println("}");
  }

  protected void generateCreateProvider(JavaWriter out) throws IOException
  {
    out.println();
    out.println("public StatelessProvider getProvider(Class api)");
    out.println("{");
    out.pushDepth();

    for (View<X,?> view : getViews()) {
      StatelessView<X,?> sView = (StatelessView<X,?>) view;

      sView.generateCreateProvider(out, "api");
    }

    out.println();
    out.println("return super.getProvider(api);");

    out.popDepth();
    out.println("}");
  }

  @Override
  protected void generateContext(JavaWriter out) throws IOException
  {
    out.println("protected static final java.util.logging.Logger __caucho_log = java.util.logging.Logger.getLogger(\""
                + getFullClassName() + "\");");
    out.println("protected static final boolean __caucho_isFiner = __caucho_log.isLoggable(java.util.logging.Level.FINER);");

    out.println();
    out.println("public " + getClassName() + "(StatelessManager server)");
    out.println("{");
    out.pushDepth();

    out.println("super(server);");
    // out.println("_xaManager = server.getTransactionManager();");

    for (View<X,?> view : getViews()) {
      view.generateContextHomeConstructor(out);
    }

    out.popDepth();
    out.println("}");

    for (View<X,?> view : getViews()) {
      view.generateContextPrologue(out);
    }

    out.println();
    out.println("public void __caucho_timeout_callback(javax.ejb.Timer timer)");
    out.println("{");
    out.pushDepth();

    for (View<X,?> view : getViews()) {
      view.generateTimer(out);
    }

    out.popDepth();
    out.println("}");

    generateTimeoutCallback(out);

    out.println();
    out.println("public void destroy()");
    out.println("{");
    out.pushDepth();

    generateDestroyViews(out);

    out.popDepth();
    out.println("}");
  }

  protected void generateTimeoutCallback(JavaWriter out) throws IOException
  {
    String beanClass = getBeanClass().getJavaClass().getName();

    out.println();
    out.println("public void __caucho_timeout_callback(java.lang.reflect.Method method, javax.ejb.Timer timer)");
    out.println("  throws IllegalAccessException, java.lang.reflect.InvocationTargetException");
    out.println("{");
    out.pushDepth();

    View<X,?> objectView = null;

    for (View<X,?> view : getViews()) {
      if (view instanceof StatelessView<?,?>) {
        objectView = view;
        break;
      }
    }

    if (objectView != null) {
      // XXX: 4.0.7 - needs to be moved to view
      /*
      out.println("StatelessPool.Item<" + beanClass +"> item");
      out.println("  = _statelessPool.allocate();");

      out.println("try {");
      out.println("  method.invoke(item.getValue(), timer);");
      out.println("} finally {");
      out.println("  _statelessPool.free(item);");
      out.println("}");
      */
    }

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_timeout_callback(java.lang.reflect.Method method)");
    out.println("  throws IllegalAccessException, java.lang.reflect.InvocationTargetException");
    out.println("{");
    out.pushDepth();

    if (objectView != null) {
      // XXX: 4.0.7 - must be moved to view
      /*
      out.println("StatelessPool.Item<" + beanClass +"> item");
      out.println("  = _statelessPool.allocate();");

      out.println("try {");
      out.println("  method.invoke(item.getValue());");
      out.println("} finally {");
      out.println("  _statelessPool.free(item);");
      out.println("}");
      */
    }

    out.popDepth();
    out.println("}");
  }

  @Override
  public void generateViews(JavaWriter out) throws IOException
  {
    for (View<X,?> view : getViews()) {
      out.println();

      view.generate(out);
    }
  }

  protected void generateNewInstance(JavaWriter out) throws IOException {
  }
}
