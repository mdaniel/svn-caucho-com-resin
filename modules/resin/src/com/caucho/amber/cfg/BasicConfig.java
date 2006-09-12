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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import java.util.ArrayList;

import javax.persistence.FetchType;

/**
 * <basic> tag in the orm.xml
 */
public class BasicConfig {

  // attributes
  private String _name;
  private FetchType _fetchType;

  // elements
  private ColumnConfig _column;

  // XXX: to do ...
  /*
  private ColumnConfig _column;
  private GeneratedValueConfig _generatedValue;
  private TemporalConfig _temporal;
  private TableGeneratorConfig _tableGenerator;
  private SequenceGeneratorConfig _sequenceGenerator;
  */

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Returns the fetch type.
   */
  public FetchType getFetchType()
  {
    return _fetchType;
  }

  /**
   * Sets the fetch type.
   */
  public void setFetchType(FetchType fetchType)
  {
    _fetchType = fetchType;
  }

  /**
   * Returns the column.
   */
  public ColumnConfig getColumn()
  {
    return _column;
  }

  /**
   * Sets the column.
   */
  public void setColumn(ColumnConfig column)
  {
    _column = column;
  }
}
