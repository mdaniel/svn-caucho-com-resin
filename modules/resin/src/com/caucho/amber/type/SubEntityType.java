/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.type;

import com.caucho.util.L10N;

import com.caucho.amber.AmberManager;

import com.caucho.amber.field.Id;

import com.caucho.amber.table.Column;

/**
 * Represents an application persistent bean type
 */
public class SubEntityType extends EntityType {
  private static final L10N L = new L10N(SubEntityType.class);

  private EntityType _root;
  private EntityType _parent;

  private Id _id;

  public SubEntityType(AmberManager amberManager, EntityType parent)
  {
    super(amberManager);

    _parent = parent;
    _root = parent.getRootType();

    _loadGroupIndex = _parent.getLoadGroupIndex() + 1;
    _defaultLoadGroupIndex = _loadGroupIndex;
    _dirtyIndex = _parent.getDirtyIndex();
  }

  /**
   * Returns the id.
   */
  public Id getId()
  {
    if (_id != null)
      return _id;
    else
      return _parent.getId();
  }

  /**
   * Sts the id.
   */
  public void setId(Id id)
  {
    _id = id;
  }

  /**
   * Returns the root type.
   */
  public EntityType getRootType()
  {
    return _root;
  }

  /**
   * Returns the parent class.
   */
  public EntityType getParentType()
  {
    return _parent;
  }

  /**
   * Returns the discriminator.
   */
  public Column getDiscriminator()
  {
    return getRootType().getDiscriminator();
  }

  /**
   * Printable version of the entity.
   */
  public String toString()
  {
    return "SubEntityType[" + getBeanClass().getName() + "]";
  }
}
