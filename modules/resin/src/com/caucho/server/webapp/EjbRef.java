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

package com.caucho.server.webapp;

import com.caucho.util.L10N;

/**
 * ejb-ref configuration
 */
public class EjbRef {
  static L10N L = new L10N(EjbRef.class);

  // The ejb-ref name
  private String _name;
  
  // The ejb-ref description
  private String _description;

  /**
   * Creates the ejb-ref.
   */
  public EjbRef()
  {
  }

  /**
   * Sets the name.
   */
  public void setEjbRefName(String name)
  {
    _name = name;
  }

  /**
   * Sets the type.
   */
  public void setEjbRefType(String type)
  {
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Sets the home.
   */
  public void setHome(String home)
  {
  }

  /**
   * Sets the remote.
   */
  public void setRemote(String remote)
  {
  }

  /**
   * Sets the local-home.
   */
  public void setLocalHome(String localHome)
  {
  }

  /**
   * Sets the local.
   */
  public void setLocal(String local)
  {
  }

  /**
   * Sets the link to the actual ejb
   */
  public void setEjbLink(String ejbLink)
  {
  }
}
