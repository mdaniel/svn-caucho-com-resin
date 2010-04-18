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

import javax.ejb.Singleton;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.View;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Generates the skeleton for a singleton bean.
 */
@Module
public class SingletonGenerator<X> extends SessionGenerator<X> {
  public SingletonGenerator(String ejbName, AnnotatedType<X> ejbClass,
                            ArrayList<AnnotatedType<?>> localApi,
                            ArrayList<AnnotatedType<?>> remoteApi)
  {
    super(ejbName, ejbClass, localApi, remoteApi, 
          Singleton.class.getSimpleName());
  }

  @Override
  public boolean isStateless()
  {
    return false;
  }

  @Override
  protected <T> View<X,T> createLocalView(AnnotatedType<T> api)
  {
    return new SingletonView<X,T>(this, api);
  }

  @Override
  protected <T> View<X,T> createRemoteView(AnnotatedType<T> api)
  {
    return new SingletonView<X,T>(this, api);
  }

  /**
   * Scans for the @Local interfaces
   */
  @Override
  protected ArrayList<AnnotatedType<?>> introspectLocalDefault()
  {
    ArrayList<AnnotatedType<?>> apiClass = new ArrayList<AnnotatedType<?>>();

    apiClass.add(getBeanClass());

    return apiClass;
  }

  /**
   * Generates the singleton session bean
   */
  @Override
  public void generate(JavaWriter out) throws IOException
  {    
    generateTopComment(out);

    out.println();
    out.println("package " + getPackageName() + ";");

    out.println();
    out.println("import com.caucho.config.*;");
    out.println("import com.caucho.ejb.*;");
    out.println("import com.caucho.ejb.session.*;");
    out.println();
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");

    out.println();
    out.println("public class " + getClassName());
    out.println("  extends SingletonContext");
    out.println("{");
    out.pushDepth();

    out.println();
    out.println("public " + getClassName() + "(SingletonManager manager)");
    out.println("{");
    out.pushDepth();

    out.println("super(manager);");

    for (View<X,?> view : getViews()) {
      view.generateContextHomeConstructor(out);
    }

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public " + getClassName() + "(" + getClassName()
                + " context)");
    out.println("{");
    out.pushDepth();

    out.println("super(context.getServer());");

    generateContextObjectConstructor(out);

    out.popDepth();
    out.println("}");

    for (View<X,?> view : getViews()) {
      view.generateContextPrologue(out);
    }

    generateCreateProvider(out);
    generateViews(out);

    generateDependency(out);

    out.popDepth();
    out.println("}");
  }

  protected void generateCreateProvider(JavaWriter out) throws IOException
  {
    out.println();
    out.println("@Override");
    out.println("public SingletonProxyFactory getProxyFactory(Class api)");
    out.println("{");
    out.pushDepth();

    for (View<X,?> view : getViews()) {
      SingletonView<X,?> sView = (SingletonView<X,?>) view;

      sView.generateCreateProvider(out, "api");
    }

    out.println();
    out.println("return super.getProxyFactory(api);");

    out.popDepth();
    out.println("}");
  }

  /**
   * Creates any additional code in the constructor
   */
  public void generateContextObjectConstructor(JavaWriter out)
      throws IOException
  {
    for (View<X,?> view : getViews()) {
      view.generateContextObjectConstructor(out);
    }
  }

  @Override
  protected void generateContext(JavaWriter out)
  {
  }
}
