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

package com.caucho.ejb.cfg;

import com.caucho.config.ConfigException;
import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Configuration for a many-to-one CMP method.
 */
public class EjbManyToOneSetMethod extends EjbMethod {
  private static final L10N L = new L10N(EjbManyToOneSetMethod.class);

  private CmrManyToOne _manyToOne;
  
  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public EjbManyToOneSetMethod(EjbView view,
			       ApiMethod apiMethod, ApiMethod implMethod,
			       CmrManyToOne manyToOne)
  {
    super(view, apiMethod, implMethod);

    _manyToOne = manyToOne;
  }

  /**
   * Assembles the bean method.
   */
  public void assembleBean(BeanAssembler beanAssembler, String fullClassName)
    throws ConfigException
  {
    beanAssembler.addMethod(new BeanMethod(getImplMethod()));
  }

  class BeanMethod extends BaseMethod {
    BeanMethod(ApiMethod method)
    {
      super(method.getMethod());
    }
  
    /**
     * Generates the code for the call.
     *
     * @param out the writer to the output stream.
     * @param args the arguments
     */
    protected void generateCall(JavaWriter out, String []args)
      throws IOException
    {
      CmrRelation targetRelation = _manyToOne.getTargetRelation();

      String value = args[0];

      if (targetRelation instanceof CmrManyToOne) {
	CmrManyToOne targetManyToOne = (CmrManyToOne) targetRelation;

	ApiMethod srcGetter = _manyToOne.getGetter();
	ApiMethod dstSetter = targetManyToOne.getSetter();

	// XXX: EJBClass, i.e. Bean setter 
	
	if (dstSetter != null) {
	  EjbEntityBean targetBean = _manyToOne.getTargetBean();
	  String targetType = targetBean.getLocal().getName();

	  String localType = _manyToOne.getBean().getLocal().getName();

	  ApiMethod localDstSetter
	    = targetBean.getMethod(targetBean.getLocal(), dstSetter);

	  out.println(targetType + " oldBean = " + srcGetter.getName() + "();");

	  out.println("if (" + value + " == oldBean || " +
		      value + " != null && " + value + ".equals(oldBean))");
	  out.println("  return;");

	  out.println("super." + getImplMethod().getName() + "(" + value + ");");

	  out.println("if (oldBean != null) {");
	  out.pushDepth();

	  if (localDstSetter != null) {
	    out.print("oldBean");
	  }
	  else {
	    out.print("((" + targetBean.getEJBClass().getName() + ") ");
	    out.print("((com.caucho.ejb.entity.EntityObject) oldBean)._caucho_getBean(_ejb_trans, false))");
	  }
	  
	  out.println("." + dstSetter.getName() + "((" + localType + ") null);");
	  
	  out.popDepth();
	  out.println("}");

	  out.println();
	  out.println("if (" + value + " != null) {");
	  out.pushDepth();

	  if (localDstSetter != null) {
	    out.print(value);
	  }
	  else {
	    out.print("((" + targetBean.getEJBClass().getName() + ") ");
	    out.print("((com.caucho.ejb.entity.EntityObject) " + value + ")._caucho_getBean(_ejb_trans, false))");
	  }
	  
	  out.println("." + dstSetter.getName() + "((" + localType + ") _ejb_context.getEJBLocalObject());");
	  
	  out.popDepth();
	  out.println("}");
    
	  return;
	}
      }
      
      out.println("super." + getImplMethod().getName() + "(" + value + ");");
    }
  }
}
