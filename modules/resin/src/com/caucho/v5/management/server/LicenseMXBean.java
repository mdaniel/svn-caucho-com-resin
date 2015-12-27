/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
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

package com.caucho.v5.management.server;

import java.sql.Date;

import com.caucho.v5.jmx.Description;
import com.caucho.v5.jmx.server.ManagedObjectMXBean;

@Description("Represents a single loaded license (may be invalid)")
public interface LicenseMXBean extends ManagedObjectMXBean
{
  @Description("Path to the license file")
  public String getPath();
  
  @Description("Description of the license")
  public String getDescription();
  
  @Description("The date the license was issued")
  public long getIssueDate();
  
  @Description("The date the license expires; once expired in this way the license is no longer valid for any purpose")
  public long getExpireDate();
  
  @Description("The date after which versions of the software can not be used; the license will still work for versions released before this date")
  public long getVersionExpireDate();

  @Description("Maximum time that the server can be active before it must be restarted")
  public long getServerTimeout();
  
  @Description("Description of when the license expires")
  public String getExpireMessage();
    
  @Description("If the license was invalid this will contain the cause")
  public String getErrorMessage();
  
  @Description("Is the license valid")
  public boolean isValid();
  
  @Description("Is this a non-expiring version license")
  public boolean isVersionLicense();
}
