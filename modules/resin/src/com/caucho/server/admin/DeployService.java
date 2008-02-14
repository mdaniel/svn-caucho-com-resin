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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.server.admin;

import com.caucho.hessian.io.ExtSerializerFactory;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.StringValueDeserializer;
import com.caucho.hessian.io.StringValueSerializer;
import com.caucho.jmx.remote.JMXService;
import com.caucho.util.L10N;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeployService
  extends ManagementService
  implements DeployServiceAPI
{
  private static final Logger log
    = Logger.getLogger(JMXService.class.getName());

  private static final L10N L = new L10N(DeployService.class);

  private ExtSerializerFactory _extFactory;

  public DeployService(Management management)
  {
    super(management, SERVICE_NAME);
  }

  @Override
  public void start()
  {
    super.start();

    _extFactory = new ExtSerializerFactory();
    _extFactory.addSerializer(ObjectName.class,
                              new StringValueSerializer());
    _extFactory.addDeserializer(ObjectName.class,
                                new StringValueDeserializer(ObjectName.class));

    log.info(L.l("Deploy management service started"));
  }

  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    Hessian2Input in = new Hessian2Input(request.getInputStream());
    Hessian2Output out = new Hessian2Output(response.getOutputStream());

    out.findSerializerFactory().addFactory(_extFactory);

    in.startCall();

    String method = in.getMethod();

    try {
      if ("getTargets".equals(method)) {
        if (! isReadAllowed(request, response))
          return;

        in.completeCall();

        log.finer("DeployService: getTargets");

        Target[] targets = getTargets();

        out.startReply();
        out.writeObject(targets);
        out.completeReply();
        out.close();
      }
      else if ("getAvailableModules".equals(method)) {
        if (! isReadAllowed(request, response))
          return;

        String moduleType = in.readString();

        in.completeCall();

        log.finer("DeployService: getAvailableModules " + moduleType);

        TargetModuleID[] modules = getAvailableModules(moduleType);

        out.startReply();
        out.writeObject(modules);
        out.completeReply();
        out.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }


  public TargetModuleID[] getAvailableModules(String moduleType)
  {
    if (true) throw new UnsupportedOperationException("unimplemented");

    return new TargetModuleID[0];
  }

  public Target[] getTargets()
  {
    if (true) throw new UnsupportedOperationException("unimplemented");

    return new Target[0];
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