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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/**
 * API for finding the mime-type for a file.
 */
public class MimetypesFileTypeMap extends FileTypeMap {
  /**
   * Default constructor.
   */
  public MimetypesFileTypeMap()
  {
  }
  
  /**
   * Fills the map with programmatic entries from the input stream.
   */
  public MimetypesFileTypeMap(InputStream is)
  {
  }
  
  /**
   * Fills the map with programmatic entries from the input stream.
   */
  public MimetypesFileTypeMap(String mimeTypeFileName)
    throws IOException
  {
  }

  /**
   * Prepend the values to the registry.
   */
  public void addMimeTypes(String mimeTypes)
  {
  }

  /**
   * Prepend the values to the registry.
   */
  public String getContentType(File file)
  {
    return getContentType(file.getName());
  }

  /**
   * Prepend the values to the registry.
   */
  public String getContentType(String fileName)
  {
    return "application/octet-stream";
  }
}
