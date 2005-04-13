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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.admin;

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.ejb.EnvServerManager;

/**
 * Administration for EJB.  EJBAdmin is available in the ServletContext
 * as caucho.ejb.admin:
 *
 * <h3>Invalidating the EJB Cache</h3>
 *
 * <pre>
 * EJBAdmin admin = (EJBAdmin) application.getAttribute("caucho.ejb.admin");
 * admin.invalidateCache();
 * </pre>
 */
public class EJBAdmin {
  protected static final L10N L = new L10N(EJBAdmin.class);
  protected static final Logger log = Log.open(EJBAdmin.class);
  
  private EnvServerManager _ejbManager;

  /**
   * Creates the new administration.
   */
  public EJBAdmin(EnvServerManager ejbManager)
  {
    _ejbManager = ejbManager;
  }

  /**
   * Clears all caches.  Applications would call this if an external
   * database update occurred.
   */
  public void invalidateCache()
  {
    _ejbManager.invalidateCache();
  }
}
