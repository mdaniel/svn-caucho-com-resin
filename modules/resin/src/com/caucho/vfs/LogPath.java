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

package com.caucho.vfs;

import java.io.*;
import java.util.*;

import com.caucho.util.*;

class LogPath extends FilesystemPath {
  static LogPath ROOT;

  LogPath(FilesystemPath root, String userPath,
	  Map<String,Object> attributes, String path)
  {
    super(root != null ? root : ROOT, userPath, path);

    if (_root == null) {
      ROOT = this;
      _root = ROOT = new LogPath(ROOT, "/", null, "/");
      ROOT._root = ROOT;
    }
  }

  protected Path fsWalk(String userPath,
			Map<String,Object> attributes,
			String path)
  {
    return new LogPath(_root, userPath, attributes, path);
  }

  public String getScheme()
  { 
    return "log";
  }

  public String getURL()
  {
    return "log:" + getPath();
  }

  public StreamImpl openWriteImpl() throws IOException
  {
    return new LogStream(getFullPath());
  }

  public StreamImpl openAppendImpl() throws IOException
  {
    return new LogStream(getFullPath());
  }
}

