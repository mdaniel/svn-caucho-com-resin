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
import com.caucho.ejb.cfg.EjbSessionBean;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Assembles the generator structure.
 */
public class SessionAssembler extends BeanAssembler
{
  private static final L10N L = new L10N(SessionAssembler.class);

  protected final EjbSessionBean _sessionBean;

  public SessionAssembler(EjbSessionBean bean, String fullClassName)
  {
    super(bean, fullClassName);

    _sessionBean = bean;

    setSuperClass();
  }

  /**
   * Sets the superclass.
   */
  protected void setSuperClass()
  {
    _genClass.setSuperClassName("com.caucho.ejb.session.StatefulContext");
  }

  /**
   * Adds the header component.
   */
  public void addHeaderComponent(JClass beanClass,
				 String contextClassName,
				 String implClassName)
  {
    _genClass.addComponent(new SessionBean(_sessionBean,
					   beanClass,
					   contextClassName));
  }

  /**
   * Creates the home view.
   */
  public ViewClass createHomeView(JClass homeClass,
				  String fullClassName,
				  String viewPrefix)
  {
    SessionHomeView homeView = new SessionHomeView(homeClass,
						   fullClassName,
						   viewPrefix,
						   false);
    
    _genClass.addComponent(homeView);

    return homeView;
  }

  /**
   * Creates the home view.
   */
  public ViewClass createView(ArrayList<JClass> apiList,
			      String fullClassName,
			      String viewPrefix)
  {
    SessionView view = new SessionView(apiList,
				       fullClassName,
				       viewPrefix,
				       false);

    _genClass.addComponent(view);

    return view;
  }
}
