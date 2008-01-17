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

import com.caucho.amber.field.IdField;
import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.ejb.gen21.AbstractQueryMethod;
import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen21.CollectionClass;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Configuration for a one-to-many CMP method.
 */
public class EjbOneToManyMethod extends CmpGetter {
  private static final L10N L = new L10N(EjbOneToManyMethod.class);

  private CmrOneToMany _oneToMany;

  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public EjbOneToManyMethod(EjbView view,
                            ApiMethod apiMethod, ApiMethod implMethod,
                            CmrOneToMany oneToMany)
  {
    super(view, apiMethod, implMethod);

    _oneToMany = oneToMany;
  }

  /**
   * Assembles the bean method.
   */
  public void assembleBean(BeanAssembler beanAssembler, String fullClassName)
    throws ConfigException
  {
    String listName = _oneToMany.getName() + "_List";

    CollectionClass list = new CollectionClass(_oneToMany, listName);

    beanAssembler.addComponent(list);

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
     */
    public void generate(JavaWriter out)
      throws IOException
    {
      String listName = _oneToMany.getName() + "_List";
      String varName = "__caucho_field_" + _oneToMany.getName();

      out.println(listName + " " + varName + ";");
      out.println();

      super.generate(out);
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
      String listName = _oneToMany.getName() + "_List";
      String varName = "__caucho_field_" + _oneToMany.getName();

      out.println("if (" + varName + " != null) {");
      out.println("  " + varName + ".__caucho_init(_ejb_trans.getAmberConnection());");
      out.println("  return " + varName + ";");
      out.println("}");

      out.println();
      out.println("try {");
      out.pushDepth();

      out.println("com.caucho.amber.AmberQuery query;");

      String abstractSchema = _oneToMany.getBean().getAbstractSchemaName();

      out.print("String sql = \"SELECT o." + _oneToMany.getName());
      out.print(" FROM " + abstractSchema + " o");
      out.print(" WHERE ");

      EntityType type = _oneToMany.getBean().getEntityType();
      ArrayList<IdField> keys = type.getId().getKeys();

      for (int i = 0; i < keys.size(); i++) {
        IdField key = keys.get(i);

        if (i != 0)
          out.print(" AND ");

        out.print("o." + key.getName() + "=?" + (i + 1));
      }

      out.println("\";");

      out.println("query = _ejb_trans.getAmberConnection().prepareQuery(sql);");

      EjbConfig config = _oneToMany.getBean().getConfig();

      out.println("int index = 1;");
      for (int i = 0; i < keys.size(); i++) {
        IdField key = keys.get(i);

	Class keyClass = key.getJavaType().getRawType().getJavaClass();
        AbstractQueryMethod.generateSetParameter(out,
                                                 config,
                                                 keyClass,
                                                 "query",
                                                 key.generateGet("super"));
      }

      out.println(varName + " = new " + listName + "(this, query);");

      out.println("return " + varName + ";");

      out.popDepth();
      out.println("} catch (RuntimeException e) {");
      out.println("  throw e;");
      out.println("} catch (java.sql.SQLException e) {");
      out.println("  throw com.caucho.ejb.EJBExceptionWrapper.createRuntime(e);");
      out.println("}");
    }
  }
}
