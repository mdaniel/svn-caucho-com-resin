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

package javax.faces.application;

import javax.faces.context.FacesContext;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.net.URL;

/**
 * @since 2.0
 */
public abstract class ResourceWrapper
  extends Resource
{

  protected abstract Resource getWrapped();

  public String getResourceName()
  {
    return getWrapped().getResourceName();
  }

  public void setResourceName(String resourceName)
  {
    getWrapped().setResourceName(resourceName);
  }

  public String getLibraryName()
  {
    return getWrapped().getLibraryName();
  }

  public void setLibraryName(String libraryName)
  {
    getWrapped().setLibraryName(libraryName);
  }

  public String getContentType()
  {
    return getWrapped().getContentType();
  }

  public void setContentType(String contentType)
  {
    getWrapped().setContentType(contentType);
  }

  public InputStream getInputStream()
    throws IOException
  {
    return getWrapped().getInputStream();
  }

  public String getRequestPath()
  {
    return getWrapped().getRequestPath();
  }


  public Map<String, String> getResponseHeaders()
  {
    return getWrapped().getResponseHeaders();
  }

  public URL getURL()
  {
    return getWrapped().getURL();
  }

  public boolean userAgentNeedsUpdate(FacesContext context)
  {
    return getWrapped().userAgentNeedsUpdate(context);
  }
}
