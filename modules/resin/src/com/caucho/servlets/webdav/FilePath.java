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

package com.caucho.servlets.webdav;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import javax.servlet.http.*;
import javax.servlet.*;

import org.xml.sax.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;

import com.caucho.server.http.*;

/**
 * Represents a virtual filesystem.
 */
public class FilePath extends ApplicationPath {
  private Path _root;

  public FilePath()
  {
  }

  public FilePath(Path root)
  {
    try {
      root.mkdirs();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    setRoot(root);
  }

  /**
   * path the root path.
   */
  public void setRoot(Path path)
  {
    _root = path;
  }

  /**
   * Returns the root path.
   */
  public Path getRoot()
  {
    return _root;
  }

  /**
   * Returns the underlying path.
   */
  protected Path getPath(String path,
                         HttpServletRequest request,
                         ServletContext app)
    throws IOException
  {
    return _root.lookup("./" + path);
  }

  public String toString()
  {
    return "FilePath[" + _root + "]";
  }
}
