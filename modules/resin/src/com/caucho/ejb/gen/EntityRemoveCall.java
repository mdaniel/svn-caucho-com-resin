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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import java.lang.reflect.Method;

import java.io.IOException;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.MethodCallChain;

import com.caucho.ejb.cfg.EjbEntityBean;
import com.caucho.ejb.cfg.CmrRelation;
import com.caucho.ejb.cfg.CmrManyToOne;

/**
 * Generates the skeleton for the remove method.
 */
public class EntityRemoveCall extends CallChain {
  private static L10N L = new L10N(EntityRemoveCall.class);

  private EjbEntityBean _bean;
  
  private String _contextClassName;

  private boolean _isCMP;

  public EntityRemoveCall(EjbEntityBean bean,
			  String contextClassName)
  {
    _bean = bean;
    
    _contextClassName = contextClassName;
  }
  
  public void setCMP(boolean isCMP)
  {
    _isCMP = isCMP;
  }

  /**
   * Prints the remove method
   *
   * @param method the remove method
   */
  public void generateCall(JavaWriter out, String retVar,
			   String var, String []args)
    throws IOException
  {
    out.println("Bean ptr = _context._ejb_begin(trans, false, true);");
    
    out.println("ptr.ejbRemove();");

    out.println("ptr._ejb_state = QEntity._CAUCHO_IS_REMOVED;");
  }
}
