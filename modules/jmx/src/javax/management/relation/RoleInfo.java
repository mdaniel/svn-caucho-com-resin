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

import javax.management.NotCompliantMBeanException;

/**
 * Represents a relation role.
 */
public class RoleInfo implements java.io.Serializable {
  final public static int ROLE_CARDINALITY_INFINITY = Integer.MAX_VALUE;

  private String _description;
  private boolean _isReadable;
  private boolean _isWritable;
  private int _maxDegree = ROLE_CARDINALITY_INFINITY;
  private int _minDegree;
  private String _name;
  private String _refMBeanClassName;

  /**
   * Cloning constructor.
   */
  public RoleInfo(RoleInfo info)
    throws IllegalArgumentException
  {
    _description = info.getDescription();
    _isReadable = info.isReadable();
    _isWritable = info.isWritable();
    _maxDegree = info.getMaxDegree();
    _minDegree = info.getMinDegree();
    _name = info.getName();
    _refMBeanClassName = info.getRefMBeanClassName();
  }

  /**
   * Constructor.
   */
  public RoleInfo(String name, String className)
    throws IllegalArgumentException,
    ClassNotFoundException, NotCompliantMBeanException
  {
    _name = name;
    _refMBeanClassName = className;
  }

  /**
   * Constructor.
   */
  public RoleInfo(String name, String className,
                  boolean isReadable, boolean isWritable)
    throws IllegalArgumentException,
    ClassNotFoundException, NotCompliantMBeanException
  {
    _name = name;
    _refMBeanClassName = className;
    _isReadable = isReadable;
    _isWritable = isWritable;
  }

  /**
   * Constructor.
   */
  public RoleInfo(String name, String className,
                  boolean isReadable, boolean isWritable,
                  int minDegree, int maxDegree, String description)
    throws IllegalArgumentException, InvalidRoleInfoException,
    ClassNotFoundException, NotCompliantMBeanException
  {
    _name = name;
    _refMBeanClassName = className;
    _isReadable = isReadable;
    _isWritable = isWritable;
    _minDegree = minDegree;
    _maxDegree = maxDegree;
    _description = description;
  }

  /**
   * Returns the role name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns true if the role is readable.
   */
  public boolean isReadable()
  {
    return _isReadable;
  }

  /**
   * Returns true if the role is writable.
   */
  public boolean isWritable()
  {
    return _isWritable;
  }

  /**
   * Returns the role description.
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * Returns the minimum degree.
   */
  public int getMinDegree()
  {
    return _minDegree;
  }

  /**
   * Returns the maximum degree.
   */
  public int getMaxDegree()
  {
    return _maxDegree;
  }

  /**
   * Returns the mbean's class name.
   */
  public String getRefMBeanClassName()
  {
    return _refMBeanClassName;
  }

  /**
   * Checks the integer against the min degree.
   */
  public boolean checkMinDegree(int degree)
  {
    return _minDegree <= degree;
  }

  /**
   * Checks the integer against the max degree.
   */
  public boolean checkMaxDegree(int degree)
  {
    return degree <= _maxDegree;
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return "RoleInfo[name=" + _name + "]";
  }
}
