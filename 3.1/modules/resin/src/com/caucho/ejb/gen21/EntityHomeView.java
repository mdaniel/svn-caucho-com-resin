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

package com.caucho.ejb.gen21;

import com.caucho.ejb.cfg21.EjbEntityBean;
import com.caucho.ejb.gen.*;
import com.caucho.ejb.gen21.EntityCreateMethod;
import com.caucho.ejb.gen21.EntityCreateCall;
import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for a session view.
 */
public class EntityHomeView extends ViewClass {
  private static L10N L = new L10N(EntityHomeView.class);

  private EjbEntityBean _bean;

  private ApiClass _remoteClass;
  private String _prefix;
  private String _contextClassName;
  private boolean _isCMP;

  public EntityHomeView(ApiClass remoteClass,
                        String contextClassName,
                        String prefix,
                        boolean isCMP)
  {
    super(prefix, "Entity" + prefix);

    addInterfaceName(remoteClass.getName());

    _contextClassName = contextClassName;
    _prefix = prefix;

    setStatic(true);
    _isCMP = isCMP;
  }

  /**
   * Adds a business method.
   */
  public void addMethod(BaseMethod method)
  {
    addComponent(method);
  }

  public BaseMethod createCreateMethod(EjbEntityBean bean,
                                       ApiMethod api,
                                       ApiMethod create,
                                       ApiMethod postCreate,
                                       String fullClassName)
  {
    EntityCreateMethod method;
    method = new EntityCreateMethod(bean, api, create,
                                    postCreate, fullClassName);

    EntityCreateCall call = (EntityCreateCall) method.getCall();

    call.setCMP(_isCMP);

    return method;
  }

  /**
   * Adds the pool chaining.
   */
  public CallChain createPoolChain(CallChain call, BaseMethod method)
  {
    return new EntityPoolChain(call, true);
  }

  public void generate(JavaWriter out)
    throws IOException
  {
    generateGetter(out);

    out.println();
    super.generate(out);
  }

  private void generateGetter(JavaWriter out)
    throws IOException
  {
    if (_prefix.equals("RemoteHome")) {
      out.println();
      out.println("public EJBHome createRemoteHomeView()");
      out.println("{");
      out.println("  return new RemoteHome(this);");
      out.println("}");
      out.println();
    }
    else {
      out.println();
      out.println("public EJBLocalHome createLocalHome()");
      out.println("{");
      out.println("  return new LocalHome(this);");
      out.println("}");
      out.println();
    }
  }

  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private " + _contextClassName + " _context;");
    out.println("private EjbTransactionManager _xaManager;");
    out.println();
    out.println(_prefix + "(" + _contextClassName + " context)");
    out.println("{");
    out.println("  super(context.getEntityServer());");
    out.println("  _context = context;");
    out.println("  _xaManager = _server.getTransactionManager();");
    out.println("}");
    out.println();
    out.println("private " + _contextClassName + " getContext()");
    out.println("{");
    out.println("  return _context;");
    out.println("}");

    out.println();
    out.println("public Handle getHandle()");
    out.println("{");
    out.println("  return getContext().getHandle();");
    out.println("}");

    out.println();

    generateComponents(out);
  }
}
