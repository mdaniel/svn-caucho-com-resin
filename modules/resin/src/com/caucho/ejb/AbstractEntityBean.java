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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb;

import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

/**
 * Convenience class implementing the EntityBean methods as stubs.
 */
abstract public class AbstractEntityBean implements EntityBean {
  // the entity context
  protected EntityContext _entityContext;

  /**
   * Sets the entity context.
   */
  public void setEntityContext(EntityContext entityContext)
    throws EJBException
  {
    _entityContext = entityContext;
  }

  /**
   * Returns the entity context.
   */
  public EntityContext getEntityContext()
  {
    return _entityContext;
  }

  /**
   * Unsets the entity context when it's taken out of use.
   */
  public void unsetEntityContext()
    throws EJBException
  {
    _entityContext = null;
  }

  /**
   * Called when the entity bean is taken from the pool and bound to
   * a specific underlying entity.
   */
  public void ejbActivate() throws EJBException
  {
  }
  
  /**
   * Called when the entity bean is about to be returned to the pool.
   */
  public void ejbPassivate() throws EJBException
  {
  }
  
  /**
   * Called to resynchronize the entity bean with the backing store.
   */
  public void ejbLoad() throws EJBException
  {
  }
  
  /**
   * Called to save any changes to the backing store
   */
  public void ejbStore() throws EJBException
  {
  }
  
  public void ejbRemove() throws RemoveException
  {
  }
}
