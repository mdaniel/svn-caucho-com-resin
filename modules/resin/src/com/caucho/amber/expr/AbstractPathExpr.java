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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.expr;

import com.caucho.amber.query.*;


import com.caucho.util.L10N;

import com.caucho.amber.AmberRuntimeException;

import com.caucho.amber.field.AmberField;

import com.caucho.amber.type.AbstractStatefulType;
import com.caucho.amber.type.RelatedType;

/**
 * Represents an amber mapping query expression
 */
abstract public class AbstractPathExpr extends AbstractAmberExpr
  implements PathExpr {
  private static final L10N L = new L10N(AbstractPathExpr.class);

  /**
   * Creates the expr from the path.
   */
  public AmberExpr createField(QueryParser parser, String fieldName)
  {
    AmberField field = null;

    AbstractStatefulType type = getTargetType();

    do {
      field = type.getField(fieldName);

      if (type instanceof RelatedType)
        type = ((RelatedType) type).getParentType();
    }
    while ((type != null) && (field == null));

    if (field == null)
      throw new AmberRuntimeException(L.l("'{0}' is an unknown field of '{1}'.",
            fieldName, getTargetType().getName()));

    return field.createExpr(parser, this);
  }

  /**
   * Creates an array reference.
   */
  public AmberExpr createArray(AmberExpr field)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Binds the expression as a select item.
   */
  public PathExpr bindSelect(QueryParser parser, String tableName)
  {
    return this;
  }

  /**
   * Binds the expression as a select item.
   */
  public FromItem bindSubPath(QueryParser parser)
  {
    return null;
  }

  /**
   * Returns the from item
   */
  public FromItem getChildFromItem()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
