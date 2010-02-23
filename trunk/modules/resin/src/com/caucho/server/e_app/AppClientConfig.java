/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.server.e_app;

import com.caucho.config.types.EjbRef;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Configuration for the application.xml file.
 */
public class AppClientConfig {
  private static final L10N L = new L10N(AppClientConfig.class);

  private ArrayList<EjbRef> _ejbRefList = new ArrayList<EjbRef>();
  
  /**
   * Sets the app client version.
   */
  public void setVersion(String version)
  {
  }

  /**
   * Sets the app client id.
   */
  public void setId(String id)
  {
  }

  /**
   * Sets the description.
   */
  public void setDescription(String desc)
  {
  }

  /**
   * Sets the display-name.
   */
  public void setDisplayName(String name)
  {
  }

  /**
   * Adds an ejb-ref
   */
  public void addEjbRef(EjbRef ejbRef)
  {
    _ejbRefList.add(ejbRef);
  }
}
