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

package com.caucho.v5.http.webapp;

import java.util.ArrayList;

import com.caucho.v5.bytecode.scan.ScanClass;
import com.caucho.v5.bytecode.scan.ScanListenerByteCode;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.vfs.PathImpl;

/**
 * scans a class hierarchy.
 */
class ScanClassClassHierarchy implements ScanClass {
  private ScanListenerClassHierarchy _listener;
  private String _name;
  
  ScanClassClassHierarchy(ScanListenerClassHierarchy listener, String name)
  {
    _listener = listener;
    _name = name;
  }

  @Override
  public void addSuperClass(char[] buffer, int offset, int length)
  {
    _listener.addEntry(_name, new String(buffer, offset, length));
  }

  @Override
  public void addInterface(char[] buffer, int offset, int length)
  {
    _listener.addEntry(_name, new String(buffer, offset, length));
  }

  @Override
  public void addClassAnnotation(char[] buffer, int offset, int length)
  {
  }

  @Override
  public void addPoolString(char[] buffer, int offset, int length)
  {
  }

  @Override
  public boolean finishScan()
  {
    return false;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
