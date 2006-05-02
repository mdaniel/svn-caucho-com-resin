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

import java.util.ArrayList;

import java.io.IOException;

import javax.ejb.EntityContext;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassWrapper;
import com.caucho.bytecode.JClassLoader;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.ClassComponent;
import com.caucho.java.gen.BaseClass;
import com.caucho.java.gen.BaseMethod;

import com.caucho.ejb.cfg.EjbEntityBean;

/**
 * Generates the skeleton for a session bean.
 */
public class EntityBean extends ClassComponent {
  private final static L10N L = new L10N(EntityBean.class);

  private EjbEntityBean _bean;
  private JClass _ejbClass;
  protected String _implClassName;
  protected String _contextClassName;

  protected BaseClass _beanClass;
  
  public EntityBean(JClass ejbClass,
		    String contextClassName,
		    String implClassName)
  {
    _ejbClass = ejbClass;
    _contextClassName = contextClassName;
    _implClassName = implClassName;

    _beanClass = new BeanImpl();
  }

  /**
   * Sets the bean.
   */
  public void setBean(EjbEntityBean bean)
  {
    _bean = bean;
  }

  /**
   * Gets the bean.
   */
  public EjbEntityBean getBean()
  {
    return _bean;
  }

  /**
   * Returns the implementation class name.
   */
  protected String getImplClassName()
  {
    return _implClassName;
  }

  /**
   * Returns true for CMP.
   */
  protected boolean isCMP()
  {
    return false;
  }

