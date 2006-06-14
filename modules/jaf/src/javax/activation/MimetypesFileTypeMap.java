/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
import java.io.*;
import java.util.logging.*;

/**
 * API for finding the mime-type for a file.
 */
public class MimetypesFileTypeMap extends FileTypeMap {

  private static Logger log =
    Logger.getLogger("javax.activation.MimetypesFileTypeMap");

  /**
   * The key is a filename extension, the value is the mime type
   */
  private HashMap _mimeTypesByExtension = new HashMap();

  /**
   * Default constructor.
   */
  public MimetypesFileTypeMap()
  {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();

    if (cl==null)
      cl = MimetypesFileTypeMap.class.getClassLoader();

    // added in reverse priority order
    try {
      addMimeTypes(cl.getResourceAsStream("META-INF/mimetypes.default"));
    }
    catch (IOException e) {
      // cyclic dependency in <clinit>
      if (log != null)
	log.log(Level.FINER, e.toString(), e);
    }

    try {
      addMimeTypes(cl.getResourceAsStream("META-INF/mime.types"));
    }
    catch (IOException e) {
      // cyclic dependency in <clinit>
      if (log != null)
	log.log(Level.FINER, e.toString(), e);
    }

    try {
      addMimeTypes(new File(System.getProperty("java.home")+"/lib/mime.types"));
    }
    catch (IOException e) {
      // cyclic dependency in <clinit>
      if (log != null)
	log.log(Level.FINER, e.toString(), e);
    }

    try {
      addMimeTypes(new File(System.getProperty("user.home")+"/.mime.types"));
    }
    catch (IOException e) {
      // cyclic dependency in <clinit>
      if (log != null)
	log.log(Level.FINER, e.toString(), e);
    }
  }
  
  /**
   * Fills the map with programmatic entries from the input stream.
   */
  public MimetypesFileTypeMap(String mimeTypeFileName)
    throws IOException
  {
    InputStream is = new FileInputStream(mimeTypeFileName);

    try {
      addMimeTypes(is);
    } finally {
      is.close();
    }
  }

  /**
   * Fills the map with programmatic entries from the input stream.
   */
  public MimetypesFileTypeMap(InputStream is)
  {
    this();
    try {
      addMimeTypes(is);
    }
    catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Prepend the values to the registry.
   */
  public void addMimeTypes(String mimeTypes)
    throws IOException
  {
    addMimeTypes(new StringReader(mimeTypes));
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
    int dot = fileName.indexOf('.');

    if (dot==-1)
      return "application/octet-stream";

    String extension = fileName.substring(fileName.lastIndexOf('.')+1);

    String mimeType = (String)_mimeTypesByExtension.get(extension);

    if (mimeType != null)
      return mimeType;

    return "application/octet-stream";
  }

  private void addMimeTypes(File f)
    throws IOException
  {
    InputStream is = new FileInputStream(f);
    try {
      addMimeTypes(is);
    } finally {
      is.close();
    }
  }

  private void addMimeTypes(InputStream is)
    throws IOException
  {
    if (is==null) return;
    addMimeTypes(new InputStreamReader(is));
  }

  private void addMimeTypes(Reader r)
    throws IOException
  {
    if (r==null) return;
    BufferedReader br = new BufferedReader(r);
    
    for(String s = br.readLine(); s!=null; s = br.readLine()) {
      int p = s.indexOf('#');
      
      if (p >= 0)
	s = s.substring(0, p);
      
      String []values = s.split("[ \t]");
      
      if (values.length < 2)
	continue;
      
      for(int i=1; i<values.length; i++)
	_mimeTypesByExtension.put(values[i], values[0]);
    }
  }

}
