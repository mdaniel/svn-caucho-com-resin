/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.types;

import com.caucho.v5.vfs.PathImpl;

/**
 * Configuration for the init-param pattern.
 */
//public class ResourceEnvRef extends BaseRef {

public class ResourceEnvRef extends ResourceRef {
  public ResourceEnvRef()
  {
    //setResType(EJBContext.class);
  }

  public ResourceEnvRef(PathImpl modulePath, String sourceEjbName)
  {
    // super(modulePath, sourceEjbName);
    
    //setResType(EJBContext.class);
  }

  /**
   * Sets the name
   */
  public void setResourceEnvRefName(String name)
  {
    setResRefName(name);
  }

  /**
   * Sets the type
   */
  public void setResourceEnvRefType(Class<?> type)
  {
    // _type = type;
    
    setResType(type);
  }
}