  /**
   * Generates the general entity information.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    out.println("protected static final java.util.logging.Logger __caucho_log = java.util.logging.Logger.getLogger(\"" + _contextClassName + "\");");
    
    generateContext(out);
    
    _beanClass.generate(out);
  }

  protected void generateContext(JavaWriter out)
    throws IOException
  {
    String shortContextName = _contextClassName;

    int p = shortContextName.lastIndexOf('.');
    if (p > 0)
      shortContextName = shortContextName.substring(p + 1);
    
    out.println();
    out.println("Bean _ejb_free;");
    out.println();
    out.println("public " + shortContextName + "(EntityServer server)");
    out.println("{");
    out.println("  super(server);");
    out.println("}");

    out.println();
    out.println("Bean _ejb_begin(TransactionContext trans, boolean isHome, boolean doLoad)");
    out.println("{");
    out.pushDepth();
    
    out.println("if (trans == null || isDead()) throw new IllegalStateException();");
    out.println();
    out.println("Bean ptr;");
    out.println("if (isHome)");
    out.println("  ptr = (Bean) trans.getEntity(_server, null);");
    out.println("else");
    out.println("  ptr = (Bean) trans.getEntity(_server, getPrimaryKey());");
    out.println("if (ptr != null)");
    out.println("  return ptr;");
    
    // Otherwise create the bean
    out.println("synchronized (this) {");
    out.println("  ptr = _ejb_free;");
    out.println("  _ejb_free = null;");
    out.println("}");

    out.println("if (ptr == null) {");
    out.pushDepth();
    out.println("ptr = new Bean(this);");

    if (BeanAssembler.hasMethod(_ejbClass, "ejbActivate", new JClass[0])) {
      out.println("if (! isHome)");
      out.println("  try { ptr.ejbActivate(); } catch (Exception e) { throw com.caucho.ejb.EJBExceptionWrapper.createRuntime(e); }");
    }
    out.popDepth();
    out.println("}");
    
    out.println();
    out.println("ptr._ejb_trans = trans;");
    out.println("trans.addObject(ptr);");

    generateLoad(out);

    out.println("return ptr;");

    out.popDepth();
    out.println("}");


    // XXX: need to optimize when no other transaction
    
    out.println();
    out.println("public void _caucho_load()");
    out.println("  throws FinderException");
    out.println("{");
    out.pushDepth();
    out.println("TransactionContext trans = _server.getTransactionManager().beginSupports();");
    out.println("try {");
    out.pushDepth();

    out.println("Bean bean = _ejb_begin(trans, false, true);");

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  trans.setRollbackOnly(e);");
    out.println("  throw FinderExceptionWrapper.create(e);");
    out.println("} finally {");
    out.println("  trans.commit();");
    out.println("}");
    out.popDepth();
    out.println("}");
    
    out.println();
    out.println("public void update()");
    out.println("{");
    out.println("}");
    
    out.println();
    out.println("public void _caucho_killCache()");
    out.println("  throws javax.ejb.RemoveException");
    out.println("{");
    out.println("}");

    generateDestroy(out);
  }

  /**
   * Generates the cleanup code when the object is removed.
   */
  protected void generateDestroy(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void destroy()");
    out.println("  throws Exception");
    out.println("{");
    out.pushDepth();
    out.println("Bean bean;");
    out.println("synchronized (this) {");
    out.pushDepth();

    /*
    if (_localClass != null) {
      println("if (viewLocal != null)");
      println("  viewLocal.destroy();");
      println("viewLocal = null;");
    }
    
    if (_remoteClass != null) {
      println("if (viewRemote != null)");
      println("  viewRemote.destroy();");
      println("viewRemote = null;");
    }
    */
    out.println("super.destroy();");
    out.println();
    out.println("bean = _ejb_free;");
    out.println("_ejb_free = null;");
    out.popDepth();
    out.println("}");

    out.println();
    out.println("if (bean != null) {");
    out.pushDepth();
    
    if (hasMethod("ejbPassivate", new JClass[0])) {
      out.println("if (bean._ejb_state > QEntity._CAUCHO_IS_HOME)");
      out.println("  bean.ejbPassivate();");
    }
    
    if (hasMethod("unsetEntityContext", new JClass[0]))
      out.println("bean.unsetEntityContext();");
    
    out.popDepth();
    out.println("}");
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Adds a bean method.
   */
  void addMethod(BaseMethod method)
  {
    if (method != null)
      _beanClass.addMethod(method);
  }

  /**
   * Adds a bean component.
   */
  void addComponent(ClassComponent component)
  {
    if (component != null)
      _beanClass.addComponent(component);
  }

  /**
   * Returns true if the method is implemented.
   */
  protected boolean hasMethod(String methodName, JClass []paramTypes)
  {
    return BeanAssembler.hasMethod(_ejbClass, methodName, paramTypes);
  }

  /**
   * Returns true if the method is implemented.
   */
  protected boolean hasMethod(JClass ejbClass, String methodName, JClass []paramTypes)
  {
    return BeanAssembler.hasMethod(ejbClass, methodName, paramTypes);
  }

  /**
   * Generates the load code.
   */
  protected void generateLoad(JavaWriter out)
    throws IOException
  {
    if (hasMethod("ejbLoad", new JClass[0])) {
      out.println("if (doLoad) {");
      out.println("  try {");
      if (hasMethod("ejbLoad", new JClass[0]))
	out.println("    ptr.ejbLoad();");
      
      out.println("  } catch (Exception e) { throw com.caucho.ejb.EJBExceptionWrapper.createRuntime(e); }");
      // out.println("  ptr._ejb_state = QEntity._CAUCHO_IS_DIRTY;");
      out.println("  ptr._ejb_state = QEntity._CAUCHO_IS_LOADED;");
      out.println("}");
    }
  }

  class BeanImpl extends BaseClass {
    BeanImpl()
    {
      super("Bean", _implClassName);

      addInterfaceName("QEntity");

      setStatic(true);
    }
    
    public void generateClassContent(JavaWriter out)
      throws IOException
    {
      out.println();
      out.println("protected final static java.util.logging.Logger __caucho_log = com.caucho.log.Log.open(" + _ejbClass.getName() + ".class);");
      out.println("private static int __caucho_dbg_id;");
      out.println("private final String __caucho_id;");
    
      out.println("Bean _ejb_next;");

      out.println(_contextClassName + " _ejb_context;");
      out.println("TransactionContext _ejb_trans;");
      out.println("byte _ejb_state;");
      out.println("byte _ejb_flags;");

      out.println();
      getBean().generateBeanPrologue(out);

      out.println();
      out.println("Bean(" + _contextClassName + " context)");
      out.println("{");
      out.pushDepth();

      out.println("__caucho_id = \"\" + __caucho_dbg_id++ + \":" + _ejbClass.getName() + "\";");

      out.println("try {");
      out.println("  _ejb_context = context;");
      out.println("  _ejb_state = QEntity._CAUCHO_IS_HOME;");
    
      if (BeanAssembler.hasMethod(_ejbClass, "setEntityContext",
				  new JClass[] {
				    JClassLoader.systemForName(EntityContext.class.getName())
				  })) {
	out.println("  setEntityContext(context);");
      }
    
      out.println("} catch (Exception e) {");
      out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
      out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
      out.println("}");

      out.popDepth();
      out.println("}");

      if (BeanAssembler.hasMethod(_ejbClass, "ejbActivate", new JClass[0])) {
	out.println();
	out.println("public void ejbActivate()");
	out.println("{");
	out.println("  if (__caucho_log.isLoggable(java.util.logging.Level.FINE))");
	out.println("    __caucho_log.fine(__caucho_id + \":activate()\");");
	out.println();
	out.println("  _ejb_state = _CAUCHO_IS_ACTIVE;");
	/*
	  if (getContainerManagedPersistence())
	  out.println("  _caucho_setPrimaryKey(_ejb_context.getPrimaryKey());");
	*/
	out.println("  try {super.ejbActivate(); } catch (Exception e) { throw com.caucho.ejb.EJBExceptionWrapper.createRuntime(e); }");
	out.println("}");
      }
    
      if (BeanAssembler.hasMethod(_ejbClass, "ejbPassivate", new JClass[0])) {
	out.println();
	out.println("public void ejbPassivate()");
	out.println("{");
	out.println("  if (__caucho_log.isLoggable(java.util.logging.Level.FINE))");
	out.println("    __caucho_log.fine(__caucho_id + \":passivate()\");");
	out.println("  super.ejbPassivate();");
	out.println("}");
      }

      out.println();
      out.println("public boolean _caucho_isMatch(com.caucho.ejb.AbstractServer server, Object primaryKey)");
      out.println("{");
      out.println("  if (server != _ejb_context.getServer())");
      out.println("    return false;");
      out.println("  Object key = _ejb_context._caucho_getPrimaryKey();");
      out.println("  return (primaryKey == key || primaryKey != null && primaryKey.equals(key));");
      out.println("}");

      generateSync(out);
      generateBeforeCompletion(out);
      generateAfterCompletion(out);

      super.generateClassContent(out);
    }
  }

  /**
   * Generates the sync code.
   */
  protected void generateSync(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void _caucho_sync()");
    out.println("{");

    if (BeanAssembler.hasMethod(_ejbClass, "ejbStore", new JClass[0])) {
      out.pushDepth();
      out.println("if (this._ejb_state >= _CAUCHO_IS_ACTIVE) {");
      // if (! getContainerManagedPersistence())
      out.println("  this._ejb_state = _CAUCHO_IS_LOADED;");
      out.println("  ejbStore();");
      out.println("}");
      out.popDepth();
    }

    out.println("}");
  }

  /**
   * Generates the before transaction completion code.
   */
  protected void generateBeforeCompletion(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public void _caucho_beforeCompletion(boolean isCommit)");
    out.println("{");

    if (BeanAssembler.hasMethod(_ejbClass, "ejbStore", new JClass[0])) {
      out.pushDepth();
      
      generateStore(out);
      
      out.popDepth();
    }
    out.println("}");
  }

  /**
   * Generates the after transaction completion code.
   */
  protected void generateAfterCompletion(JavaWriter out)
    throws IOException
  {

    // Frees the bean after it's done with the transaction.
    out.println();
    out.println("public void _caucho_afterCompletion(boolean isCommit)");
    out.println("{");
    out.pushDepth();
    out.println(_contextClassName + " cxt = (" + _contextClassName + ") _ejb_context;");
    out.println();
    out.println("boolean isDead = false;");

    if (isCMP()) {
      out.println("if (isCommit)");
      out.println("  __caucho_afterCommit();");
      out.println("else");
      out.println("  __caucho_afterRollback();");
    }

    out.println("if (! isCommit && _ejb_flags != 0) {");
    out.println("  _ejb_state = QEntity._CAUCHO_IS_DEAD;");
    out.println("  cxt.getEntityServer().removeCache(cxt.getPrimaryKey());");
    out.println("}");
    out.println("else if (_ejb_state == QEntity._CAUCHO_IS_REMOVED) {");
    out.println("  cxt.getEntityServer().removeCache(cxt.getPrimaryKey());");
    
    if (isCMP())
      out.println("  _ejb_context._amber = null;");
    
    out.println("  try {");
    out.println("    cxt.destroy();");
    out.println("  } catch (Exception e) {");
    out.println("  }");
      
    out.println("}");
    out.println("else {");
    out.pushDepth();

    if (isCMP()) {
      out.println("if (_ejb_context._amber == null)");
      out.println("  _ejb_context._amber = __caucho_item;");

      _bean.generateAfterCommit(out);
    }
    out.println("_ejb_trans = null;");
    out.println("synchronized (this) {");
    out.println("  if (_ejb_context._ejb_free == null) {");
    // ejb/0252
    // out.println("    __caucho_expire();"); // ejb/0aje
    out.println("    _ejb_context._ejb_free = this;");
    out.println("    return;");
    out.println("  }");
    out.println("}");
    
    if (hasMethod("ejbPassivate", new JClass[0])) {
      out.println("if (_ejb_state > QEntity._CAUCHO_IS_HOME)");
      out.println("  ejbPassivate();");
    }
    
    if (hasMethod("unsetEntityContext", new JClass[0])) {
      out.println();
      out.println("unsetEntityContext();");
    }
      
    out.popDepth();
    out.println("}");
    out.popDepth();
    out.println("}");
  }
  
  /**
   * Generates the store code.
   */
  protected void generateStore(JavaWriter out)
    throws IOException
  {
    out.println("if (_ejb_state >= _CAUCHO_IS_LOADED && isCommit) {");
    out.println("  _ejb_state = _CAUCHO_IS_LOADED;");
    out.println("  ejbStore();");
    out.println("}");
  }
}
