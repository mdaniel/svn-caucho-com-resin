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

package com.caucho.j2ee.deployserver;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.j2ee.deployclient.DeploymentStatusImpl;
import com.caucho.j2ee.deployclient.ProgressObjectImpl;
import com.caucho.j2ee.deployclient.TargetImpl;
import com.caucho.j2ee.deployclient.TargetModuleIDImpl;
import com.caucho.log.Log;
import com.caucho.management.server.HostMXBean;
import com.caucho.server.webapp.Application;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for the deployments.
 */
public class DeployServlet extends GenericServlet {
  private static final L10N L = new L10N(DeployServlet.class);
  private static final Logger log = Log.open(DeployServlet.class);
  
  private static final int GET_TARGETS = 1;
  private static final int DISTRIBUTE = 2;
  private static final int GET_AVAILABLE_MODULES = 3;
  private static final int UNDEPLOY = 4;

  private static final IntMap _methodMap = new IntMap();

  private TargetImpl []_targets;

  private Path _deployPath;

  private HostMXBean _hostMXBean;

  /**
   * Sets the deploy path.
   */
  public void setDeployPath(Path path)
  {
    _deployPath = path;
  }

  /**
   * Initialize the servlet.
   */
  public void init()
    throws ServletException
  {
    try {
      TargetImpl target = new TargetImpl("Resin-target", "default target");

      _targets = new TargetImpl[] { target };

      if (_deployPath == null)
	throw new ServletException(L.l("<deploy-path> is required."));

      _deployPath.mkdirs();

      Application app = (Application) getServletContext();

      _hostMXBean = app.getHostAdmin();
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
  
  /**
   * Serves the deployment.
   */
  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException
  {
    InputStream is = req.getInputStream();
    OutputStream os = res.getOutputStream();

    HessianInput in = new HessianInput(is);
    HessianOutput out = new HessianOutput(os);

    in.readCall();
    String method = in.readMethod();

    try {
      switch (_methodMap.get(method)) {
      case GET_TARGETS:
	in.completeCall();
	out.startReply();
	out.writeObject(_targets);
	out.completeReply();
	break;
	
      case GET_AVAILABLE_MODULES:
	{
	  String type = in.readString();
	  in.completeCall();
	  
	  out.startReply();
	  out.writeObject(getAvailableModules(type));
	  out.completeReply();
	  break;
	}
      
      case DISTRIBUTE:
	{
	  TargetImpl []targets = (TargetImpl[]) in.readObject(TargetImpl[].class);
	  DeploymentPlan plan = new DeploymentPlan();

          InputStream planIs = in.readInputStream();

          try {
	    new Config().configure(plan, planIs);
	  } finally {
            planIs.close();
	  }

	  InputStream archiveIs = in.readInputStream();

	  ProgressObject po = distribute(targets, plan, archiveIs);
	  
	  in.completeCall();
	  out.startReply();

	  out.writeObject(po);
	  out.completeReply();
	  break;
	}
      
      case UNDEPLOY:
	{
	  TargetModuleID []targetIDs;
	  targetIDs = (TargetModuleID []) in.readObject(TargetModuleIDImpl[].class);

	  ProgressObject po = undeploy(targetIDs);
	  
	  in.completeCall();
	  out.startReply();
	  out.writeObject(po);
	  out.completeReply();
	  break;
	}
      
      default:
	out.startReply();
	out.writeFault("UnknownMethod", "UnknownMethod: " + method, null);
	out.completeReply();
	break;
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      out.startReply();
      out.writeFault(e.toString(), e.toString(), e);
      out.completeReply();
    }
  }

  private ProgressObject distribute(TargetImpl []targets,
				    DeploymentPlan plan,
				    InputStream archiveIs)
    throws IOException
  {
    if (archiveIs == null)
      return null;

    Path path = _deployPath.lookup(plan.getArchiveName());

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, L.l("creating local file {0}", path));

    WriteStream os = path.openWrite();

    try {
      os.writeStream(archiveIs);
    } finally {
      os.close();
    }

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, L.l("distribute {0}", plan));

    _hostMXBean.expandEarDeploy(plan.getName());
    
    Path metaPath = _deployPath.lookup(plan.getMetaPathName());

    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("metaPath {0}", metaPath));

    /**
     * XXX: this doesn;t work, it stops the deployment of the ear
     * because the file it creates is newer than the ear
    ArrayList<DeploymentPlan.ExtFile> _extFileList;
    _extFileList = plan.getExtFileList();

    for (int i = 0; i < _extFileList.size(); i++) {
      DeploymentPlan.ExtFile extFile = _extFileList.get(i);

      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, L.l("extFile {0}", extFile));

      Path filePath = metaPath.lookup(extFile.getName());
      Node node = extFile.getData();

      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, L.l("file {0}", filePath));

      filePath.getParent().mkdirs();

      XmlPrinter.print(filePath, node);
    }
     */

    log.info("deploying: " + plan.getName());
    
    Throwable configException = null;

    try {
      _hostMXBean.startEarDeploy(plan.getName());
      // XXX: would need to get the specific wars in the ear
      _hostMXBean.updateWebAppDeploy("/" + plan.getName());
    } catch (Throwable e) {
      configException = e;
      
      log.log(Level.FINE, e.toString(), e);
    }
    

    TargetModuleIDImpl id = new TargetModuleIDImpl(targets[0],
						   plan.getArchiveName());

    /*
    if ("rar".equals(plan.getArchiveType())) {
      TargetModuleIDImpl parent = new TargetModuleIDImpl(targets[0],
							 "host");
      id.setParentTargetModuleID(id);
    }
    */

    ProgressObjectImpl progress
      = new ProgressObjectImpl(new TargetModuleID[] { id });

    DeploymentStatusImpl status = new DeploymentStatusImpl();
    progress.setDeploymentStatus(status);

    if (configException != null) {
      status.setFailed(true);
      
      if (configException instanceof ConfigException)
	status.setMessage(configException.getMessage());
      else
	status.setMessage(configException.toString());
    }

    return progress;
  }

