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

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a type of relation.
 */
public class RelationTypeSupport implements RelationType {
  private String _name;
  private ArrayList _roles = new ArrayList();

  /**
   * Creates a new RelationTypeSupport.
   */
  public RelationTypeSupport(String name, RoleInfo []roleInfos)
    throws InvalidRelationTypeException
  {
    _name = name;

    for (int i = 0; i < roleInfos.length; i++)
      _roles.add(roleInfos[i]);
  }

  /**
   * Protected constructor.
   */
  protected RelationTypeSupport(String name)
  {
    _name = name;
  }

  /**
   * Returns the type name.
   */
  public String getRelationTypeName()
  {
    return _name;
  }

  /**
   * Returns a list of the role info.
   */
  public List getRoleInfos()
  {
    return _roles;
  }
  
  /**
   * Returns the named role info.
   */
  public RoleInfo getRoleInfo(String name)
    throws RoleInfoNotFoundException
  {
    for (int i = 0; i < _roles.size(); i++) {
      RoleInfo role = (RoleInfo) _roles.get(i);

      if (role.getName().equals(name))
	return role;
    }

    throw new RoleInfoNotFoundException(name);
  }

  /**
   * Adds role info.
   */
  protected void addRoleInfo(RoleInfo roleInfo)
    throws InvalidRelationTypeException
  {
    _roles.add(roleInfo);
  }
}

 
