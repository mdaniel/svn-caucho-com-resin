/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.resin.mbean;

import java.util.Date;

import com.caucho.server.resin.ResinServer;

/**
 * Management interface for the server.
 */
public interface ResinServerMBean {
  /**
   * Returns the server id, the value of "-server id"
   */
  public String getServerId();
  
  /**
   * Returns the config file, the value of "-conf foo.conf"
   */
  public String getConfigFile();

  /**
   * Returns the initial start time.
   */
  public Date getInitialStartTime();

  /**
   * Returns the last restart time.
   */
  public Date getStartTime();
}
