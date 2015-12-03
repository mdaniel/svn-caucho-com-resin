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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.inject;

import com.caucho.vfs.*;

import java.util.ArrayList;

/**
 * Scanned data for the root context
 */
public class ScanRootContext  {
  private final Path _root;
  
  private ArrayList<String> _classList = new ArrayList<String>();
  private boolean _isScanComplete;
  
  public ScanRootContext(Path root, String packageRoot)
  {
    _root = root;
  }

  public Path getRoot()
  {
    return _root;
  }

  public void addClassName(String className)
  {
    _classList.add(className);
  }

  public ArrayList<String> getClassNameList()
  {
    return _classList;
  }

  public boolean isScanComplete()
  {
    return _isScanComplete;
  }

  public void setScanComplete(boolean isScanComplete)
  {
    _isScanComplete = isScanComplete;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _root + "]";
  }
}
