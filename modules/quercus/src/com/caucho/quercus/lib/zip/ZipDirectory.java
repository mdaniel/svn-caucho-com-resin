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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
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
    return entry.zip_entry_read(env, length == 0 ? Integer.MAX_VALUE : length);
  }
  
  public String toString()
  {
    return "ZipDirectory[]";
  }
}
