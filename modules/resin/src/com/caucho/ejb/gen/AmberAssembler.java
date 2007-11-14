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

import com.caucho.ejb.cfg.*;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Assembles the generator structure.
 */
public class AmberAssembler extends EntityAssembler {
  private static final L10N L = new L10N(AmberAssembler.class);

  public AmberAssembler(EjbEntityBean bean, String fullClassName)
  {
    super(bean, fullClassName);
  }

  /**
   * Adds the header component.
   */
  public void addHeaderComponent(ApiClass beanClass,
				 String contextClassName,
				 String implClassName)
  {
    _entityBean = new AmberBean(beanClass,
				contextClassName,
				implClassName);

    _entityBean.setBean((EjbEntityBean) getBean());
    
    _genClass.addComponent(_entityBean);
  }

  /**
   * Creates the home view.
   */
  public ViewClass createHomeView(ApiClass homeClass,
				  String fullClassName,
				  String viewPrefix)
  {
    EntityHomeView homeView = new EntityHomeView(homeClass,
						 fullClassName,
						 viewPrefix,
						 true);

    _genClass.addComponent(homeView);

    return homeView;
  }

  /**
   * Creates the instance view.
   */
  public ViewClass createView(ArrayList<ApiClass> apiList,
			      String fullClassName,
			      String viewPrefix)
  {
    AmberView view = new AmberView(apiList.get(0),
				   fullClassName,
				   viewPrefix);

    _genClass.addComponent(view);

    return view;
  }
}
