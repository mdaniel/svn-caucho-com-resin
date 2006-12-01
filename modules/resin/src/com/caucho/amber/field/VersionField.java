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

package com.caucho.amber.field;

import com.caucho.amber.type.RelatedType;
import com.caucho.amber.type.Type;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Configuration for a bean's field
 */
public class VersionField extends PropertyField {
  private static final L10N L = new L10N(VersionField.class);
  protected static final Logger log = Log.open(VersionField.class);

  public VersionField(RelatedType entityType, String name)
    throws ConfigException
  {
    super(entityType, name);

    setLazy(false);
  }

  public VersionField(RelatedType entityType)
  {
    super(entityType);

    setLazy(false);
  }

  /**
   * Generates the increment version.
   */
  public void generateIncrementVersion(JavaWriter out)
    throws IOException
  {
    int dirtyGroup = getIndex() / 64;
    String dirtyVar = "__caucho_dirtyMask_" + dirtyGroup;

    long dirtyMask = 1L << (getIndex() % 64);

    out.println();
    out.println(generateSuperSetter(generateSuperGetter() + " + 1") + ";");

    out.println();
    out.println("long oldMask = " + dirtyVar + ";");
    out.println(dirtyVar + " |= " + dirtyMask + "L;");
    out.println();
    out.println("if (__caucho_session != null && oldMask == 0)");
    out.println("  __caucho_session.update(this);");
  }

  /**
   * Returns the where code
   */
  public String generateMatchArgWhere(String id)
  {
    return getColumn().generateMatchArgWhere(id);
  }

  /**
   * Generates the set version clause.
   */
  public void generateSetVersion(JavaWriter out,
                                 String pstmt,
                                 String index)
    throws IOException
  {
    String value = generateGet("super");
    Type type = getColumn().getType();
    getColumn().generateSet(out, pstmt, index, type.generateIncrementVersion(value));
  }

  /**
   * Generates the update set clause
   */
  public void generateUpdate(CharBuffer sql)
  {
    sql.append(getColumn().generateUpdateSet());
  }

  /**
   * Generates loading cache
   */
  public void generateUpdate(JavaWriter out,
                             String maskVar,
                             String pstmt,
                             String index)
    throws IOException
  {
    int group = getIndex() / 64;

    out.println();
    out.println("if (" + maskVar + "_" + group + " != 0L) {");
    out.pushDepth();

    generateSetVersion(out, pstmt, index);

    out.popDepth();
    out.println("}");
  }
}
