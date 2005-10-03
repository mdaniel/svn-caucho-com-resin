/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.jmx;

import java.util.TreeMap;
import java.util.TreeSet;

public class AdminInfo
{
  private String _title;
  private String _description;

  private TreeMap<String, AdminAttributeInfo> _attributeMap;
  private TreeMap<String, AdminOperationInfo> _operationMap;

  /**
   * Set's the title for the mbean, default null.
   */
  public void setTitle(String title)
  {
    _title = title;
  }

  public String getTitle()
  {
    return _title;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  public String getDescription()
  {
    return _description;
  }

  /**
   * Return an existing {@link AdminAttributeInfo}, or create one and return it.
   */
  public AdminAttributeInfo createAdminAttributeInfo(String name)
  {
    if (_attributeMap == null)
      _attributeMap = new TreeMap<String, AdminAttributeInfo>();

    AdminAttributeInfo adminAttributeInfo = _attributeMap.get(name);

    if (adminAttributeInfo == null) {
      name = name.intern();

      adminAttributeInfo = new AdminAttributeInfo(name);

      adminAttributeInfo.setSortOrder(_attributeMap.size());

      _attributeMap.put(name, adminAttributeInfo);
    }

    return adminAttributeInfo;
  }

  /**
   * Return an existing {@link AdminOperationInfo}, or create one and return it.
   */
  public AdminOperationInfo createAdminOperationInfo(String name)
  {
    if (_operationMap == null)
    _operationMap = new TreeMap<String, AdminOperationInfo>();

    AdminOperationInfo adminOperationInfo = _operationMap.get(name);

    if (adminOperationInfo == null) {
      name = name.intern();

      adminOperationInfo = new AdminOperationInfo(name);

      adminOperationInfo.setSortOrder(_operationMap.size());

      _operationMap.put(name, adminOperationInfo);
    }

    return adminOperationInfo;
  }

  /**
   * Return an existing {@link AdminAttributeInfo}, or null
   */
  public AdminAttributeInfo getAdminAttributeInfo(String name)
  {
    AdminAttributeInfo adminAttributeInfo;

    if (_attributeMap != null)
      adminAttributeInfo = _attributeMap.get(name);
    else
      adminAttributeInfo = null;

    return adminAttributeInfo;
  }

  /**
   * Return an existing {@link AdminOperationInfo}, or null
   */
  public AdminOperationInfo getAdminOperationInfo(String name)
  {
    AdminOperationInfo adminOperationInfo;

    if (_operationMap != null)
      adminOperationInfo = _operationMap.get(name);
    else
      adminOperationInfo = null;

    return adminOperationInfo;
  }
}
