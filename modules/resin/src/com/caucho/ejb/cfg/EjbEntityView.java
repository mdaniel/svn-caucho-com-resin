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

package com.caucho.ejb.cfg;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.java.gen.CallChain;

import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.ejb.gen.EntityRemoveMethod;
import com.caucho.ejb.gen.TransactionChain;

/**
 * Configuration for a cmp view.
 */
public class EjbEntityView extends EjbObjectView {
  private static final Logger log = Log.open(EjbEntityView.class);
  private static final L10N L = new L10N(EjbEntityView.class);

  /**
   * Creates a new entity bean configuration.
   */
  public EjbEntityView(EjbEntityBean bean, JClass apiClass, String prefix)
    throws ConfigException
  {
    super(bean, apiClass, prefix);
  }

  protected EjbEntityBean getEntityBean()
  {
    return (EjbEntityBean) getBean();
  }

  /**
   * Assembles the generator methods.
   */
  protected void assembleMethods(BeanAssembler assembler,
				 ViewClass viewClass,
				 String fullClassName)
    throws ConfigException
  {
    super.assembleMethods(assembler, viewClass, fullClassName);

    JMethod removeApiMethod = null;

    removeApiMethod = getApiClass().getMethod("remove", new JClass[0]);

    if (removeApiMethod != null) {
      EntityRemoveMethod removeMethod;

      removeMethod = new EntityRemoveMethod(getEntityBean(),
					    removeApiMethod,
					    fullClassName);

      CallChain call = removeMethod.getCall();

      // XXX: can be supports if there's no auto-update
      call = TransactionChain.create(call, EjbMethod.TRANS_REQUIRED);

      removeMethod.setCall(call);

      viewClass.addMethod(removeMethod);
    }
  }
}
