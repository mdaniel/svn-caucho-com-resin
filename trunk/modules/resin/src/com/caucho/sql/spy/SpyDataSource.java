/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.sql.spy;

import java.util.concurrent.atomic.AtomicInteger;

import com.caucho.sql.DriverConfig;

/**
 * Source for spy connections
 */
public class SpyDataSource {
  private String _name;
  
  private AtomicInteger _connIdCount = new AtomicInteger();;

  /**
   * Creates a new SpyDataSource
   */
  public SpyDataSource()
  {
    this(null);
  }

  /**
   * Creates a new SpyDataSource
   */
  public SpyDataSource(String name)
  {
    if (name == null)
      name = "";
    else if (name.indexOf('?') >= 0)
      name = name.substring(0, name.indexOf('?'));
    
    if (! "".equals(name))
      _name = name + ".";
    else
      _name = "";
  }

  /**
   * Creates a connection id.
   */
  public String createConnectionId(DriverConfig driver)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(_name);
    
    if (driver != null)
      sb.append('d').append(driver.getIndex()).append(".");
    
    sb.append(_connIdCount.getAndIncrement());
    
    return sb.toString();
  }
}
