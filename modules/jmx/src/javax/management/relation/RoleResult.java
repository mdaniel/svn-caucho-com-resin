/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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

package javax.management.relation;

/**
 * The result of multiple accesses to a role.
 */
public class RoleResult implements java.io.Serializable {
  private RoleList roleList;
  private RoleUnresolvedList unresolvedRoleList;

  /**
   * Creates a new role.
   */
  public RoleResult(RoleList roleList, RoleUnresolvedList roleUnresolvedList)
  {
    this.roleList = roleList;
    this.unresolvedRoleList = roleUnresolvedList;
  }

  /**
   * Returns the roles accessed.
   */
  public RoleList getRoles()
  {
    return this.roleList;
  }

  /**
   * Sets the roles accessed.
   */
  public void setRoles(RoleList roleList)
  {
    this.roleList = roleList;
  }

  /**
   * Returns the roles unsuccessfully accessed.
   */
  public RoleUnresolvedList getRolesUnresolved()
  {
    return this.unresolvedRoleList;
  }

  /**
   * Sets the roles unsuccessfully accessed.
   */
  public void setRolesUnresolved(RoleUnresolvedList roleList)
  {
    this.unresolvedRoleList = roleList;
  }

  public String toString()
  {
    return ("RoleResult[roles:" + this.roleList + ",unres:" +
	    this.unresolvedRoleList + "]");
  }
}

 
