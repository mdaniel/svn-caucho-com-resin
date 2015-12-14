/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Charles Reich
 */

package com.caucho.quercus.lib.zip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;

/**
 * Reads the zip header and prepares zip entries.
 */
public class ZipDirectory
{
  private int _currentIndex = -1;
  private ZipFile _in;
  private List<QuercusZipEntry> _entries = new ArrayList<QuercusZipEntry>();
  private Map<String, QuercusZipEntry> _nameToEntry = new HashMap<String, QuercusZipEntry>();

  protected ZipDirectory()
  {
    
  }
  
  public ZipDirectory(ZipFile in)
  {
    _in = in;
    for (Enumeration<? extends ZipEntry> i = _in.entries(); i.hasMoreElements();) {
      QuercusZipEntry entry = new QuercusZipEntry(i.nextElement(), _in);
      _entries.add(entry);
      _nameToEntry.put(entry.zip_entry_name(), entry);
    }
  }
  
  /**
   * Closes the previous entry and returns the next entry's metadata.
   */
  public QuercusZipEntry zip_read()
    throws IOException
  {
    _currentIndex++;

    if (_currentIndex >= _entries.size()) {
      return null;
    } else {
      return _entries.get(_currentIndex);
    }
  }

  public boolean zip_close()
  {
    try {
      _in.close();
    } catch (IOException e) {
    } finally {
      _in = null;
    }

    return true;
  }

  public Value getFromName (Env env, String name, int length, Integer flags) {
    if (_in == null) {
      return BooleanValue.FALSE;
    }
    QuercusZipEntry entry = _nameToEntry.get(name);
    if (entry == null) {
      return BooleanValue.FALSE;
    }
    entry.zip_entry_open(env);
    Value result = entry.zip_entry_read(env, length == 0 ? Integer.MAX_VALUE : length);
    try {
      entry.zip_entry_close();
    } catch (IOException e) {
    }
    return result;
  }
  
  public Value locateName (Env env, String name, Integer flags) {
    QuercusZipEntry entry = _nameToEntry.get(name);
    if (entry == null) {
      return BooleanValue.FALSE;
    }
    return LongValue.create(_entries.indexOf(entry));
  }
  
  public boolean extractTo( Env env, String destination, Value entries) {
    File destinationFile = new File(destination);
    if (!destinationFile.isDirectory()) {
      if (!destinationFile.mkdirs()) {
        return false;
      }
    }
    for (QuercusZipEntry entry : _entries) {
      ZipEntry zipEntry = entry.getZipEntry();
      if (zipEntry.isDirectory()) {
        File directory = new File(destinationFile, zipEntry.getName());
        if (!directory.isDirectory()) {
          if (!directory.mkdirs()) {
            return false;
          }
        }
      } else {
        File file = new File(destinationFile, zipEntry.getName());
        if (!file.getParentFile().isDirectory()) {
          if (!file.getParentFile().mkdirs()) {
            return false;
          }
        }
        InputStream is = null;
        OutputStream os = null;
        try {
          byte[] buffer = new byte[1024];
          is = _in.getInputStream(zipEntry);
          os = new FileOutputStream(file);
          int bytesRead;
          while (-1 != (bytesRead = is.read(buffer))) {
            os.write(buffer, 0, bytesRead);
          }
        } catch (IOException e) {
          return false;
        } finally {
          if (is != null) {
            try {
              is.close();
            } catch (IOException e) {
            }
          }
          if (os != null) {
            try {
              os.close();
            } catch (IOException e) {
            }
          }
        }
      }
    }
    return true;
  }
  
  public String toString()
  {
    return "ZipDirectory[]";
  }
}
