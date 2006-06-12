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

import java.util.*;
import java.io.File;

/**
 * Abstract API for datatyping for files.
 */
abstract public class FileTypeMap {

  private static WeakHashMap _fileTypeMaps = new WeakHashMap();

  public FileTypeMap()
  {
  }
  
  /**
   * Returns the content-type of the given file.
   */
  public abstract String getContentType(File file);
  
  /**
   * Returns the content-type of the given filename.
   */
  public abstract String getContentType(String filename);

  /**
   * Sets the default file type.
   */
  public static void setDefaultFileTypeMap(FileTypeMap map)
  {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl==null)
      cl = CommandMap.class.getClassLoader();

    _fileTypeMaps.put(cl, map);
  }

  /**
   * Gets the default file type.
   */
  public static FileTypeMap getDefaultFileTypeMap()
  {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl==null)
      cl = CommandMap.class.getClassLoader();

    FileTypeMap fileTypeMap =
      (FileTypeMap)_fileTypeMaps.get(cl);

    if (fileTypeMap==null) {
      fileTypeMap = new MimetypesFileTypeMap();
      _fileTypeMaps.put(cl, fileTypeMap);
    }

    return fileTypeMap;
  }
}
