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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.hessian;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import javax.ejb.*;

import com.caucho.vfs.*;
import com.caucho.util.*;
import com.caucho.ejb.*;
import com.caucho.ejb.protocol.*;
import com.caucho.hessian.io.AbstractSerializer;
import com.caucho.hessian.io.AbstractHessianOutput;

public class HomeSerializer extends AbstractSerializer {
  private static HomeSerializer _singleton = new HomeSerializer();

  static HomeSerializer create()
  {
    return _singleton;
  }
  
  public void writeObject(Object obj, AbstractHessianOutput out)
    throws IOException
  {
    if (obj instanceof AbstractEJBHome) {
      AbstractEJBHome ejbHome = (AbstractEJBHome) obj;
      AbstractServer server = ejbHome.__caucho_getServer();
      
      out.writeRemote(server.getRemoteHomeClass().getName(),
                      server.getHandleEncoder("hessian").getURL());
    }
    else if (obj instanceof AbstractEJBObject) {
      AbstractEJBObject ejbObject = (AbstractEJBObject) obj;
      AbstractServer server = ejbObject.__caucho_getServer();
      
      out.writeRemote(server.getRemoteHomeClass().getName(),
                      server.getHandleEncoder("hessian").getURL());
    }
    else {
      EJBHome ejbHome = (EJBHome) obj;
      HomeHandle handle = ejbHome.getHomeHandle();

      Class api = ejbHome.getEJBMetaData().getHomeInterfaceClass();

      out.writeRemote(api.getName(), handle.toString());
    }
  }
}
