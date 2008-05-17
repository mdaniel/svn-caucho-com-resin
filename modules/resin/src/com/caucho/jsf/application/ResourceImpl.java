/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

package com.caucho.jsf.application;

import com.caucho.vfs.Path;

import javax.faces.application.Resource;
import javax.faces.context.FacesContext;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.net.URL;

public class ResourceImpl extends Resource {

  private Path _path;

  public ResourceImpl(Path path,
                      String resourceName,
                      String libraryName,
                      String contentType)
  {
    _path = path;

    setResourceName(resourceName);
    setLibraryName(libraryName);
    setContentType(contentType);
  }

  public InputStream getInputStream() throws IOException
  {
    return _path.openRead();
  }

  public void writeToStream(OutputStream outputStream)
    throws IOException
  {
    _path.writeToStream(outputStream);
  }

  public String getRequestPath()
  {
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public Map<String, String> getResponseHeaders()
  {
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public URL getURL()
  {
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public boolean userAgentNeedsUpdate(FacesContext context)
  {
    if (true) throw new UnsupportedOperationException("unimplemented");

    return false;
  }
}
