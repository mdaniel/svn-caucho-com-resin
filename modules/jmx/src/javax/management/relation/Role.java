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
 * Represents a relation role.
 */
public class Role implements java.io.Serializable {
  private String name;
  private ArrayList objectNameList;

  /**
   * Creates a new role.
   */
  public Role(String roleName, List roleValue)
  {
    this.name = roleName;
    this.objectNameList = new ArrayList();
    
    this.objectNameList.addAll(roleValue);
  }

  /**
   * Returns the role name.
   */
  public String getRoleName()
  {
    return this.name;
  }

  /**
   * Sets the role name.
   */
  public void setRoleName(String roleName)
  {
    this.name = roleName;
  }

  /**
   * Returns the role value.
   */
  public List getRoleValue()
  {
    return this.objectNameList;
  }

  /**
   * Sets the role value.
   */
  public void setRoleValue(List value)
  {
    this.objectNameList.clear();
    this.objectNameList.addAll(value);
  }

  /**
   * Clones the value.
   */
  public Object clone()
  {
    return new Role(this.name, this.objectNameList);
  }

  /**
   * Return a string of the role values.
   */
  public static String roleValuesToString(List values)
  {
    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < values.size(); i++) {
      if (i != 0)
        sb.append(",");
      sb.append(values.get(i));
    }

    return sb.toString();
  }

  /**
   * Returns a string value of the role.
   */
  public String toString()
  {
    return "Role[name=" + this.name + "]";
  }
}

 