  private TargetModuleID []getAvailableModules(String type)
    throws IOException
  {
    TargetImpl target = _targets[0];

    ArrayList<TargetModuleID> idList = new ArrayList<TargetModuleID>();
    
    String []list = _deployPath.list();
    
    for (int i = 0; i < list.length; i++) {
      String name = list[i];
      Path path = _deployPath.lookup(name);

      if (type.equals("war") && name.endsWith(".war")) {
	TargetModuleIDImpl id = new TargetModuleIDImpl(target, name);

	if (! idList.contains(id))
	  idList.add(id);
      }
      else if (type.equals("war") &&
	       path.isDirectory() && ! name.startsWith("_")) {
	name = name + ".war";
	
	TargetModuleIDImpl id = new TargetModuleIDImpl(target, name);

	if (! idList.contains(id))
	  idList.add(id);
      }
      else if (type.equals("rar") && name.endsWith(".rar")) {
	TargetModuleIDImpl id = new TargetModuleIDImpl(target, name);

	if (! idList.contains(id))
	  idList.add(id);
      }
      else if (type.equals("rar") &&
	       path.isDirectory() && name.startsWith("_rar_")) {
	name = name.substring(5) + ".rar";
	
	TargetModuleIDImpl id = new TargetModuleIDImpl(target, name);

	if (! idList.contains(id))
	  idList.add(id);
      }
    }

    TargetModuleID []idArray = new TargetModuleID[idList.size()];
    idList.toArray(idArray);

    return idArray;
  }

  private ProgressObject undeploy(TargetModuleID []ids)
    throws IOException
  {
    if (ids == null || ids.length == 0)
      return null;

    for (int i = 0; i < ids.length; i++) {
      TargetModuleID targetModuleID = ids[i];
      log.info(L.l("undeploying {0}", targetModuleID.getModuleID()));
      stop(ids[i]);
    }

    for (int i = 0; i < ids.length; i++) {
      String name = ids[i].getModuleID();

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("deleting {0}", _deployPath.lookup(name)));

      _deployPath.lookup(name).remove();

      if (name.endsWith(".war")) {
	name = name.substring(0, name.length() - 4);

        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, L.l("deleting {0}", _deployPath.lookup(name)));

        _deployPath.lookup(name).removeAll();
      }
      else if (name.endsWith(".ear")) {
	name = "_ear_" + name.substring(0, name.length() - 4);

        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, L.l("deleting {0}", _deployPath.lookup(name)));

        _deployPath.lookup(name).removeAll();
      }
      else if (name.endsWith(".rar")) {
	name = "_rar_" + name.substring(0, name.length() - 4);

        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, L.l("deleting {0}", _deployPath.lookup(name)));

        _deployPath.lookup(name).removeAll();
      }

      Throwable configException = null;
      
      try {
	_hostMXBean.updateWebAppDeploy(name);
      } catch (Throwable e) {
	log.log(Level.FINE, e.toString(), e);
	
	configException = e;
      }
    }
    
    return new ProgressObjectImpl(ids);
  }
  
  private void stop(TargetModuleID target)
    throws IOException
  {
  }
  
  static {
    _methodMap.put("getTargets", GET_TARGETS);
    _methodMap.put("distribute", DISTRIBUTE);
    _methodMap.put("getAvailableModules", GET_AVAILABLE_MODULES);
    _methodMap.put("undeploy", UNDEPLOY);
  }
}

