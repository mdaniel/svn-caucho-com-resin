/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.http.host;

import com.caucho.http.webapp.WebAppContainer;
import com.caucho.http.webapp.WebAppContainerResin;

/**
 * Resin's virtual host implementation.
 */
public class HostResin extends Host
{
  /**
   * Creates the webApp with its environment loader.
   */
  public HostResin(HostBuilderResin builder)
  {
    super(builder);
  }
  
  @Override
  protected WebAppContainer createWebAppContainer()
  {
    return new WebAppContainerResin(getHttpContainer(), 
                               this, 
                               getRootDirectory(),
                               getClassLoader(), 
                               getLifecycle());
  }
}
