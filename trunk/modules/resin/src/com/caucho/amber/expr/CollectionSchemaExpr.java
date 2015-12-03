/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParseException;
import com.caucho.amber.query.QueryParser;
import com.caucho.util.L10N;

/**
 * Represents a collection from a from-item table.
 */
public class CollectionSchemaExpr extends SchemaExpr {
  private static final L10N L = new L10N(CollectionSchemaExpr.class);

  private PathExpr _expr;
  private String _name;

  /**
   * Creates the collection schema.
   */
  public CollectionSchemaExpr(PathExpr expr, String name)
  {
    _expr = expr;
    _name = name;
  }

  /**
   * Returns the tail name.
   */
  public String getTailName()
  {
    return _name;
  }

  /**
   * Creates a field-based schema.
   */
  public SchemaExpr createField(QueryParser parser, String name)
    throws QueryParseException
  {
    throw parser.error(L.l("collections in FROM may not be sub-collected."));
  }

  /**
   * Adds the from item.
   */
  public FromItem addFromItem(QueryParser parser, String id)
    throws QueryParseException
  {
    _expr = _expr.bindSelect(parser, id);

    FromItem fromItem = _expr.bindSubPath(parser);

    return fromItem;
  }
}
