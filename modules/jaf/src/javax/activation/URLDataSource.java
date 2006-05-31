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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.activation;

import java.net.URL;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * An abstraction of serialized data.
 */
public class URLDataSource implements DataSource {
  private URL _url;

  public URLDataSource(URL url)
  {
    _url = url;
  }
  
  /**
   * Returns an InputStream to the data.
   */
  public InputStream getInputStream()
    throws IOException
  {
    return _url.openStream();
  }
  
  /**
   * Returns an OutputStream to write the data.
   */
  public OutputStream getOutputStream()
    throws IOException
  {
    return _url.openConnection().getOutputStream();
  }
  
  /**
   * Returns the MIME type of the data.  Note: this method attempts to
   * call the openConnection method on the URL. If this method fails,
   * or if a content type is not returned from the URLConnection,
   * getContentType returns "application/octet-stream" as the content
   * type.
   */
  public String getContentType()
  {
    try
      {
	String mimeType =
	  _url.openConnection().getContentType();

	if (mimeType!=null)
	  return mimeType;
      } catch (Throwable t) {
	// deliberately ignored
      }
    return "application/octet-stream";
  }

  /**
   * Returns the URL.
   */
  public URL getURL()
  {
    return _url;
  }
  
  /**
   * Returns the name of the data.  e.g. the last component of
   * a filename, not the entire path name.
   */
  public String getName()
  {
    return getURL().getPath();
  }
}
