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

package com.caucho.ejb;

import java.io.*;
import java.util.*;
import javax.ejb.*;

import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.util.*;

public class EJBMetaDataImpl implements EJBMetaData, Serializable {
  private EJBHome home;
  
  private Class homeInterfaceClass;
  private Class remoteInterfaceClass;
  private Class primaryKeyClass;

  private boolean isSession;
  private boolean isStatelessSession;

  /**
   * Null constructor for serialization.
   */
  public EJBMetaDataImpl() {}

  /**
   * Create a new meta data.
   */
  EJBMetaDataImpl(EJBHome home,
                  Class homeInterfaceClass,
                  Class remoteInterfaceClass,
                  Class primaryKeyClass)
  {
    this.home = home;
    this.homeInterfaceClass = homeInterfaceClass;
    this.remoteInterfaceClass = remoteInterfaceClass;
    this.primaryKeyClass = primaryKeyClass;
  }

  public EJBHome getEJBHome()
  {
    return home;
  }

  public Class getHomeInterfaceClass()
  {
    return homeInterfaceClass;
  }

  public Class getRemoteInterfaceClass()
  {
    return remoteInterfaceClass;
  }

  public Class getPrimaryKeyClass()
  {
    return primaryKeyClass;
  }

  void setSession(boolean isSession)
  {
    this.isSession = isSession;
  }

  public boolean isSession()
  {
    return isSession;
  }

  void setStatelessSession(boolean isStatelessSession)
  {
    this.isStatelessSession = isStatelessSession;
  }

  public boolean isStatelessSession()
  {
    return isStatelessSession;
  }

  public String toString()
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append("MetaData[");
    if (isSession() && isStatelessSession())
      cb.append("stateless-session");
    else if (isSession())
      cb.append("session");
    else
      cb.append("entity");

    if (homeInterfaceClass != null)
      cb.append(" home:" + homeInterfaceClass.getName());
    if (remoteInterfaceClass != null)
      cb.append(" remote:" + remoteInterfaceClass.getName());
    if (primaryKeyClass != null)
      cb.append(" key:" + primaryKeyClass.getName());

    cb.append("]");

    return cb.close();
  }
}
