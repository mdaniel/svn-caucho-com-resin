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

/**
 * Various error when accessing a role.
 */
public class RoleStatus {
  final public static int NO_ROLE_WITH_NAME = 1;
  final public static int ROLE_NOT_READABLE = 2;
  final public static int ROLE_NOT_WRITABLE = 3;
  final public static int LESS_THAN_MIN_ROLE_DEGREE = 4;
  final public static int MORE_THAN_MAX_ROLE_DEGREE = 5;
  final public static int REF_MBEAN_OF_INCORRECT_CLASS = 6;
  final public static int REF_MBEAN_NOT_REGISTERED = 7;

  /**
   * Creates a new RoleStatus object.
   */
  public RoleStatus()
  {
  }

  /**
   * Returns true if this is a valid role status.
   */
  public static boolean isRoleStatus(int status)
  {
    return status >= 1 && status <= 7;
  }
}

 
