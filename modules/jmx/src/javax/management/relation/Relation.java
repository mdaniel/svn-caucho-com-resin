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
import java.util.Map;
import java.util.ArrayList;

import javax.management.ObjectName;

/**
 * Represents a relation.
 */
public interface Relation {
  /**
   * Returns the role for the given role name.
   */
  public List getRole(String roleName)
    throws RoleNotFoundException, RelationServiceNotRegisteredException;

  /**
   * Returns result of accessing by a set of roles.
   */
  public RoleResult getRoles(String []roleNames)
    throws RelationServiceNotRegisteredException;

  /**
   * Returns the number of MBean currently referenced in the role.
   */
  public Integer getRoleCardinality(String roleName)
    throws RoleNotFoundException;
  
  /**
   * Returns all the roles in the relation.
   */
  public RoleResult getAllRoles()
    throws RelationServiceNotRegisteredException;

  /**
   * Returns all the roles in the relation.
   */
  public RoleList retrieveAllRoles();

  /**
   * Set the role.
   */
  public void setRole(Role role)
    throws RoleNotFoundException, RelationTypeNotFoundException,
	   InvalidRoleValueException, RelationServiceNotRegisteredException,
	   RelationTypeNotFoundException, RelationNotFoundException;

  /**
   * Sets a set of roles.
   */
  public RoleResult setRoles(RoleList roleList)
    throws RelationServiceNotRegisteredException,
	   RelationTypeNotFoundException,
	   RelationNotFoundException;

  /**
   * Handles the callback when an MBean is unregistered.
   */
  public void handleMBeanUnregistration(ObjectName objectName,
					String roleName)
    throws RoleNotFoundException, InvalidRoleValueException,
    RelationServiceNotRegisteredException, RelationTypeNotFoundException,
    RelationNotFoundException;

  /**
   * Returns a map of the referenced mbeans.
   */
  public Map getReferencedMBeans();

  /**
   * Returns the type of the associated relation.
   */
  public String getRelationTypeName();

  /**
   * Returns the object name of the relation service.
   */
  public ObjectName getRelationServiceName();

  /**
   * Returns the relation identifier.
   */
  public String getRelationId();
}

 
