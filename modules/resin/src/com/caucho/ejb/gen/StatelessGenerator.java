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

import com.caucho.config.gen.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import javax.ejb.*;

/**
 * Generates the skeleton for a session bean.
 */
public class StatelessGenerator extends SessionGenerator {
  private static final L10N L = new L10N(StatelessGenerator.class);

  public StatelessGenerator(String ejbName,
                            ApiClass ejbClass,
                            ApiClass localHome,
                            ArrayList<ApiClass> localApi,
                            ApiClass remoteHome,
                            ArrayList<ApiClass> remoteApi)
  {
    super(ejbName, ejbClass,
          localHome, localApi,
          remoteHome, remoteApi);
  }

  public boolean isStateless()
  {
    return true;
  }

  @Override
  protected View createLocalView(ApiClass api)
  {
    return new StatelessLocalView(this, api);
  }

  @Override
  protected View createLocalHomeView(ApiClass api)
  {
    return new StatelessLocalHomeView(this, api);
  }

  @Override
  protected View createRemoteView(ApiClass api)
  {
    return new StatelessRemoteView(this, api);
  }

  @Override
  protected View createRemoteHomeView(ApiClass api)
  {
    return new StatelessRemoteHomeView(this, api);
  }

  /**
   * Generates the stateful session bean
   */
  @Override
  public void generate(JavaWriter out)
    throws IOException
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

  protected void generateCreateProvider(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public StatelessProvider getProvider(Class api)");
    out.println("{");
    out.pushDepth();

    for (View view : getViews()) {
      StatelessView sView = (StatelessView) view;

      sView.generateCreateProvider(out, "api");
    }

    out.println();
    out.println("return super.getProvider(api);");

    out.popDepth();
    out.println("}");
  }

  @Override
  protected void generateContext(JavaWriter out)
    throws IOException
  {
    String shortContextName = getBeanClass().getSimpleName();

    int freeStackMax = 16;

    out.println("protected static final java.util.logging.Logger __caucho_log = java.util.logging.Logger.getLogger(\"" + getFullClassName() + "\");");
    out.println("protected static final boolean __caucho_isFiner = __caucho_log.isLoggable(java.util.logging.Level.FINER);");

    String beanClass = getBeanClass().getName();

    /*
    out.println();
    out.println("private " + beanClass + " []_freeBeanStack = new "
                + beanClass + "[" + freeStackMax + "];");
    out.println("private int _freeBeanTop;");
    */

    out.println();
    out.println("public " + getClassName() + "(StatelessServer server)");
    out.println("{");
    out.pushDepth();

    out.println("super(server);");
    //out.println("_xaManager = server.getTransactionManager();");

    for (View view : getViews()) {
      view.generateContextHomeConstructor(out);
    }

    out.popDepth();
    out.println("}");

    for (View view : getViews()) {
      view.generateContextPrologue(out);
    }

    out.println();
    out.println("public void __caucho_timeout_callback(javax.ejb.Timer timer)");
    out.println("{");
    out.pushDepth();

    for (View view : getViews()) {
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

  protected void generateTimeoutCallback(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void __caucho_timeout_callback(java.lang.reflect.Method method, javax.ejb.Timer timer)");
    out.println("  throws IllegalAccessException, java.lang.reflect.InvocationTargetException");
    out.println("{");
    out.pushDepth();

    View objectView = null;

    for (View view : getViews()) {
      if (view instanceof StatelessObjectView) {
        objectView = view;
        break;
      }
    }

    if (objectView != null) {
      out.print(objectView.getBeanClassName() + " bean = ");
      objectView.generateNewInstance(out);
      out.println(";");
      out.println("method.invoke(bean, timer);");
      objectView.generateFreeInstance(out, "bean");
    }

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_timeout_callback(java.lang.reflect.Method method)");
    out.println("  throws IllegalAccessException, java.lang.reflect.InvocationTargetException");
    out.println("{");
    out.pushDepth();

    if (objectView != null) {
      out.print(objectView.getBeanClassName() + " bean = ");
      objectView.generateNewInstance(out);
      out.println(";");
      out.println("method.invoke(bean);");
      objectView.generateFreeInstance(out, "bean");
    }

    out.popDepth();
    out.println("}");
  }

  public void generateViews(JavaWriter out)
    throws IOException
  {
    for (View view : getViews()) {
      out.println();

      view.generate(out);
    }
  }

  protected void generateNewInstance(JavaWriter out)
    throws IOException
  {
  }
}
