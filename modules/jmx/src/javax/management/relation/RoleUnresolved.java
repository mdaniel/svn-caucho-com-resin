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

import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

/**
 * Represents an unresolved role.
 */
public class RoleUnresolved implements java.io.Serializable {
  private String roleName;
  private ArrayList roleValue;
  private int problemType;

  /**
   * Creates a new role.
   */
  public RoleUnresolved(String roleName, List roleValue,
                        int problemType)
  {
    this.roleName = roleName;

    if (roleValue != null)
      this.roleValue = new ArrayList(roleValue);
    this.problemType = problemType;
  }

  /**
   * Returns the role name.
   */
  public String getRoleName()
  {
    return this.roleName;
  }

  /**
   * Sets the role name.
   */
  public void setRoleName(String roleName)
  {
    this.roleName = roleName;
  }

  /**
   * Returns the role value.
   */
  public List getRoleValue()
  {
    return this.roleValue;
  }

  /**
   * Sets the role value.
   */
  public void setRoleValue(List value)
  {
    this.roleValue.clear();
    this.roleValue.addAll(value);
  }

  /**
   * Returns the problem type.
   */
  public int getProblemType()
  {
    return this.problemType;
  }

  /**
   * Sets the problem type.
   */
  public void setProblemType(int problemType)
  {
    this.problemType = problemType;
  }

  /**
   * Clones the value.
   */
  public Object clone()
  {
    return new RoleUnresolved(this.roleName, this.roleValue,
			      this.problemType);
  }

  /**
   * Returns a string value of the role.
   */
  public String toString()
  {
    return "RoleUnresolved[name=" + this.roleName + "]";
  }
}

 
