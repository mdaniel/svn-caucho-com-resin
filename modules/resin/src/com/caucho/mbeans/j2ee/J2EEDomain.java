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

package com.caucho.mbeans.j2ee;

import com.caucho.jmx.MBean;

/**
 * J2EE Domain
 */
@MBean(j2eeType="J2EEDomain")
public interface J2EEDomain extends J2EEManagedObject {
  /**
   * For example, <tt>j2ee:j2eeType=J2EEDomain,name=default</tt>
   */
  public String getObjectName();

  /**
   * Returns the servers running in this domain
   */
  public String []getServers();

  /**
   * For J2EEDomain this is a special case.  If true, the domain
   * supports event notifications and the J2EEDomain managed object
   * emits notifications from all events providers in the domain.
   *
   * <h3>Events specifric to this managed object</h2>
   * <dl>
   * <dt>j2ee.object.created
   * <dd>occurs when the managed object is created
   * <dt>j2ee.object.deleted
   * <dd>occurs when the managed object is deleted
   * </dl>
   * @return
   */
  boolean isEventsProvider();
}
