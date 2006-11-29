/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.entity;

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.xa.TransactionObject;
/**
 * Abstract base class for an Entity implementation.
 */
public interface QEntity extends TransactionObject {
  public static final byte _CAUCHO_IS_REMOVED = 0;
  public static final byte _CAUCHO_IS_DEAD = 1;
  public static final byte _CAUCHO_IS_NEW = 2;
  public static final byte _CAUCHO_IS_HOME = 3;
  public static final byte _CAUCHO_IS_ACTIVE = 4;
  public static final byte _CAUCHO_IS_LOADED = 5;
  public static final byte _CAUCHO_IS_DIRTY = 6;
  public static final byte _CAUCHO_IS_CREATED = 7;
  // in the process of removing
  public static final byte _CAUCHO_IS_REMOVING = 8;

  public boolean _caucho_isMatch(AbstractServer server, Object primaryKey);
}
