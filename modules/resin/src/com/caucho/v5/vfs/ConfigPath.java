/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.vfs;

import java.util.Map;

import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.SchemeMap;
import com.caucho.v5.vfs.VfsOld;

/**
 * ConfigPath implements remote configuration scheme.
 */
public class ConfigPath extends PathImpl {
  private static final EnvironmentLocal<RemotePwd> _remotePath
    = new EnvironmentLocal<RemotePwd>();

  /**
   * Creates the path (for the scheme)
   */
  ConfigPath()
  {
    super(SchemeMap.getNullSchemeMap());
  }

  /**
   * Sets the remote.
   */
  public static void setRemote(PathImpl remotePath)
  {
    _remotePath.set(new RemotePwd(remotePath, VfsOld.lookup()));
  }

  /**
   * Path-specific lookup.  Path implementations will override this.
   *
   * @param userPath the user's lookup() path.
   * @param newAttributes the attributes for the new path.
   * @param newPath the lookup() path
   * @param offset offset into newPath to start lookup.
   *
   * @return the found path
   */
  @Override
  public PathImpl schemeWalk(String userPath,
                         Map<String,Object> newAttributes,
                         String newPath, int offset)
  {
    throw new UnsupportedOperationException();
    /*
    Path path = Vfs.lookup();

    path = path.schemeWalk(userPath, newAttributes, newPath, offset);

    RemotePwd remotePwd = _remotePath.get();

    if (remotePwd == null)
      return path;

    Path configPwd = remotePwd.getPwd();
    Path remotePath = remotePwd.getRemote();

    String pathName = path.getFullPath();
    String configName = configPwd.getFullPath();

    if (pathName.startsWith(configName))
      pathName = pathName.substring(configName.length());

    return remotePath.schemeWalk(userPath, newAttributes, pathName, 0);
    */
  }

  /**
   * Returns the scheme.
   */
  @Override
  public String getScheme()
  {
    PathImpl path = VfsOld.lookup();

    return path.getScheme();
  }

  /**
   * Returns the path.
   */
  public String getPath()
  {
    PathImpl path = VfsOld.lookup();

    return path.getPath();
  }

  static class RemotePwd {
    PathImpl _remote;
    PathImpl _pwd;

    RemotePwd(PathImpl remote, PathImpl pwd)
    {
      _remote = remote;
      _pwd = pwd;
    }

    PathImpl getRemote()
    {
      return _remote;
    }

    PathImpl getPwd()
    {
      return _pwd;
    }
  }
}
