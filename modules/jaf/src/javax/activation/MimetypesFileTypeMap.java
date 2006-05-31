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
import java.io.*;

/**
 * API for finding the mime-type for a file.
 */
public class MimetypesFileTypeMap extends FileTypeMap {

  /**
   * The key is a filename extension, the value is the mime type
   */
  private HashMap _mimeTypesByExtension = new HashMap();

  /**
   * Default constructor.
   */
  public MimetypesFileTypeMap()
  {
    ClassLoader cl = getClass().getClassLoader();

    // added in reverse priority order
    //addMimeTypes(new ReadStream(cl.getResourceStream("META-INF/mimetypes.default")).openRead());
    //addMimeTypes(new ReadStream(cl.getResourceStream("META-INF/mime.types")).openRead());
    //addMimeTypes(Vfs.lookup("file:"+System.getProperty("java.home")+"/lib/mime.types"));
    //addMimeTypes(Vfs.lookup("file:"+System.getProperty("user.home")+"/.mime.types"));
  }
  
  /**
   * Fills the map with programmatic entries from the input stream.
   */
  public MimetypesFileTypeMap(InputStream is)
  {
    this();
    addMimeTypes(is);
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

  private void addMimeTypes(InputStream is)
  {
    try
      {
	BufferedReader mimeTypes =
	  new BufferedReader(new InputStreamReader(is));
	// XXX: use CharScanner
	for(String s = mimeTypes.readLine(); s!=null;
	    s = mimeTypes.readLine())
	  {
	    s = s.trim();
	    if (s.charAt(0)=='#') continue;
	    int space = s.indexOf(' ');
	    if (space==-1) continue;
	    String mimeType = s.substring(0, space);
	    while(true) {
	      while(space < s.length() && s.charAt(space)==' ') space++;
	      if (space==s.length()) break;
	      int nextSpace = s.indexOf(' ', space+1);
	      if (nextSpace==-1) nextSpace = s.length() + 1;
	      String extension = s.substring(space+1, nextSpace);
	      _mimeTypesByExtension.put(extension, mimeType);
	      space = nextSpace;
	    }
	  }
      }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Prepend the values to the registry.
   */
  public void addMimeTypes(String mimeTypes)
  {
    addMimeTypes(new ByteArrayInputStream(mimeTypes.getBytes()));
  }

  /**
   * Prepend the values to the registry.
   */
  public String getContentType(File file)
  {
    // XXX: could do better here?
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
    String extension =
      fileName.substring(fileName.lastIndexOf('.')+1);
    String mimeType =
      (String)_mimeTypesByExtension.get(extension);
    if (mimeType != null) return mimeType;
    // XXX
    return null;
  }
}
