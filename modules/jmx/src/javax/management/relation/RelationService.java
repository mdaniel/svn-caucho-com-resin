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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import javax.management.ObjectName;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.Notification;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.InstanceNotFoundException;

/**
 * Represents a relation service.
 */
public class RelationService extends NotificationBroadcasterSupport
  implements RelationServiceMBean, MBeanRegistration, NotificationListener {
  private boolean _purgeFlag;
  private MBeanServer _server;
  private ObjectName _name;

  private HashMap _typeMap = new HashMap();

  private HashMap _relationMap = new HashMap();

  /**
   * Creates a new RelationService.
   */
  public RelationService(boolean immediatePurge)
  {
    setPurgeFlag(immediatePurge);
  }
  
  /**
   * Tests if the relation service is active.
   */
  public boolean isActive()
    throws RelationServiceNotRegisteredException
  {
    return _server != null;
  }

  /**
   * If true, a notification will purge old relations.
   */
  public boolean getPurgeFlag()
  {
    return _purgeFlag;
  }

  /**
   * If true, a notification will purge old relations.
   */
  public void setPurgeFlag(boolean purgeFlag)
  {
    _purgeFlag = purgeFlag;
  }

  /**
   * Creates a new relation type.
   */
  public void createRelationType(String typeName, RoleInfo []roleInfos)
    throws InvalidRelationTypeException
  {
    RelationType type = (RelationType) _typeMap.get(typeName);

    if (type != null)
      throw new InvalidRelationTypeException("duplicate relation " + typeName);

    type = new RelationTypeSupport(typeName, roleInfos);

    _typeMap.put(typeName, type);
  }

  /**
   * Adds a new relation type.
   */
  public void addRelationType(RelationType relationType)
    throws InvalidRelationTypeException
  {
    String name = relationType.getRelationTypeName();
    
    if (_typeMap.get(name) != null)
      throw new InvalidRelationTypeException("duplicate relation " + name);

    _typeMap.put(name, relationType);
  }

  /**
   * Returns all the relation type names.
   */
  public List getAllRelationTypeNames()
  {
    ArrayList names = new ArrayList();
    names.addAll(_typeMap.keySet());
    
    return names;
  }
  
  /**
   * Returns all the role info objects for the type.
   */
  public List getRoleInfos(String relationType)
    throws RelationTypeNotFoundException
  {
    RelationType type = (RelationType) _typeMap.get(relationType);

    if (type == null)
      throw new RelationTypeNotFoundException(relationType);

    ArrayList roles = new ArrayList();

    roles.addAll(type.getRoleInfos());

    return roles;
  }

  /**
   * Returns the role information.
   */
  public RoleInfo getRoleInfo(String relationType, String roleInfo)
    throws RelationTypeNotFoundException, RoleInfoNotFoundException
  {
    RelationType type = (RelationType) _typeMap.get(relationType);

    if (type == null)
      throw new RelationTypeNotFoundException(relationType);

    List roles = type.getRoleInfos();

    for (int i = roles.size() - 1; i >= 0; i--) {
      RoleInfo role = (RoleInfo) roles.get(i);

      if (role.getName().equals(roleInfo))
	return role;
    }

    throw new RoleInfoNotFoundException(roleInfo);
  }

  /**
   * Removes a relation type.
   */
  public void removeRelationType(String relationType)
    throws RelationTypeNotFoundException
  {
    RelationType type = (RelationType) _typeMap.remove(relationType);

    if (type == null)
      throw new RelationTypeNotFoundException(relationType);
  }

  /**
   * Creates a new relation.
   */
  public void createRelation(String id,
			     String relationType,
                             RoleList roleList)
    throws RelationServiceNotRegisteredException, RoleNotFoundException,
	   InvalidRelationIdException, RelationTypeNotFoundException,
	   InvalidRoleValueException
  {
    if (_server == null)
      throw new RelationServiceNotRegisteredException();
    
    RelationType type = (RelationType) _typeMap.get(relationType);

    if (type == null)
      throw new RelationTypeNotFoundException(relationType);

    

    for (int i = roleList.size() - 1; i >= 0; i--) {
      Role role = (Role) roleList.get(i);

      try {
	if (type.getRoleInfo(role.getRoleName()) == null)
	  throw new RoleNotFoundException(role.getRoleName());
      } catch (RoleInfoNotFoundException e) {
	throw new RoleNotFoundException(role.getRoleName());
      }
    }

    _relationMap.put(id, new RelationSupport(id, _name, _server,
					     type.getRelationTypeName(),
					     roleList));
  }

  /**
   * Adds an object to the relation.
   */
  public void addRelation(ObjectName objectName)
    throws RelationServiceNotRegisteredException, 
	   InvalidRelationIdException, InstanceNotFoundException,
	   InvalidRelationServiceException, RelationTypeNotFoundException,
	   RoleNotFoundException, InvalidRoleValueException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the object name if the relation is an mbean.
   */
  public ObjectName isRelationMBean(String id)
    throws RelationNotFoundException
  {
    return null;
  }

  /**
   * Returns the relation name if the object is a relation mbean.
   */
  public String isRelation(ObjectName objName)
  {
    return null;
  }

  /**
   * Returns true if the relation exists.
   */
  public Boolean hasRelation(String id)
  {
    return new Boolean(_relationMap.get(id) != null);
  }

  /**
   * Returns all the relation ids.
   */
  public List getAllRelationIds()
  {
    ArrayList relationIds = new ArrayList();

    relationIds.addAll(_relationMap.keySet());

    return relationIds;
  }

  /**
   * Checks if the role can be read in the relation.
   */
  public Integer checkRoleReading(String roleName, String relationType)
    throws RelationTypeNotFoundException
  {
    RelationType type = (RelationType) _typeMap.get(relationType);

    if (type == null)
      throw new RelationTypeNotFoundException(relationType);

    List roles = type.getRoleInfos();

    for (int i = roles.size() - 1; i >= 0; i--) {
      RoleInfo role = (RoleInfo) roles.get(i);

      if (role.getName().equals(roleName)) {
	if (role.isReadable())
	  return new Integer(0);
	else
	  return new Integer(RoleStatus.ROLE_NOT_READABLE);
      }
    }

    return new Integer(RoleStatus.NO_ROLE_WITH_NAME);
  }

  /**
   * Checks if the role can be written in the relation.
   */
  public Integer checkRoleWriting(Role role, String relationType,
                                  Boolean initFlag)
    throws RelationTypeNotFoundException
  {
    RelationType type = (RelationType) _typeMap.get(relationType);

    if (type == null)
      throw new RelationTypeNotFoundException(relationType);

    List roles = type.getRoleInfos();

    for (int i = roles.size() - 1; i >= 0; i--) {
      RoleInfo roleInfo = (RoleInfo) roles.get(i);

      if (! roleInfo.getName().equals(role.getRoleName()))
	continue;
      
      if (initFlag != null &&
	  ! initFlag.equals(Boolean.FALSE) &&
	  ! roleInfo.isReadable())
	  return new Integer(RoleStatus.ROLE_NOT_WRITABLE);

      if (role.getRoleValue().size() < roleInfo.getMinDegree())
	return new Integer(RoleStatus.LESS_THAN_MIN_ROLE_DEGREE);
      if (roleInfo.getMaxDegree() < role.getRoleValue().size())
	return new Integer(RoleStatus.MORE_THAN_MAX_ROLE_DEGREE);
	
    }

    return new Integer(RoleStatus.NO_ROLE_WITH_NAME);
  }

  /**
   * Sends a relation creation notification event.
   */
  public void sendRelationCreationNotification(String id)
    throws RelationNotFoundException
  {
  }
  
  /**
   * Sends a role update notification event.
   */
  public void sendRoleUpdateNotification(String id, Role newRole,
                                         List oldRoles)
    throws RelationNotFoundException
  {
  }
  
  /**
   * Sends a relation removal notification event.
   */
  public void sendRelationRemovalNotification(String id,
                                              List unregMBeanList)
    throws RelationNotFoundException
  {
  }
  
  /**
   * Updates the role map.
   */
  public void updateRoleMap(String id, Role newRole,
			    List oldRoleValue)
    throws RelationServiceNotRegisteredException, RelationNotFoundException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes a relation.
   */
  public void removeRelation(String id)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException
  {
    if (_server == null)
      throw new RelationServiceNotRegisteredException();

    Relation relation = (Relation) _relationMap.remove(id);

    if (relation == null)
      throw new RelationNotFoundException(id);
  }

  /**
   * Purges old relations.
   */
  public void purgeRelations()
    throws RelationServiceNotRegisteredException
  {
  }

  /**
   * Finds a map of referencing relations.
   */
  public Map findReferencingRelations(ObjectName mbeanName,
			     String type,
			     String roleName)
  {
    HashMap map;
    map = new HashMap();

    synchronized (_relationMap) {
      Iterator iter = _relationMap.values().iterator();

      while (iter.hasNext()) {
	Relation relation = (Relation) iter.next();

	if (type != null && ! type.equals(relation.getRelationTypeName()))
	  continue;

	ArrayList roleNames = null;

	RoleResult roles;

	try {
	  roles = relation.getAllRoles();
	} catch (Exception e) {
	  continue;
	}
	
	RoleList roleList = roles.getRoles();

	for (int i = roleList.size() - 1; i >= 0; i--) {
	  Role role = (Role) roleList.get(i);

	  if (roleName != null && ! role.getRoleName().equals(roleName))
	    continue;

	  List names = role.getRoleValue();
	  if (! names.contains(mbeanName))
	    continue;

	  if (roleNames == null) {
	    roleNames = new ArrayList();
	    
	    map.put(relation.getRelationId(), roleNames);
	  }
	  
	  roleNames.add(role.getRoleName());
	}
      }
    }
    
    return map;
  }

  /**
   * Finds the associated mbeans.
   */
  public Map findAssociatedMBeans(ObjectName mbeanName,
				  String type,
				  String roleName)
  {
    HashMap map;
    map = new HashMap();

    synchronized (_relationMap) {
      Iterator iter = _relationMap.values().iterator();

      while (iter.hasNext()) {
	Relation relation = (Relation) iter.next();

	if (type != null && ! type.equals(relation.getRelationTypeName()))
	  continue;

	RoleResult roles;

	try {
	  roles = relation.getAllRoles();
	} catch (Exception e) {
	  continue;
	}
	
	RoleList roleList = roles.getRoles();

	for (int i = roleList.size() - 1; i >= 0; i--) {
	  Role role = (Role) roleList.get(i);

	  if (roleName != null && ! role.getRoleName().equals(roleName))
	    continue;

	  List names = role.getRoleValue();
	  if (! names.contains(mbeanName))
	    continue;

	  for (int j = names.size() - 1; j >= 0; j--) {
	    ObjectName name = (ObjectName) names.get(j);

	    if (name.equals(mbeanName))
	      continue;

	    ArrayList roleNames = (ArrayList) map.get(name);
	    
	    if (roleNames == null) {
	      roleNames = new ArrayList();
	      map.put(name, roleNames);
	    }

	    roleNames.add(role.getRoleName());
	  }
	}
      }
    }
    
    return map;
  }

  /**
   * Finds the relations of a certain type.
   */
  public List findRelationsOfType(String relationType)
    throws RelationTypeNotFoundException
  {
    ArrayList relationNames = new ArrayList();
    
    synchronized (_relationMap) {
      Iterator iter = _relationMap.values().iterator();
      while (iter.hasNext()) {
	Relation rel = (Relation) iter.next();

	if (rel.getRelationTypeName().equals(relationType))
	  relationNames.add(rel.getRelationId());
      }
    }

    return relationNames;
  }

  /**
   * Returns the role value for the role name.
   */
  public List getRole(String id, String roleName)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException, RoleNotFoundException
  {
    if (_server == null)
      throw new RelationServiceNotRegisteredException();

    Relation relation = (Relation) _relationMap.get(id);

    if (relation == null)
      throw new RelationNotFoundException(id);

    return relation.getRole(roleName);
  }

  /**
   * Returns the matching roles for the role name.
   */
  public RoleResult getRoles(String id, String []roleNames)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException
  {
    if (_server == null)
      throw new RelationServiceNotRegisteredException();

    Relation relation = (Relation) _relationMap.get(id);

    if (relation == null)
      throw new RelationNotFoundException(id);

    return relation.getRoles(roleNames);
  }

  /**
   * Returns all the roles for the relation.
   */
  public RoleResult getAllRoles(String relationId)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException
  {
    if (_server == null)
      throw new RelationServiceNotRegisteredException();

    Relation relation = (Relation) _relationMap.get(relationId);

    if (relation == null)
      throw new RelationNotFoundException(relationId);

    return relation.getAllRoles();
  }

  /**
   * Returns number of mbeans referenced.
   */
  public Integer getRoleCardinality(String relationId, String roleName)
    throws RelationNotFoundException,
	   RoleNotFoundException
  {
    Relation relation = (Relation) _relationMap.get(relationId);

    if (relation == null)
      throw new RelationNotFoundException(relationId);

    return relation.getRoleCardinality(roleName);
  }

  /**
   * Sets a role for the relation.
   */
  public void setRole(String relationId, Role role)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException,
	   RoleNotFoundException,
	   InvalidRoleValueException,
	   RelationTypeNotFoundException
  {
    Relation relation = (Relation) _relationMap.get(relationId);

    if (relation == null)
      throw new RelationNotFoundException(relationId);

    relation.setRole(role);
  }

  /**
   * Sets a list of roles for the relation.
   */
  public RoleResult setRoles(String relationId, RoleList roles)
    throws RelationServiceNotRegisteredException,
	   RelationNotFoundException
  {
    Relation relation = (Relation) _relationMap.get(relationId);

    if (relation == null)
      throw new RelationNotFoundException(relationId);

    try {
      return relation.setRoles(roles);
    } catch (RelationTypeNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns mbean referenced in the relation.
   */
  public Map getReferencedMBeans(String relationId)
    throws RelationNotFoundException
  {
    Relation relation = (Relation) _relationMap.get(relationId);

    if (relation == null)
      throw new RelationNotFoundException(relationId);

    return relation.getReferencedMBeans();
  }

  /**
   * Returns the relation type for the relation.
   */
  public String getRelationTypeName(String relationId)
    throws RelationNotFoundException
  {
    Relation relation = (Relation) _relationMap.get(relationId);

    if (relation == null)
      throw new RelationNotFoundException(relationId);

    return relation.getRelationTypeName();
  }
  /**
   * Called before the registration.
   *
   * @param server the mbean server to be registered
   * @param name the client's name to be registered
   *
   * @return the name the object wans the be registered as
   */
  public ObjectName preRegister(MBeanServer server,
                                ObjectName name)
    throws Exception
  {
    _server = server;
    _name = name;
    
    return name;
  }
  /**
   * Called after the registration.
   *
   * @param registrationDone true if the registration was successful.
   */
  public void postRegister(Boolean registrationDone)
  {
  }
  
  /**
   * Called before deregistration.
   */
  public void preDeregister()
    throws Exception
  {
  }
  
  /**
   * Called after the deregistration.
   */
  public void postDeregister()
  {
    _server = null;
  }
  
  /**
   * Handles the notification
   *
   * @param notification the notification
   * @param handback the handback
   */
  public void handleNotification(Notification notification, Object handback)
  {
  }
}
