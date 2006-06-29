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

package javax.xml.ws;
import java.security.*;

/**
 * This class defines web service permissions. Web service Permissions are
 * identified by name (also referred to as a "target name") alone. There are no
 * actions associated with them. The following permission target name is
 * defined: publishEndpoint The publishEndpoint permission allows publishing a
 * web service endpoint using the publish methods defined by the
 * javax.xml.ws.Endpoint class. See Also:Endpoint, BasicPermission, Permission,
 * Permissions, SecurityManager, Serialized Form
 */
public final class WebServicePermission extends BasicPermission {

  /**
   * Creates a new permission with the specified name. Parameters:name - the
   * name of the WebServicePermission
   */
  public WebServicePermission(String name)
  {
    super(name);
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a new permission with the specified name and actions. The actions
   * parameter is currently unused and it should be null. Parameters:name - the
   * name of the WebServicePermissionactions - should be null
   */
  public WebServicePermission(String name, String actions)
  {
    super(name);
    throw new UnsupportedOperationException();
  }

}

