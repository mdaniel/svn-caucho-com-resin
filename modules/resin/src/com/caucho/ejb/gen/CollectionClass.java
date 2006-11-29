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

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.ejb.cfg.CmrManyToMany;
import com.caucho.ejb.cfg.CmrManyToOne;
import com.caucho.ejb.cfg.CmrRelation;
import com.caucho.ejb.cfg.EjbEntityBean;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseClass;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.Set;

/**
 * Generates the skeleton for an Amber-based entity bean.
 */
public class CollectionClass extends BaseClass {
  private final static L10N L = new L10N(CollectionClass.class);

  private CmrRelation _oneToMany;
  
  public CollectionClass(CmrRelation oneToMany,
			 String className)
  {
    _oneToMany = oneToMany;

    setClassName(className);

    JClass retType = oneToMany.getGetter().getReturnType();
    if (retType.isAssignableTo(Set.class))
      setSuperClassName("com.caucho.ejb.entity.CmpSetImpl");
    else
      setSuperClassName("com.caucho.ejb.entity.CmpCollectionImpl");
  }

  /**
   * Generates the list's class content.
   */
  public void generateClassContent(JavaWriter out)
    throws IOException
  {
    generateConstructor(out);
    
    generateAdd(out);
    generateRemove(out);
    
    super.generateClassContent(out);
  }

  /**
   * Generates the list's class content.
   */
  public void generateConstructor(JavaWriter out)
    throws IOException
  {
    EjbEntityBean sourceBean = _oneToMany.getBean();
    String sourceType = sourceBean.getLocal().getName();

    out.println();
    out.println("Bean _bean;");
    out.println(sourceType + " _beanLocal;");
    
    out.println();
    out.println("public " + getClassName() + "(Bean bean, com.caucho.amber.AmberQuery query)");
    out.println("{");
    out.pushDepth();
    out.println("_bean = bean;");
    out.println("_beanLocal = bean._ejb_context._viewLocal;");
    out.println("fill(query);");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the list's class content.
   */
  public void generateAdd(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public boolean addImpl(Object v)");
    out.println("{");
    out.pushDepth();
    
    EjbEntityBean targetBean = _oneToMany.getTargetBean();
    String targetType = targetBean.getLocal().getName();

    out.println("if (v == null)");
    out.println("  return false;");
    out.println("else if (! (v instanceof " + targetType + "))");
    out.println("  throw new IllegalArgumentException(v.getClass().getName() + \": \" + v);");

    CmrRelation targetRelation = _oneToMany.getTargetRelation();

    if (targetRelation instanceof CmrManyToOne) {
      CmrManyToOne manyToOne = (CmrManyToOne) targetRelation;

      JMethod setter = manyToOne.getSetter();

      if (setter != null) {
	out.println(targetType + " bean = (" + targetType + ") v;");

	JMethod localDstSetter = targetBean.getMethod(targetBean.getLocal(),
						      setter);

	if (localDstSetter != null) {
	  out.print("bean");
	}
	else {
	  out.print("((" + targetBean.getEJBClass().getName() + ") ");
	  out.print("((com.caucho.ejb.entity.EntityObject) bean)._caucho_getBean(_ejb_trans, true))");
	}
	
	out.println("." + setter.getName() + "(_beanLocal);");
      }
    }
    else if (_oneToMany instanceof CmrManyToMany) {
      CmrManyToMany manyToMany = (CmrManyToMany) _oneToMany;

      JMethod getter = manyToMany.getGetter();

      if (manyToMany.isTargetUnique())
	out.println("_bean.__amber_" + getter.getName() + "_remove_target(v);");
      
      out.println("_bean.__amber_" + getter.getName() + "_add(v);");
    }

    out.println("return true;");
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the list's class content.
   */
  public void generateRemove(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("protected boolean removeImpl(Object v)");
    out.println("{");
    out.pushDepth();
    
    EjbEntityBean targetBean = _oneToMany.getTargetBean();
    String targetType = targetBean.getLocal().getName();

    out.println("if (v == null)");
    out.println("  return false;");
    out.println("else if (! (v instanceof " + targetType + "))");
    out.println("  throw new IllegalArgumentException(v.getClass().getName() + \": \" + v);");

    CmrRelation targetRelation = _oneToMany.getTargetRelation();

    if (targetRelation instanceof CmrManyToOne) {
      CmrManyToOne manyToOne = (CmrManyToOne) targetRelation;

      JMethod setter = manyToOne.getSetter();
      JMethod getter = manyToOne.getGetter();

      if (setter != null) {
	out.println("if (_bean != null) {");
	out.pushDepth();
	
	out.println(targetType + " bean = (" + targetType + ") v;");
	
	out.println("if (_beanLocal != null) {");
	out.pushDepth();

	JMethod localDstSetter = targetBean.getMethod(targetBean.getLocal(),
						      setter);
	String bean = "bean";

	if (localDstSetter == null) {
	  String beanClass = targetBean.getEJBClass().getName();
	  
	  out.print(beanClass + " bean1 = ((" + beanClass + ") ");
	  out.print("((com.caucho.ejb.entity.EntityObject) bean)._caucho_getBean(_ejb_trans, true));");

	  bean = "bean1";
	}
	
	out.println("if (_beanLocal.equals(" + bean + "." + getter.getName() + "())) {");
	out.pushDepth();
	
	out.println(bean + "." + setter.getName() + "(null);");
    
	out.popDepth();
	out.println("}");
    
	out.popDepth();
	out.println("}");
	
	out.popDepth();
	out.println("}");
	out.println();
      }
    }
    else if (_oneToMany instanceof CmrManyToMany) {
      CmrManyToMany manyToMany = (CmrManyToMany) _oneToMany;

      JMethod getter = manyToMany.getGetter();

      out.println("_bean.__amber_" + getter.getName() + "_remove(v);");
    }

    out.println("return true;");
    
    out.popDepth();
    out.println("}");
  }
}
