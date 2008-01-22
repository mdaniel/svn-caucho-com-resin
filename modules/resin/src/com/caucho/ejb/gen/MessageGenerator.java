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

import com.caucho.java.JavaWriter;
import com.caucho.ejb.cfg.*;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.io.IOException;

/**
 * Generates the skeleton for a message bean.
 */
public class MessageGenerator extends BeanGenerator {
  private static final L10N L = new L10N(BeanGenerator.class);

  public MessageGenerator(String ejbName, ApiClass ejbClass)
  {
    super(toFullClassName(ejbName, ejbClass.getSimpleName()), ejbClass);

    //    addView(new MessageView(this));
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
    sb.append("__EJB");

    return sb.toString();
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
    out.println("import com.caucho.ejb.session.*;");
    out.println();
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");
    
    out.println();
    out.println("public class " + getClassName());
    out.println("{");
    out.pushDepth();

    out.println();
    out.println("public " + getClassName() + "(MessageServer server)");
    out.println("{");
    out.pushDepth();
    
    out.println("super(server);");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public " + getClassName() + "(" + getClassName() + " context)");
    out.println("{");
    out.pushDepth();
    
    out.println("super(context.getMessageServer());");

    out.popDepth();
    out.println("}");

    for (View view : getViews()) {
      view.generateContextPrologue(out);
    }

    generateViews(out);
    
    out.popDepth();
    out.println("}");
  }
}
