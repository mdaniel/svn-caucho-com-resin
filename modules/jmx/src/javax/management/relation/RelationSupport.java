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
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;

/**
 * Represents a relation.
 */
public class RelationSupport
  implements RelationSupportMBean, MBeanRegistration {
  private String _id;
  private ObjectName _serviceName;
  private String _relationType;
  private RoleList _roleList;
  private MBeanServer _server;
  private Boolean _isInRelationService;
  
  /**
   * Creates the new relation support.
   */
  public RelationSupport(String id, ObjectName serviceName,
                         String relationType, RoleList roleList)
    throws InvalidRoleValueException
  {
    _id = id;
    _serviceName = serviceName;
    _relationType = relationType;
    _roleList = roleList;
  }
  
  /**
   * Creates the new relation support.
   */
  public RelationSupport(String id, ObjectName serviceName,
                         MBeanServer mbeanServer,
                         String relationType, RoleList roleList)
    throws InvalidRoleValueException
  {
    _id = id;
    _serviceName = serviceName;
    _relationType = relationType;
    _roleList = roleList;
    _server = mbeanServer;
  }
  
  /**
   * Returns the role for the given role name.
   */
  public List getRole(String roleName)
    throws RoleNotFoundException, RelationServiceNotRegisteredException
  {
    for (int i = _roleList.size() - 1; i >= 0; i--) {
      Role role = (Role) _roleList.get(i);

      if (role.getRoleName().equals(roleName))
	return role.getRoleValue();
    }

    return null;
  }

  /**
   * Returns result of accessing by a set of roles.
   */
  public RoleResult getRoles(String []roleNames)
    throws RelationServiceNotRegisteredException
  {
    RoleList roleList = new RoleList();
    RoleUnresolvedList unresolvedList = new RoleUnresolvedList();

    for (int i = 0; i < roleNames.length; i++) {
      getRole(roleNames[i], roleList, unresolvedList);
    }

    return new RoleResult(roleList, unresolvedList);
  }

  /**
   * Adds the named role either to the role list or unresolvedList.
   */
  private void getRole(String roleName,
		       RoleList roleList,
		       RoleUnresolvedList unresolvedList)
  {
    for (int i = _roleList.size() - 1; i >= 0; i--) {
      Role role = (Role) _roleList.get(i);

      if (role.getRoleName().equals(roleName)) {
	roleList.add(role);
	return;
      }
    }

    unresolvedList.add(new RoleUnresolved(roleName, null,
					  RoleStatus.NO_ROLE_WITH_NAME));
  }

  /**
   * Returns the number of MBean currently referenced in the role.
   */
  public Integer getRoleCardinality(String roleName)
    throws RoleNotFoundException
  {
    for (int i = _roleList.size() - 1; i >= 0; i--) {
      Role role = (Role) _roleList.get(i);

      if (role.getRoleName().equals(roleName))
	return new Integer(role.getRoleValue().size());
    }

    return null;
  }
  
  /**
   * Returns all the roles in the relation.
   */
  public RoleResult getAllRoles()
    throws RelationServiceNotRegisteredException
  {
    RoleList roleList = new RoleList();
    RoleUnresolvedList unresolvedList = new RoleUnresolvedList();

    roleList.addAll(_roleList);

    return new RoleResult(roleList, unresolvedList);
  }

  /**
   * Returns all the roles in the relation.
   */
  public RoleList retrieveAllRoles()
  {
    RoleList roleList = new RoleList();

    roleList.addAll(_roleList);

    return roleList;
  }

  /**
   * Set the role.
   */
  public void setRole(Role role)
    throws RoleNotFoundException, RelationTypeNotFoundException,
	   InvalidRoleValueException, RelationServiceNotRegisteredException,
	   RelationTypeNotFoundException, RelationNotFoundException
  {
    for (int i = _roleList.size() - 1; i >= 0; i--) {
      Role oldRole = (Role) _roleList.get(i);

      if (role.getRoleName().equals(oldRole.getRoleName())) {
	_roleList.set(i, (Role) role.clone());
	return;
      }
    }

    _roleList.add((Role) role.clone());
  }

  /**
   * Sets a set of roles.
   */
  public RoleResult setRoles(RoleList roleList)
    throws RelationServiceNotRegisteredException,
	   RelationTypeNotFoundException,
	   RelationNotFoundException
  {
    RoleList setRoles = new RoleList();
    RoleUnresolvedList unsetRoles = new RoleUnresolvedList();

    loop:
    for (int j = roleList.size() - 1; j >= 0; j--) {
      Role role = (Role) roleList.get(j);
	
      for (int i = _roleList.size() - 1; i >= 0; i--) {
	Role oldRole = (Role) _roleList.get(i);

	if (role.getRoleName().equals(oldRole.getRoleName())) {
	  _roleList.set(i, (Role) role.clone());
	  setRoles.add(role);
	  continue loop;
	}
      }

      _roleList.add((Role) role.clone());
      setRoles.add(role);
    }
    
    return new RoleResult(setRoles, unsetRoles);
  }

  /**
   * Handles the callback when an MBean is unregistered.
   */
  public void handleMBeanUnregistration(ObjectName objectName, String roleName)
    throws RoleNotFoundException, InvalidRoleValueException,
    RelationServiceNotRegisteredException, RelationTypeNotFoundException,
    RelationNotFoundException
  {
  }

  /**
   * Returns a map of the referenced mbeans.
   */
  public Map getReferencedMBeans()
  {
    HashMap map = new HashMap();

    synchronized (_roleList) {
      for (int i = _roleList.size() - 1; i >= 0; i--) {
	Role role = (Role) _roleList.get(i);

	List value = role.getRoleValue();

	if (value == null)
	  continue;

	Iterator iter = value.iterator();
	while (iter.hasNext()) {
	  ObjectName name = (ObjectName) iter.next();

	  ArrayList roles = (ArrayList) map.get(name);
	  if (roles == null) {
	    roles = new ArrayList();
	    map.put(name, roles);
	  }
	  roles.add(role.getRoleName());
	}
      }
    }

    return map;
  }

  /**
   * Returns the type of the associated relation.
   */
  public String getRelationTypeName()
  {
    return _relationType;
  }

  /**
   * Returns the object name of the relation service.
   */
  public ObjectName getRelationServiceName()
  {
    return _serviceName;
  }

  /**
   * Returns the relation identifier.
   */
  public String getRelationId()
  {
    return _id;
  }
  
  /**
   * Returns true if the object is handled by the service.
   */
  public Boolean isInRelationService()
  {
    return _isInRelationService;
  }

  /**
   * Sets the flag if the object is handled by the service.
   */
  public void setRelationServiceManagementFlag(Boolean flag)
  {
    _isInRelationService = flag;
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
    _serviceName = name;

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
  }
}

 
