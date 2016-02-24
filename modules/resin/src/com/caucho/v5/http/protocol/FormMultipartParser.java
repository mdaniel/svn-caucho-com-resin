/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.protocol;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Part;

import com.caucho.v5.http.webapp.WebAppResinBase;
import com.caucho.v5.io.TempBuffer;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.HashMapImpl;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.MultipartStream;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.WriteStream;

/**
 * Multipart form handling.
 */
class FormMultipartParser {
  private static final Logger log
    = Logger.getLogger(FormMultipartParser.class.getName());
  static final L10N L = new L10N(FormMultipartParser.class);
  
  static void parsePostData(HashMapImpl<String,String[]> table,
                            List<Part> parts,
                            ReadStream rawIs, String boundary,
                            RequestCauchoBase request,
                            String javaEncoding,
                            long uploadMax,
                            long fileUploadMax,
                            long lengthMax)
    throws IOException
  {
    MultipartStream ms = new MultipartStream(rawIs, boundary);
    ms.setEncoding(javaEncoding);
    ReadStream is;

    while ((is = ms.openRead()) != null) {
      String attr = (String) ms.getAttribute("content-disposition");

      if (attr == null || ! attr.startsWith("form-data")) {
        // XXX: is this an error?
        continue;
      }

      String name = getAttribute(attr, "name");
      String filename = getAttribute(attr, "filename");
      String contentType = getAttribute(attr, "content-type");
      String value = null;
      PathImpl tempFile = null;
      
      if (contentType == null)
        contentType = ms.getAttribute("content-type");

      if (name == null) {
        // XXX: is this an error?
        continue;
      }
      else if (filename != null) {
        PathImpl tempDir = CauchoUtil.getWorkPath().lookup("form");
        try {
          tempDir.mkdirs();
        } catch (IOException e) {
        }
        tempFile = tempDir.createTempFile("form", ".tmp");
        request.addCloseOnExit(tempFile);

        WriteStream os = tempFile.openWrite();

        TempBuffer tempBuffer = TempBuffer.allocate();
        byte []buf = tempBuffer.buffer();

        int totalLength = 0;

        try {
          int len;

          while ((len = is.read(buf, 0, buf.length)) > 0) {
            os.write(buf, 0, len);
            totalLength += len;
          }
        } finally {
          os.close();

          TempBuffer.free(tempBuffer);
          tempBuffer = null;
        }

        if (uploadMax > 0 && uploadMax < tempFile.length()) {
          String msg = L.l("multipart form data '{0}' too large",
                           "" + tempFile.length());
          
          long fileLength = tempFile.length();
          tempFile.remove();

          throw formError(msg, fileLength, request);
        } else if (fileUploadMax > 0 && fileUploadMax < tempFile.length()){
          String msg = L.l("multipart form data part '{0}':'{1}' is greater then the accepted value of '{2}'",
                           name, "" + tempFile.length(), fileUploadMax);

          tempFile.remove();

          throw new IllegalStateException(msg);
        }
        else if (tempFile.length() != totalLength) {
          String msg = L.l("multipart form upload failed (possibly due to full disk).");
          
          long fileLength = tempFile.length();
          tempFile.remove();
          
          throw formError(msg, fileLength, request);
        }

        WebAppResinBase webApp = request.getWebApp();
        if (webApp != null && webApp.isMultipartFormEnabled()) {
          // server/136u, server/136v, #2578
          if (table.get(name + ".filename") == null) {
            table.put(name, new String[] { tempFile.getNativePath() });
            table.put(name + ".file", new String[] { tempFile.getNativePath() });
            table.put(name + ".filename", new String[] { filename });
            table.put(name + ".content-type", new String[] { contentType });
          }
          else {
            addTable(table, name, tempFile.getNativePath());
            addTable(table, name + ".file", tempFile.getNativePath());
            addTable(table, name + ".filename", filename);
            addTable(table, name + ".content-type", contentType);
          }
        }

      if (log.isLoggable(Level.FINE))
          log.fine("mp-file: " + name + "(filename:" + filename + ")");
      } else {
        CharBuffer valueBuffer = new CharBuffer();
        int ch;
        long totalLength = 0;

        for (ch = is.readChar(); ch >= 0; ch = is.readChar()) {
          valueBuffer.append((char) ch);
          totalLength++;
          
          if (lengthMax < totalLength) {
            String msg = L.l("multipart form upload failed because field '{0}' exceeds max length {1}",
                             name, lengthMax);

            throw formError(msg, totalLength, request);
          }
        }

        value = valueBuffer.toString();

        if (log.isLoggable(Level.FINE))
          log.fine("mp-form: " + name + "=" + value);

        addTable(table, name, value);
        
        if (contentType != null)
          addTable(table, name + ".content-type", contentType);
      }
      
      LinkedHashMap<String,List<String>> headers
        = new LinkedHashMap<>(ms.getHeaders());

      parts.add(request.createPart(name,
                                   contentType,
                                   headers,
                                   tempFile,
                                   value));
    }

    if (! ms.isComplete()) {
      throw formError(L.l("End of post before multipart-mime boundary"),
                      -1, request);
    }
  }
  
  private static IOException formError(String msg, long length,
                                       RequestCauchoBase request)
    throws IOException
  {
    log.fine(request.getRequestURI() + ": " + msg);
    
    request.setAttribute("caucho.multipart.form.error", msg);
    request.setAttribute("caucho.multipart.form.error.size",
                         new Long(length));
    
    throw new IOException(msg);
  }

  private static void addTable(HashMapImpl<String,String[]> table, String name, String value)
  {
    String []oldArray = table.get(name);

    if (oldArray != null) {
      String []newArray = new String[oldArray.length + 1];
      System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
      newArray[oldArray.length] = value;
      table.put(name, newArray);
    }
    else
      table.put(name, new String[] {value});
  }

  private static String getAttribute(String attr, String name)
  {
    if (attr == null)
      return null;
    
    int length = attr.length();
    
    int i = findAttribute(attr, name);
    if (i < 0)
      return null;

    if (length <= i || attr.charAt(i) != '=')
      return null;
    
    /*
    for (i += name.length(); i < length && attr.charAt(i) != '='; i++) {
    }
    */
    
    for (i++; i < length && attr.charAt(i) == ' '; i++) {
    }

    CharBuffer value = CharBuffer.allocate();
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

    return value.close();
  }
  
  private static int findAttribute(String attribute, String name)
  {
    int length = attribute.length();
    int nameLength = name.length();
    
    for (int i = 0; i < length - nameLength; i++) {
      if (attribute.regionMatches(true, i, name, 0, nameLength)) {
        char ch;
        
        if (i > 0
            && (ch = attribute.charAt(i - 1)) != ' '
            && ch != ';'
            && ch != '\t') {
          continue;
        }
        
        int j = i + nameLength;
        
        for (; j < length; j++) {
          ch = attribute.charAt(j);
          
          if (ch == '=')
            return j;
          else if (ch == ' ' || ch == '\t')
            continue;
          else
            break;
        }
      }
    }
    
    return -1;
  }
}
