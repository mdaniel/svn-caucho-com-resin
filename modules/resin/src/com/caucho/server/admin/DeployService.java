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

import com.caucho.bam.*;
import com.caucho.config.*;
import com.caucho.git.*;
import com.caucho.hemp.broker.*;
import com.caucho.server.resin.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

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

public class DeployService extends GenericService
{
  private static final Logger log
    = Logger.getLogger(DeployService.class.getName());

  private static final L10N L = new L10N(DeployService.class);

  private Resin _resin;
  private Path _gitRoot;
  private GitRepository _git;

  DeployService(BamBroker adminBroker)
  {
    _resin = Resin.getCurrent();
    
    setBroker(adminBroker);
    setName("deploy");
  }

  @Override
  public void init()
  {
    getBroker().addService(this);

    Path root = _resin.getRootDirectory();

    // QA
    if (root instanceof MemoryPath)
      root = Vfs.lookup("file:/tmp/caucho/qa");

    _gitRoot = root.lookup(".git");
    _git = new GitRepository(_gitRoot);

    try {
      _git.initDb();
    } catch (IOException e) {
      throw ConfigException.create(e);
    }
  }

  public void start()
  {
  }
}