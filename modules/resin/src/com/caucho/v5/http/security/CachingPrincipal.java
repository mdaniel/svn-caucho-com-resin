/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.security;

import java.util.ArrayList;

/**
 * Principal which caches the roles.
 */
public class CachingPrincipal extends BasicPrincipal {
  private ArrayList<String> _roles;
  
  /**
   * Zero-arg constructor for serialization.
   */
  public CachingPrincipal()
  {
  }

  /**
   * Creates a new caching principal.
   */
  public CachingPrincipal(String name)
  {
    super(name);
  }

  /**
   * Returns the boolean for the role.
   *
   * @return null if the role status is unknown
   */
  public Boolean isInRole(String role)
  {
    if (_roles == null)
      return null;
    else if (_roles.indexOf(role) >= 0)
      return Boolean.TRUE;
    else
      return Boolean.FALSE;
  }

  /**
   * Sets the boolean for the role.
   */
  public void addRole(String role)
  {
    if (_roles == null)
      _roles = new ArrayList<String>();

    if (! _roles.contains(role))
      _roles.add(role);
  }
}
