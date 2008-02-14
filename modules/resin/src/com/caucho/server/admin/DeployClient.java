/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.server.admin;

import com.caucho.util.L10N;
import com.caucho.hessian.io.ExtSerializerFactory;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import java.io.InputStream;
import java.io.IOException;

public class DeployClient
  extends HmuxClient
  implements DeployServiceAPI
{
  private static final L10N L = new L10N(DeployClient.class);

  public DeployClient(String serverId)
  {
    super(SERVICE_NAME, serverId);
  }

  public DeployClient(String address, int port)
  {
    super(SERVICE_NAME, address, port);
  }

  protected void initExtSerializerFactory(ExtSerializerFactory factory)
  {
  }

  public Target[] getTargets()
    throws IOException
  {
    ServiceCall call = createServiceCall("getTargets");

    try {
      return call.complete(Target[].class);
    }
    finally {
      call.close();
    }
  }

  public TargetModuleID[] getAvailableModules(String moduleType)
    throws IOException
  {
    ServiceCall call = createServiceCall("getAvailableModules");

    try {
      call.writeString(moduleType);
      return call.complete(TargetModuleID[].class);
    }
    finally {
      call.close();
    }
  }

  public void distribute(Target[] targets,
                         InputStream deploymentPlan,
                         InputStream archiveIs)
  {
    if (true) throw new UnsupportedOperationException("unimplemented");
  }

  public void start(TargetModuleID[] ids)
  {
    if (true) throw new UnsupportedOperationException("unimplemented");
  }

  public void stop(TargetModuleID[] ids)
  {
    if (true) throw new UnsupportedOperationException("unimplemented");
  }

  public void undeploy(TargetModuleID[] ids)
  {
    if (true) throw new UnsupportedOperationException("unimplemented");
  }
}
