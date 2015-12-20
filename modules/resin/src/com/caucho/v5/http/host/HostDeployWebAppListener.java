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

package com.caucho.v5.http.host;

import io.baratine.files.Watch;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

/**
 * The generator for the host deploy
 */
public class HostDeployWebAppListener
{
  private final static Logger log
    = Logger.getLogger(HostDeployWebAppListener.class.getName());
  
  private final Path _deployPath;
  private final DeployGeneratorHostExpand _gen;

  private HostWatch _watch;

  HostDeployWebAppListener(DeployGeneratorHostExpand gen)
  {
    _gen = gen;
    
    // String clusterId = gen.getContainer().getClusterId();
    
    _deployPath = Vfs.lookup("bfs:///system/webapps");

    _watch = new HostWatch();

    _deployPath.watch(_watch);
    
    update();
    // getRepository().addListener(_idPrefix, this);
  }
  
  Path getExpandDirectory()
  {
    return _gen.getExpandDirectory();
  }
  
  /*
  Repository getRepository()
  {
    return _gen.getRepository();
  }
  */
  
  void update()
  {
    try {
      for (String host : _deployPath.list()) {
        getExpandDirectory().lookup(host).mkdirs();
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /*
  @Override
  public void onTagChange(String tag)
  {
    update();
  }
  */
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getExpandDirectory() + "]";
  }
  
  private class HostWatch implements Watch {
    @Override
    public void onUpdate(String path)
    {
      update();
    }
  }
}
