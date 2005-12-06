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

package com.caucho.quercus.env;

import java.io.InputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Locale;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ByteToChar;
import com.caucho.vfs.MultipartStream;

import com.caucho.util.Log;
import com.caucho.util.L10N;
import com.caucho.util.IntMap;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.QuercusExitException;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.page.PhpPage;

import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.AbstractClassDef;

import com.caucho.quercus.resources.StreamContextResource;

/**
 * Handling of POST requests.
 */
public class Post {
  static void fillPost(ArrayValue post, HttpServletRequest request)
  {
    InputStream is = null;
    
    try {
      if (! request.getMethod().equals("POST"))
	return;

      String contentType = request.getHeader("Content-Type");

      if (contentType == null ||
	  ! contentType.startsWith("multipart/form-data"))
	return;

      String boundary = getBoundary(contentType);

      is = request.getInputStream();
      
      MultipartStream ms = new MultipartStream(Vfs.openRead(is), boundary);
      // ms.setEncoding(javaEncoding);

      readMultipartStream(ms, post);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
	if (is != null)
	  is.close();
      } catch (IOException e) {
      }
    }
  }

  private static void readMultipartStream(MultipartStream ms,
					  ArrayValue post)
    throws IOException
  {
    ReadStream is;

    while ((is = ms.openRead()) != null) {
      String attr = (String) ms.getAttribute("content-disposition");

      if (attr == null || ! attr.startsWith("form-data")) {
        // XXX: is this an error?
        continue;
      }

      String name = getAttribute(attr, "name");
      String filename = getAttribute(attr, "filename");

      StringBuilder value = new StringBuilder();
      int ch;

      while ((ch = is.read()) >= 0) {
	value.append((char) ch);
      }

      post.put(name, value.toString());
    }
  }

  private static String getBoundary(String contentType)
  {
    int i = contentType.indexOf("boundary=");
    if (i < 0)
      return null;

    i += "boundary=".length();

    int length = contentType.length();

    char ch;

    if (length <= i)
      return null;
    else if ((ch = contentType.charAt(i)) == '\'') {
      StringBuilder sb = new StringBuilder();
      
      for (i++; i < length && (ch = contentType.charAt(i)) != '\''; i++) {
	sb.append(ch);
      }

      return sb.toString();
    }
    else if (ch == '"') {
      StringBuilder sb = new StringBuilder();
      
      for (i++; i < length && (ch = contentType.charAt(i)) != '"'; i++) {
	sb.append(ch);
      }

      return sb.toString();
    }
    else {
      StringBuilder sb = new StringBuilder();
      
      for (; i < length && (ch = contentType.charAt(i)) != ' ' && ch != ';'; i++) {
	sb.append(ch);
      }

      return sb.toString();
    }
  }

  private static String getAttribute(String attr, String name)
  {
    if (attr == null)
      return null;
    
    int length = attr.length();
    int i = attr.indexOf(name);
    if (i < 0)
      return null;

    for (i += name.length(); i < length && attr.charAt(i) != '='; i++) {
    }
    
    for (i++; i < length && attr.charAt(i) == ' '; i++) {
    }

    StringBuilder value = new StringBuilder();
    
    if (i < length && attr.charAt(i) == '\'') {
      for (i++; i < length && attr.charAt(i) != '\''; i++)
        value.append(attr.charAt(i));
    }
    else if (i < length && attr.charAt(i) == '"') {
      for (i++; i < length && attr.charAt(i) != '"'; i++)
        value.append(attr.charAt(i));
    }
    else if (i < length) {
      char ch;
      for (; i < length && (ch = attr.charAt(i)) != ' ' && ch != ';'; i++)
        value.append(ch);
    }

    return value.toString();
  }
}

