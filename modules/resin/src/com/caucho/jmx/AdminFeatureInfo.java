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

abstract public class AdminFeatureInfo
  implements Comparable<AdminFeatureInfo>
{
  private final String _name;

  private String _title;
  private String _description;
  private String _deprecated;
  private boolean _isIgnored;
  private int _sortOrder;

  AdminFeatureInfo(String name)
  {
    _name = name;
  }

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
   * If set, clients should ignore this attribute and not display
   * it, default is null (not deprecated).  The format of the String is
   * "VERSION[ REASON]", for example "3.0.15" or "3.0.15 Use bar instead".
   *
   * <p>The corresponding descriptor field is "deprecated"</p>
   */
  public AdminFeatureInfo setDeprecated(String deprecated)
  {
    _deprecated = deprecated;

    return this;
  }

  public String getDeprecated()
  {
    return _deprecated;
  }

  /**
   * If "true" the Resin administration service will ignore the attribute,
   * default null (false).
   *
   * <p>The corresponding descriptor field is "com.caucho.admin.ignored"</p>
   */
  public AdminFeatureInfo setIgnored(boolean isIgnored)
  {
    _isIgnored = isIgnored;

    return this;
  }

  public boolean isIgnored()
  {
    return _isIgnored;
  }

  /**
   * A number that establishes a sort order for the Resin administration
   * service, default is null.
   */
  void setSortOrder(int sortOrder)
  {
    _sortOrder = sortOrder;
  }

  public int getSortOrder()
  {
    return _sortOrder;
  }

  public int compareTo(AdminFeatureInfo o)
  {
    if (o == this)
      return 0;

    if (o == null)
      return 1;

    if (!(this.getClass().isAssignableFrom(o.getClass())))
      return getClass().getName().compareTo((o.getClass().getName()));

    if (_sortOrder != o._sortOrder)
      return _sortOrder < o._sortOrder ? -1 : 1;

    return _name.compareTo(o._name);
  }

  public boolean equals(Object o)
  {
    if (o == this)
      return true;

    if (!(o instanceof AdminFeatureInfo))
      return false;

    return compareTo((AdminFeatureInfo) o) == 0;
  }
}
