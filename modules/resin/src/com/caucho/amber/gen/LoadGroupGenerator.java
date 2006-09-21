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

package com.caucho.amber.gen;

import java.io.IOException;

import java.util.ArrayList;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.ClassComponent;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.LinkColumns;

import com.caucho.bytecode.JMethod;

/**
 * Generates the Java code for the wrapped object.
 */
public class LoadGroupGenerator extends ClassComponent {
  private static final L10N L = new L10N(LoadGroupGenerator.class);

  private String _extClassName;
  private EntityType _entityType;
  private int _index;

  public LoadGroupGenerator(String extClassName,
                            EntityType entityType,
                            int index)
  {
    _extClassName = extClassName;
    _entityType = entityType;
    _index = index;
  }

  /**
   * Generates the load group.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("protected void __caucho_load_" + _index +  "(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("{");
    out.println("  __caucho_load_" + _index + "(aConn, null);");
    out.println("}");

    out.println();
    out.println("protected void __caucho_load_" + _index +  "(com.caucho.amber.manager.AmberConnection aConn, java.util.Map preloadedProperties)");
    out.println("{");
    out.pushDepth();

    int group = _index / 64;
    long mask = (1L << (_index % 64));

    // non-read-only entities must be reread in a transaction
    if (! _entityType.isReadOnly()) {
      out.println("if (aConn.isInTransaction()) {");
      out.println("  if (com.caucho.amber.entity.Entity.P_DELETING <= __caucho_state) {");
      out.println("    return;");
      out.println("  }");
      out.println("  else if (__caucho_state < com.caucho.amber.entity.Entity.P_TRANSACTIONAL) {");
      out.println("    __caucho_state = com.caucho.amber.entity.Entity.P_TRANSACTIONAL;");

      int loadCount = _entityType.getLoadGroupIndex();
      for (int i = 0; i <= loadCount / 64; i++) {
        out.println("    __caucho_loadMask_" + i + " = 0;");
      }
      int dirtyCount = _entityType.getDirtyIndex();
      for (int i = 0; i <= dirtyCount / 64; i++) {
        out.println("    __caucho_dirtyMask_" + i + " = 0;");
      }

      out.println("    aConn.makeTransactional(this);");
      out.println("  }");
      out.println("  else if ((__caucho_loadMask_" + group + " & " + mask + "L) != 0)");
      out.println("    return;");
      out.println("}");
      out.print("else ");
    }

    out.println("if ((__caucho_loadMask_" + group + " & " + mask + "L) != 0)");
    out.println("  return;");

    // XXX: the load doesn't cover other load groups
    out.println("else if (__caucho_item != null) {");
    out.pushDepth();
    out.println(_extClassName + " item = (" + _extClassName + ") __caucho_item.getEntity();");

    out.println("item.__caucho_load_" + _index + "(aConn);");

    _entityType.generateCopyLoadObject(out, "super", "item", _index);

    // out.println("__caucho_loadMask_" + group + " |= " + mask + "L;");
    out.println("__caucho_loadMask_" + group + " |= item.__caucho_loadMask_" + group + ";"); // mask + "L;");

    out.println();
    out.println("return;");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("try {");
    out.pushDepth();

    Table table = _entityType.getTable();

    String from = null;
    String select = null;
    String where = null;

    String subSelect = _entityType.generateLoadSelect(table, "o", _index);
    Table mainTable = null;
    String tableName = null;

    if (subSelect != null) {
      select = subSelect;

      from = table.getName() + " o";
      where = _entityType.getId().generateMatchArgWhere("o");

      mainTable = table;
      tableName = "o";
    }

    ArrayList<Table> subTables = _entityType.getSecondaryTables();

    for (int i = 0; i < subTables.size(); i++) {
      Table subTable = subTables.get(i);

      subSelect = _entityType.generateLoadSelect(subTable, "o" + i, _index);

      if (subSelect == null)
        continue;

      if (select != null)
        select = select + ", " + subSelect;
      else
        select = subSelect;

      if (from != null)
        from = from + ", " + subTable.getName() + " o" + i;
      else
        from = subTable.getName() + " o" + i;

      if (where != null) {
        LinkColumns link = subTable.getDependentIdLink();

        where = where + " and " + link.generateJoin("o" + i, "o");
      }
      else
        throw new IllegalStateException();
    }

    if (select == null)
      select = "1";

    if (where == null) {
      from = table.getName() + " o";

      where = _entityType.getId().generateMatchArgWhere("o");
    }

    String sql = "select " + select + " from " + from + " where " + where;

    out.println("String sql = \"" + sql + "\";");

    out.println();
    out.println("java.sql.PreparedStatement pstmt = aConn.prepareStatement(sql);");

    out.println("int index = 1;");
    _entityType.getId().generateSet(out, "pstmt", "index", "super");

    out.println();
    out.println("java.sql.ResultSet rs = pstmt.executeQuery();");

    out.println("if (rs.next()) {");
    out.pushDepth();
    _entityType.generateLoad(out, "rs", "", 1, _index);
    out.println("__caucho_loadMask_" + group + " |= " + mask + "L;");

    _entityType.generateLoadEager(out, "rs", "", 1, _index);

    // commented out: jpa/0r01
    // ArrayList<JMethod> postLoadCallbacks = _entityType.getPostLoadCallbacks();
    // if (postLoadCallbacks.size() > 0 && _index == 0) {
    //   out.println("if (__caucho_state == com.caucho.amber.entity.Entity.P_TRANSACTIONAL) {");
    //   out.pushDepth();
    //   generateCallbacks(out, postLoadCallbacks);
    //   out.popDepth();
    //   out.println("}");
    // }

    if (_entityType.getHasLoadCallback())
      out.println("__caucho_load_callback();");

    out.popDepth();
    out.println("}");
    out.println("else {");
    out.println("  rs.close();");

    String errorString = ("(\"amber load: no matching object " +
                          _entityType.getName() + "[\" + __caucho_getPrimaryKey() + \"]\")");

    out.println("  throw new com.caucho.amber.AmberObjectNotFoundException(" + errorString + ");");
    out.println("}");

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  throw new com.caucho.amber.AmberRuntimeException(e);");
    out.println("}");

    // needs to be after load to prevent loop if toString() expects data
    out.println();
    out.println("if (__caucho_log.isLoggable(java.util.logging.Level.FINE))");
    out.println("  __caucho_log.fine(\"amber loaded-" + _index + " \" + this);");

    out.popDepth();
    out.println("}");

    if (_index == 0 && _entityType.getHasLoadCallback()) {
      out.println();
      out.println("protected void __caucho_load_callback() {}");
    }
  }

  private void generateCallbacks(JavaWriter out, ArrayList<JMethod> callbacks)
    throws IOException
  {
    if (callbacks.size() == 0)
      return;

    out.println();
    for (JMethod method : callbacks) {
      out.println(method.getName() + "();");
    }
  }
}
