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
import javax.management.InstanceNotFoundException;

/**
 * Represents a relation service.
 */
public interface RelationServiceMBean {
  /**
   * Tests if the relation service is active.
   */
  public boolean isActive()
    throws RelationServiceNotRegisteredException;

  /**
   * If true, a notification will purge old relations.
   */
  public boolean getPurgeFlag();

  /**
   * If true, a notification will purge old relations.
   */
  public void setPurgeFlag(boolean purgeFlag);

  /**
   * Creates a new relation type.
   */
  public void createRelationType(String relationType, RoleInfo []roleInfos)
    throws InvalidRelationTypeException;

  /**
   * Adds a new relation type.
   */
  public void addRelationType(RelationType relationType)
    throws InvalidRelationTypeException;

  /**
   * Returns all the relation type names.
   */
  public List getAllRelationTypeNames();
  
  /**
   * Returns all the role info objects for the type.
   */
  public List getRoleInfos(String relationType)
    throws RelationTypeNotFoundException;

  /**
   * Returns the role information.
   */
  public RoleInfo getRoleInfo(String relationType, String roleInfo)
    throws RelationTypeNotFoundException, RoleInfoNotFoundException;

  /**
   * Removes a relation type.
   */
  public void removeRelationType(String relationType)
    throws RelationTypeNotFoundException;

  /**
   * Creates a new relation.
   */
  public void createRelation(String id, String relationType,
                             RoleList roleList)
    throws RelationServiceNotRegisteredException, RoleNotFoundException,
    InvalidRelationIdException, RelationTypeNotFoundException,
    InvalidRoleValueException;

  /**
   * Adds an object to the relation.
   */
  public void addRelation(ObjectName objectName)
    throws RelationServiceNotRegisteredException, 
    InvalidRelationIdException, InstanceNotFoundException,
    InvalidRelationServiceException, RelationTypeNotFoundException,
    RoleNotFoundException, InvalidRoleValueException;

  /**
   * Returns the object name if the relation is an mbean.
   */
  public ObjectName isRelationMBean(String id)
    throws RelationNotFoundException;

  /**
   * Returns the relation name if the object is a relation mbean.
   */
  public String isRelation(ObjectName objName);

  /**
   * Returns true if the relation exists.
   */
  public Boolean hasRelation(String id);

  /**
   * Returns all the relation ids.
   */
  public List getAllRelationIds();

  /**
   * Checks if the role can be read in the relation.
   */
  public Integer checkRoleReading(String roleName, String relationType)
    throws RelationTypeNotFoundException;

  /**
   * Checks if the role can be written in the relation.
   */
  public Integer checkRoleWriting(Role role, String relationType,
                                  Boolean initFlag)
    throws RelationTypeNotFoundException;

  /**
   * Sends a relation creation notification event.
   */
  public void sendRelationCreationNotification(String id)
    throws RelationNotFoundException;
  
  /**
   * Sends a role update notification event.
   */
  public void sendRoleUpdateNotification(String id, Role newRole,
                                         List oldRoles)
    throws RelationNotFoundException;
  
  /**
   * Sends a relation removal notification event.
   */
  public void sendRelationRemovalNotification(String id,
                                              List unregMBeanList)
    throws RelationNotFoundException;
  
  /**
   * Updates the role map.
   */
  public void updateRoleMap(String id, Role newRole,
			    List oldRoleValue)
    throws RelationServiceNotRegisteredException, RelationNotFoundException;

  /**
   * Removes a relation.
   */
  public void removeRelation(String id)
    throws RelationServiceNotRegisteredException,
    RelationNotFoundException;

  /**
   * Purges old relations.
   */
  public void purgeRelations()
    throws RelationServiceNotRegisteredException;

  /**
   * Finds a map of referencing relations.
   */
  public Map
    findReferencingRelations(ObjectName mbeanName,
			     String relationType,
			     String roleName);

  /**
   * Finds the associated mbeans.
   */
  public Map
    findAssociatedMBeans(ObjectName mbeanName,
			 String relationType,
			 String roleName);

  /**
   * Finds the relations of a certain type.
   */
  public List findRelationsOfType(String relationType)
    throws RelationTypeNotFoundException;

  /**
   * Returns the role value for the role name.
   */
  public List getRole(String id, String roleName)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException, RoleNotFoundException;

  /**
   * Returns the matching roles for the role name.
   */
  public RoleResult getRoles(String relationId, String []roleNames)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException;

  /**
   * Returns all the roles for the relation.
   */
  public RoleResult getAllRoles(String relationId)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException;

  /**
   * Returns number of mbeans referenced.
   */
  public Integer getRoleCardinality(String relationId, String roleName)
    throws RelationNotFoundException,
	   RoleNotFoundException;

  /**
   * Sets a role for the relation.
   */
  public void setRole(String relationId, Role role)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException,
	   RoleNotFoundException,
	   InvalidRoleValueException,
	   RelationTypeNotFoundException;

  /**
   * Sets a list of roles for the relation.
   */
  public RoleResult setRoles(String relationId, RoleList roles)
    throws RelationServiceNotRegisteredException,
    RelationNotFoundException;

  /**
   * Returns mbean referenced in the relation.
   */
  public Map getReferencedMBeans(String relationId)
    throws RelationNotFoundException;

  /**
   * Returns the relation type for the relation.
   */
  public String getRelationTypeName(String relationId)
    throws RelationNotFoundException;
}
