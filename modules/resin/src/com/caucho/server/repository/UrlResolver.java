/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.repository;

import com.caucho.config.ConfigException;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;
import com.caucho.server.cache.TempFileInode;
import com.caucho.server.cache.TempFileManager;
import com.caucho.server.resin.Resin;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempBuffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The module repository holds the module jars for osgi and ivy.
 */
public class UrlResolver extends Resolver
{
  private static final Logger log
    = Logger.getLogger(UrlResolver.class.getName());

  private TempFileManager _tempFileManager;

  public UrlResolver()
  {
    _tempFileManager = Resin.getCurrent().getTempFileManager();
  }
  
  public DataSource resolveArtifact(String org,
				    String module,
				    String artifact,
				    String rev,
				    String ext)
  {
    String urlString = resolveArtifactString(org, module, artifact, rev, ext);

    TempFileInode inode = _tempFileManager.createInode();

    try {
      String sha1 = loadChecksum(urlString, "sha1");
      String md5 = null;

      if (sha1 == null)
	md5 = loadChecksum(urlString, "md5");

      System.out.println("SHA1: " + sha1);
      System.out.println("MD5: " + md5);
      
      URL url = new URL(urlString);

      URLConnection conn = url.openConnection();

      if (conn == null)
	return null;

      InputStream is = null;
      try {
	conn.connect();
	int length = conn.getContentLength();
	
	is = conn.getInputStream();

	OutputStream out = inode.openOutputStream();

	TempBuffer tempBuffer = TempBuffer.allocate();
	byte []buffer = tempBuffer.getBuffer();
	
	int readLength = 0;
	int len;

	log.info("ModuleRepository[] loading " + urlString);

	while ((len = is.read(buffer, 0, buffer.length)) > 0) {
	  out.write(buffer, 0, len);
	  readLength += len;
	}

	out.close();

	TempBuffer.free(tempBuffer);

	InodeDataSource dataSource = new InodeDataSource(inode);
	inode = null;

	return dataSource;
      } finally {
	if (is != null)
	  is.close();
      }
    } catch (MalformedURLException e) {
      throw ConfigException.create(e);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    } finally {
      if (inode != null)
	inode.free();
    }
  }

  private String loadChecksum(String urlString, String ext)
  {
    InputStream is = null;
    
    try {
      log.finer(urlString + " looking for checksum " + ext);

      URL url = new URL(urlString);

      is = url.openStream();
      StringBuilder sb = new StringBuilder();
      int ch;

      while (Character.isWhitespace((ch = is.read()))) {
      }

      for (; ch >= 0 && ! Character.isWhitespace(ch); ch = is.read()) {
	sb.append((char) ch);
      }
      
      log.fine(urlString + "." + ext + " loaded " + sb);

      return sb.toString();
    } catch (FileNotFoundException e) {
      log.finer(urlString + "." + ext + " does not exist");
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      try {
	if (is != null)
	  is.close();
      } catch (IOException e) {
      }
    }

    return null;
  }
}
