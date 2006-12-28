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

import com.caucho.amber.type.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.HashSet;

/**
 * Generates the Java code for the wrapped object.
 */
public class MappedSuperclassComponent extends AmberMappedComponent {
  private static final L10N L = new L10N(MappedSuperclassComponent.class);

  public MappedSuperclassComponent()
  {
  }

  /**
   * Gets the mapped superclass type.
   */
  public MappedSuperclassType getMappedSuperclassType()
  {
    return (MappedSuperclassType) _relatedType;
  }

  /**
   * Sets the bean info for the generator
   */
  public void setMappedSuperclassType(MappedSuperclassType mappedSuperclassType)
  {
    setRelatedType(mappedSuperclassType);
  }

  /**
   * Starts generation of the Java code
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    try {
      generateHeader(out, false);

      generateInit(out, false);

      HashSet<Object> completedSet = new HashSet<Object>();

      generatePrologue(out, completedSet);

      generateGetEntityType(out);

      generateGetEntityState(out);

      generateMatch(out, false);

      generateFields(out);

      generateMethods(out);

      generateDetach(out, false);

      generateLoad(out, false);

      int min = 0;
      int max = getMappedSuperclassType().getLoadGroupIndex();

      for (int i = min; i <= max; i++)
        generateLoadGroup(out, i);

      generateResultSetLoad(out, false);

      generateSetQuery(out, false);

      generateCopy(out);

      generateMakePersistent(out);

      generateCascadePersist(out);

      generateCascadeRemove(out);

      generateCreate(out);

      generateDelete(out);

      generateDeleteForeign(out);

      generateFlush(out);

      generateAfterCommit(out, false);

      generateAfterRollback(out);

      generateHome(out);

      generateInternals(out);

    } catch (IOException e) {
      throw e;
    }
  }
}
