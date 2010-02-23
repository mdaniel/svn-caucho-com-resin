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

import static javax.ejb.TransactionAttributeType.REQUIRED;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ejb.MessageDrivenBean;

import com.caucho.config.gen.ApiClass;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.gen.BusinessMethodGenerator;
import com.caucho.config.gen.View;
import com.caucho.java.JavaWriter;

/**
 * Generates the skeleton for a message bean.
 */
public class MessageGenerator extends BeanGenerator {
  private MessageView _view;
  private ArrayList<View> _views = new ArrayList<View>();
  
  public MessageGenerator(String ejbName, ApiClass ejbClass)
  {
    super(toFullClassName(ejbName, ejbClass.getSimpleName()), ejbClass);
  }

  private static String toFullClassName(String ejbName, String className)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("_ejb.");

    if (! Character.isJavaIdentifierStart(ejbName.charAt(0)))
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
    sb.append("__BeanContext");

    return sb.toString();
  }

  public void setApi(ApiClass api)
  {
    _view = new MessageView(this, api);
    _views.add(_view);
  }

  public ArrayList<View> getViews()
  {
    return _views;
  }

  /**
   * Introspects the bean.
   */
  @Override
  public void introspect()
  {
    super.introspect();
    
    for (View view : getViews())
      view.introspect();
  }
  
  /**
   * Generates the message session bean
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
    out.println("import com.caucho.ejb.*;");
    out.println("import com.caucho.ejb.message.*;");
    out.println();
    out.println("import java.util.*;");
    out.println("import java.lang.reflect.*;");
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");
    out.println("import javax.transaction.xa.*;");
    out.println("import javax.resource.spi.endpoint.*;");
    
    out.println();
    out.println("public class " + getClassName()
		+ " extends " + getBeanClass().getName()
		+ " implements MessageEndpoint, CauchoMessageEndpoint");
    out.println("{");
    out.pushDepth();

    /*
    // ejb/0931
    out.println();
    out.println("private static final com.caucho.ejb3.xa.XAManager _xa");
    out.println("  = new com.caucho.ejb3.xa.XAManager();");
    */

    out.println("private static HashSet<Method> _xaMethods = new HashSet<Method>();");
    out.println();
    out.println("private MessageServer _server;");
    out.println("private XAResource _xaResource;");
    out.println("private boolean _isXa;");

    HashMap<String,Object> map = new HashMap<String,Object>();
    map.put("caucho.ejb.xa", "true");
    for (View view : getViews()) {
      // view.generateContextPrologue(out);
      view.generateBeanPrologue(out, map);
    }

    out.println();
    out.println("public " + getClassName() + "(MessageServer server)");
    out.println("{");
    out.pushDepth();

    out.println("_server = server;");

    if (MessageDrivenBean.class.isAssignableFrom(getBeanClass().getJavaClass())) {
      out.println("setMessageDrivenContext(server.getMessageContext());");
    }

    _view.generateBeanConstructor(out);

    out.popDepth();
    out.println("}");

    out.println();
    out.println("static {");
    out.pushDepth();
    out.println("try {");
    out.pushDepth();

    for (BusinessMethodGenerator bizMethod : _view.getMethods()) {
      if (REQUIRED.equals(bizMethod.getXa().getTransactionType())) {
	Method api = bizMethod.getApiMethod().getMethod();
	
	out.print("_xaMethods.add(");
	out.printClass(api.getDeclaringClass());
	out.print(".class.getMethod(\"");
	out.print(api.getName());
	out.print("\", new Class[] { ");

	for (Class<?> cl : api.getParameterTypes()) {
	  out.printClass(cl);
	  out.print(".class, ");
	}
	out.println("}));");
      }
    }

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void __caucho_setXAResource(XAResource xaResource)");
    out.println("{");
    out.println("  _xaResource = xaResource;");
    out.println("}");


    out.println();
    out.println("public void beforeDelivery(java.lang.reflect.Method method)");
    out.println("{");
    out.println("  if (_xaMethods.contains(method)) {");
    out.println("    _isXa = (_xa.beginRequired() == null);");
    out.println("  }");
    out.println("}");

    out.println("public void afterDelivery()");
    out.println("{");
    out.println("  if (_isXa) {");
    out.println("    _isXa = false;");
    out.println("    _xa.commit();");
    out.println("  }");
    out.println("}");

    out.println();
    out.println("public void release()");
    out.println("{");
    out.pushDepth();

    if (getBeanClass().hasMethod("ejbRemove", new Class[0])) {
      out.println("ejbRemove();");
    }
    
    out.popDepth();
    out.println("}");

    /*
    for (View view : getViews()) {
      view.generateContextPrologue(out);
    }
    */

    generateViews(out);

    generateDependency(out);
    
    out.popDepth();
    out.println("}");
  }
}
