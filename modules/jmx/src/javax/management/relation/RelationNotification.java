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

import javax.management.Notification;
import javax.management.ObjectName;

/**
 * Implementation of the relation interface.
 */
public class RelationNotification extends Notification {
  public static final String RELATION_BASIC_CREATION =
  "jmx.relation.creation.basic";
  public static final String RELATION_MBEAN_CREATION =
  "jmx.relation.creation.mbean";
  public static final String RELATION_BASIC_UPDATE =
  "jmx.relation.update.basic";
  public static final String RELATION_MBEAN_UPDATE =
  "jmx.relation.update.mbean";
  public static final String RELATION_BASIC_REMOVAL =
  "jmx.relation.removal.basic";
  public static final String RELATION_MBEAN_REMOVAL =
  "jmx.relation.removal.mbean";

  private ArrayList myNewRoleValue;
  private ArrayList myOldRoleValue;
  private String myRelId;
  private ObjectName myRelObjName;
  private String myRelTypeName;
  private String myRoleName;
  private ArrayList myUnregMBeanList;

  RelationNotification(String type, Object source,
                       long sequenceNumber, long timeStamp, String message,
                       String relationId, String relationType,
                       ObjectName relationObject, List unregList)
  {
    super(type, source, sequenceNumber, timeStamp, message);
        
    myRelId = relationId;
    myRelTypeName = relationType;
    myRelObjName = relationObject;
    myUnregMBeanList = new ArrayList(unregList);
  }

  RelationNotification(String type, Object source,
                       long sequenceNumber, long timeStamp, String message,
                       String relationId, String relationType,
                       ObjectName relationObject, String roleName,
                       List newRoleValue, List oldRoleValue)
  {
    super(type, source, sequenceNumber, timeStamp, message);
        
    myRelId = relationId;
    myRelTypeName = relationType;
    myRelObjName = relationObject;
    myRoleName = roleName;
    myNewRoleValue = new ArrayList(newRoleValue);
    myOldRoleValue = new ArrayList(oldRoleValue);
  }

  /**
   * Returns the relation identifier that has changed.
   */
  public String getRelationId()
  {
    return myRelId;
  }

  /**
   * Returns the relation type name that has changed.
   */
  public String getRelationTypeName()
  {
    return myRelTypeName;
  }

  /**
   * Returns the object name that has changed.
   */
  public ObjectName getObjectName()
  {
    return myRelObjName;
  }

  /**
   * Returns the mbeans that are being unregistered.
   */
  public List getMBeansToUnregister()
  {
    return myUnregMBeanList;
  }

  /**
   * Returns the updated role name.
   */
  public String getRoleName()
  {
    return myRoleName;
  }

  /**
   * Returns the old role values
   */
  public List getOldRoleValue()
  {
    return myOldRoleValue;
  }

  /**
   * Returns the new role values
   */
  public List getNewRoleValue()
  {
    return myNewRoleValue;
  }
}

  
