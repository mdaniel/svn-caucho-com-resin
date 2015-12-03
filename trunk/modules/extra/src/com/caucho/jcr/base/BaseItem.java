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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jcr.base;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

/**
 * Represents a Node or Property in the repository.
 */
public class BaseItem implements Item {
  protected BaseItem()
  {
  }
  
  /**
   * Returns the full absolute pathname of the item.
   */
  public String getPath()
    throws RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Returns the tail name of the item.
   */
  public String getName()
    throws RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the ancestor given by the depth.
   */
  public Item getAncestor(int depth)
    throws ItemNotFoundException,
           AccessDeniedException,
           RepositoryException
  {
    Item item = this;
    
    for (; depth > 0; depth--) {
      item = item.getParent();
    }

    return item;
  }

  /**
   * Returns the parent node.
   */
  public Node getParent()
    throws ItemNotFoundException,
           AccessDeniedException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the current depth of the item.
   */
  public int getDepth()
    throws RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the owning session.
   */
  public Session getSession()
    throws RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true for a node (directory).
   */
  public boolean isNode()
  {
    return false;
  }

  /**
   * Returns true if the item is newly added to the repository.
   */
  public boolean isNew()
  {
    return false;
  }
  
  /**
   * Returns true if the item has been modified.
   */
  public boolean isModified()
  {
    return false;
  }
  
  /**
   * Returns true if the item is identical to another item.
   */
  public boolean isSame(Item otherItem)
    throws RepositoryException
  {
    return this == otherItem;
  }

  /**
   * Visits the node.
   */
  public void accept(ItemVisitor visitor)
    throws RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Saves changes to the item.
   */
  public void save()
    throws AccessDeniedException,
           ItemExistsException,
           ConstraintViolationException,
           InvalidItemStateException,
           ReferentialIntegrityException,
           VersionException,
           LockException,
           NoSuchNodeTypeException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Refreshes data from the backing store.
   *
   * @param keepChanges if true, changes are merged from the repository
   */
  public void refresh(boolean keepChanges)
    throws InvalidItemStateException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Removes the item from the store.
   */
  public void remove()
    throws VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
