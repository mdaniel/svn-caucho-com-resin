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

package com.caucho.amber.hibernate;

import com.caucho.util.L10N;

import com.caucho.make.PersistentDependency;

import com.caucho.amber.AmberManager;

/**
 * top-level config for the hibernate-mapping.
 */
public class HibernateMapping {
  private static final L10N L = new L10N(HibernateMapping.class);

  private AmberManager _amberManager;
  private PersistentDependency _depend;

  HibernateMapping(AmberManager amberManager)
  {
    _amberManager = amberManager;
  }

  /**
   * Returns the manager.
   */
  AmberManager getManager()
  {
    return _amberManager;
  }

  /**
   * Sets the dependency.
   */
  void setDependency(PersistentDependency depend)
  {
    _depend = depend;
  }

  /**
   * Gets the dependency.
   */
  PersistentDependency getDependency()
  {
    return _depend;
  }

  /**
   * Adds a hibernate class.
   */
  public HibernateClass createClass()
  {
    return new HibernateClass(this);
  }
}
