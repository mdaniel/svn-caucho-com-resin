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

import com.caucho.ejb.gen.*;
import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.ClassComponent;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for an Amber-based entity bean.
 */
public class AmberBean extends EntityBean {
  private final static L10N L = new L10N(AmberBean.class);
  
  public AmberBean(ApiClass ejbClass,
		   String contextClassName,
		   String implClassName)
  {
    super(ejbClass, contextClassName, implClassName);

    addComponent(new FlushMethod());

    if (ejbClass.hasMethod("ejbLoad", new Class[0])) {
      addComponent(new LoadMethod());
    }

    boolean hasRemove = ejbClass.hasMethod("ejbRemove", new Class[0]);
    
    addComponent(new RemoveMethod(hasRemove));
  }

  /**
   * Returns true for CMP.
   */
  protected boolean isCMP()
  {
    return true;
  }

  /**
   * Generates the amber context stuff.
   */
  protected void generateContext(JavaWriter out)
    throws IOException
  {
    super.generateContext(out);

    out.println();
    out.println("private com.caucho.amber.entity.EntityItem  __amber_cacheItem;");

    out.println();
    out.println("protected final void __caucho_setAmberCacheItem(com.caucho.amber.entity.EntityItem amber)");
    out.println("{");
    out.println("  __amber_cacheItem = amber;");
    out.println("}");
    
    out.println();
    out.println("protected final com.caucho.amber.entity.EntityItem __caucho_getAmberCacheItem()");
    out.println("{");
    out.println("  if (__amber_cacheItem == null)");
    out.println("    __amber_cacheItem = _server.getAmberCacheItem(getPrimaryKey());");
    out.println("  return __amber_cacheItem;");
    out.println("}");
    
    out.println();
    out.println("protected final " + getImplClassName() + " __caucho_getAmberCacheEntity()");
    out.println("{");
    out.println("  if (__amber_cacheItem == null)");
    out.println("    __amber_cacheItem = _server.getAmberCacheItem(getPrimaryKey());");
    out.println("  return (" + getImplClassName() + ") __amber_cacheItem.getEntity();");
    out.println("}");
  }

  /**
   * Generates the load code.
   */
  protected void generateStore(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the load code.
   */
  protected void generateLoad(JavaWriter out)
    throws IOException
  {
    out.println("if (doLoad) {");
    out.println("  try {");
    //out.println("    ptr.__caucho_makePersistent(trans.getAmberConnection(), __caucho_getAmber());");
    out.println("    ptr.__caucho_retrieve_eager(trans.getAmberConnection());");

    /*
    if (hasMethod("ejbLoad", new Class[0]))
      out.println("    ptr.ejbLoad();");
    */
      
    out.println("  } catch (Exception e) { throw com.caucho.ejb.EJBExceptionWrapper.createRuntime(e); }");
    // out.println("  ptr._ejb_state = QEntity._CAUCHO_IS_DIRTY;");
    out.println("  ptr._ejb_state = QEntity._CAUCHO_IS_LOADED;");
    out.println("}");
  }

  private static boolean hasMethod(Class cl, String name, Class []param)
  {
    try {
      return cl.getMethod(name, param) != null;
    } catch (Throwable e) {
      return false;
    }
  }

  static class FlushMethod extends ClassComponent {
    public void generate(JavaWriter out)
      throws IOException
    {
      out.println("protected void __caucho_flush_callback()");
      out.println("  throws java.sql.SQLException");
      out.println("{");
      out.println("  ejbStore();");
      out.println("}");
    }
  }

  static class LoadMethod extends ClassComponent {
    public void generate(JavaWriter out)
      throws IOException
    {
      out.println("protected void __caucho_load_callback()");
      out.println("{");
      out.println("  try {");
      out.println("    ejbLoad();");
      out.println("  } catch (Throwable e) {");
      out.println("    __caucho_log.log(java.util.logging.Level.WARNING, e.toString(), e);");
      out.println("  }");
      out.println("}");
    }
  }

  static class RemoveMethod extends ClassComponent {
    private boolean _hasRemove;

    RemoveMethod(boolean hasRemove)
    {
      _hasRemove = hasRemove;
    }
    
    public void generate(JavaWriter out)
      throws IOException
    {
      out.println("public void ejbRemove()");
      out.println("{");
      out.pushDepth();
      
      out.println("try {");
      out.pushDepth();

      if (_hasRemove)
	out.println("super.ejbRemove();");

      out.println("_ejb_state = com.caucho.ejb.entity.EntityObject._CAUCHO_IS_DEAD;");

      //out.println("_ejb_trans.getAmberConnection().delete(this);");
      out.println("__caucho_delete();");

      out.popDepth();
      out.println("} catch (Exception e) {");
      out.println("  __caucho_log.log(java.util.logging.Level.WARNING, e.toString(), e);");
      out.println("}");

      out.popDepth();
      out.println("}");
    }
  }
}
