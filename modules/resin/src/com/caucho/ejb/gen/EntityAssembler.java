/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;

import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.BaseClass;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.ClassComponent;

import com.caucho.ejb.cfg.EjbEntityBean;

/**
 * Assembles the generator structure.
 */
public class EntityAssembler extends BeanAssembler {
  private static final L10N L = new L10N(EntityAssembler.class);

  private EjbEntityBean _bean;
  
  protected EntityBean _entityBean;
  
  public EntityAssembler(EjbEntityBean bean, String fullClassName)
  {
    super(bean, fullClassName);

    _bean = bean;

    setSuperClass();
  }

  /**
   * Sets the superclas.
   */
  protected void setSuperClass()
  {
    _genClass.setSuperClassName("QEntityContext");
  }

  /**
   * Adds the header component.
   */
  public void addHeaderComponent(JClass beanClass,
				 String contextClassName,
				 String implClassName)
  {
    _entityBean = new EntityBean(beanClass,
				 contextClassName,
				 implClassName);
    _entityBean.setBean(_bean);
    
    _genClass.addComponent(_entityBean);
  }

  /**
   * Returns the entity bean.
   */
  public EjbEntityBean getBean()
  {
    return _bean;
  }

  /**
   * Adds the bean method
   */
  public void addMethod(BaseMethod method)
  {
    _entityBean.addMethod(method);
  }

  /**
   * Adds the bean component
   */
  public void addComponent(ClassComponent component)
  {
    _entityBean.addComponent(component);
  }

  /**
   * Creates the home view.
   */
  public ViewClass createHomeView(JClass homeClass,
				  String fullClassName,
				  String viewPrefix)
  {
    EntityHomeView homeView = new EntityHomeView(homeClass,
						 fullClassName,
						 viewPrefix,
						 false);

    _genClass.addComponent(homeView);

    return homeView;
  }

  /**
   * Creates the home view.
   */
  public ViewClass createView(JClass homeClass,
			      String fullClassName,
			      String viewPrefix)
  {
    EntityView view = new EntityView(homeClass,
				     fullClassName,
				     viewPrefix);

    _genClass.addComponent(view);

    return view;
  }
}
