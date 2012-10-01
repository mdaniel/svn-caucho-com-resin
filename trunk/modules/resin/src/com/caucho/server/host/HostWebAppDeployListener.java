/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.server.host;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositoryTagListener;
import com.caucho.vfs.Path;

/**
 * The generator for the host deploy
 */
public class HostWebAppDeployListener implements RepositoryTagListener {
  private final static Logger log
    = Logger.getLogger(HostWebAppDeployListener.class.getName());
  
  private final HostExpandDeployGenerator _gen;
  private final String _idPrefix;

  HostWebAppDeployListener(HostExpandDeployGenerator gen)
  {
    _gen = gen;
    
    _idPrefix = gen.getContainer().getStageTag() + "/webapp";

    getRepository().addListener(_idPrefix, this);
  }
  
  Path getExpandDirectory()
  {
    return _gen.getExpandDirectory();
  }
  
  Repository getRepository()
  {
    return _gen.getRepository();
  }
  
  void update()
  {
    for (String key : getRepository().getTagMap().keySet()) {
      if (! key.startsWith(_idPrefix)) {
        continue;
      }
      
      String host = key.substring(_idPrefix.length() + 1);
      
      int p = host.indexOf('/');
      
      if (p >= 0) {
        host = host.substring(0, p);
      }
      
      if (host.length() == 0) {
        continue;
      }
      
      try {
        getExpandDirectory().lookup(host).mkdirs();
      } catch (IOException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  @Override
  public void onTagChange(String tag)
  {
    update();
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getExpandDirectory() + "]";
  }
}
