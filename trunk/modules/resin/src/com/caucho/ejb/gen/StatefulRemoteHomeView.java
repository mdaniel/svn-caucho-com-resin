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
import com.caucho.config.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Represents a public interface to a bean, e.g. a remote stateful view
 */
public class StatefulRemoteHomeView extends StatefulHomeView {
  private static final L10N L = new L10N(StatefulRemoteHomeView.class);

  public StatefulRemoteHomeView(StatefulGenerator bean, ApiClass api)
  {
    super(bean, api);
  }

  @Override
  public String getViewClassName()
  {
    return getViewClass().getSimpleName() + "__EJBRemoteHome";
  }

  @Override
  protected void generateExtends(JavaWriter out)
    throws IOException
  {
    out.println("  extends StatefulHome");
  }

  /**
   * Generates prologue for the context.
   */
  public void generateContextPrologue(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("private " + getViewClassName() + " _remoteHome;");

    if (EJBHome.class.isAssignableFrom(getViewClass().getJavaClass())) {
      out.println();
      out.println("@Override");
      out.println("public EJBHome getEJBHome()");
      out.println("{");
      out.println("  return _remoteHome;");
      out.println("}");
    }
  }

  /**
   * Generates context home's constructor
   */
  @Override
  public void generateContextHomeConstructor(JavaWriter out)
    throws IOException
  {
    out.println("_remoteHome = new " + getViewClassName() + "(this);");
  }

  /**
   * Generates context home's constructor
   */
  @Override
  public void generateContextObjectConstructor(JavaWriter out)
    throws IOException
  {
    out.println("_remoteHome = context._remoteHome;");
  }

  /**
   * Generates code to create the provider
   */
  public void generateCreateProvider(JavaWriter out, String var)
    throws IOException
  {
    out.println();
    out.println("if (" + var + " == " + getViewClass().getName() + ".class)");
    out.println("  return _remoteHome;");
  }
}
