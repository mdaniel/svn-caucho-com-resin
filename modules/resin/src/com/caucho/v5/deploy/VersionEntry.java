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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.deploy;

public class VersionEntry {
  private final String _name;
  private final String _contextPath;
  private final String _baseContextPath;
  private final String _sha1;
  private final String _root;

  VersionEntry(String name,
               String contextPath,
               String baseContextPath,
               String sha1,
               String root)
  {
    _name = name;
    _contextPath = contextPath;
    _baseContextPath = baseContextPath;
    _sha1 = sha1;
    _root = root;
  }

  public String getName()
  {
    return _name;
  }

  public String getContextPath()
  {
    return _contextPath;
  }

  public String getBaseContextPath()
  {
    return _baseContextPath;
  }

  public String getSha1()
  {
    return _sha1;
  }

  public String getRoot()
  {
    return _root;
  }

  @Override
    public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _contextPath
            + "," + _baseContextPath
            + "," + _root + "]");
  }
}
