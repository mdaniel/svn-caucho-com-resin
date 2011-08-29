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
import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.MethodCallChain;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for the create method.
 */
public class EntityCreateCall extends CallChain {
  private static L10N L = new L10N(EntityCreateMethod.class);

  private EjbEntityBean _bean;
  
  private ApiMethod _createMethod;
  private ApiMethod _postCreateMethod;

  private CallChain _createCall;
  private CallChain _postCreateCall;
  
  private ApiClass _primKeyClass;
  private String _contextClassName;

  private boolean _isCMP;

  public EntityCreateCall(EjbEntityBean bean,
			  ApiMethod createMethod,
			  ApiMethod postCreateMethod,
			  String contextClassName)
  {
    _bean = bean;
    
    _createMethod = createMethod;
    _postCreateMethod = postCreateMethod;
    
    _createCall = new MethodCallChain(_createMethod.getMethod());
    
    if (_postCreateMethod != null)
      _postCreateCall = new MethodCallChain(_postCreateMethod.getMethod());
    
    _contextClassName = contextClassName;
  }
  
  public void setCMP(boolean isCMP)
  {
    _isCMP = isCMP;
  }

  /**
   * Prints the create method
   *
   * @param method the create method
   */
  public void generateCall(JavaWriter out, String retVar,
			   String var, String []args)
    throws IOException
  {
    if (_isCMP) {
      out.println("try {");
      out.pushDepth();
    }
    
    // out.println("Bean home = _ejb_begin(trans, true, false);");
    out.println();
    out.println("Bean bean = new Bean(cxt);");
    out.println("bean._ejb_trans = trans;");
    out.println("bean._ejb_flags = 1;");
    
    out.printClass(_createCall.getReturnType());
    out.println(" key;");

    _createCall.generateCall(out, "key", "bean", args);

    if (_isCMP) {
      String name = _bean.getEntityType().getName();
      out.println("trans.getAmberConnection().create(\"" + name + "\", bean);");
      out.println("cxt.__amber_cacheItem = bean.__caucho_cacheItem;");
      out.println("Object okey = bean.__caucho_getPrimaryKey();");
      out.println("cxt.postCreate(okey);");
    }

    out.println("trans.addObject(bean);");

    Class retType = _createCall.getReturnType();
    if (_isCMP) {
    }
    else if (! retType.isPrimitive())
      out.println("cxt.postCreate(key);");
    else {
      out.print("Object okey = ");
      out.printJavaTypeToObject("key", retType);
      out.println(";");
      
      out.println("cxt.postCreate(okey);");
    }

    // if (! _gen.isEntityCMP()) {
    out.println("if (__caucho_log.isLoggable(java.util.logging.Level.FINE))");
    out.println("  __caucho_log.fine(bean.__caucho_id + \":create(\" + key + \")\");");
    out.println();
    
    out.println("bean._ejb_state = QEntity._CAUCHO_IS_LOADED;");
    //println("bean._ejb_load_time = com.caucho.util.Alarm.getCurrentTime();");

    if (_postCreateCall != null)
      _postCreateCall.generateCall(out, null, "bean", args);
    
    if (_isCMP) {
      out.popDepth();
      out.println("} catch (java.sql.SQLException e) {");
      out.println("  throw new com.caucho.ejb.CreateExceptionWrapper(e);");
      out.println("}");
    }
  }
}
