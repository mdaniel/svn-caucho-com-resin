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

package com.caucho.loader;

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.make.DependencyContainer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;
import java.io.*;
import java.util.zip.*;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class JarMap {
  private static final AtomicReference<JarList> _key
    = new AtomicReference<JarList>();
  
  // list of the jars in the directory
  private JarList []_entries;
  private int _mask;
  private int _size;

  /**
   * Creates a new jar map
   */
  public JarMap()
  {
    _entries = new JarList[128];
    _mask = _entries.length - 1;
  }

  public JarList add(String name, JarEntry entry)
  {
    // XXX: efficiency issues

    int length = name.length();
    char []cbuf = new char[length];
    name.getChars(0, length, cbuf, 0);

    return add(cbuf, length, entry);
  }
    
  public JarList add(char []name, int length, JarEntry entry)
  {
    if (_entries.length <= _size)
      resize();
    
    JarList key = new JarList();

    key.init(name, length);

    key._entry = entry;

    int hash = key.hashCode() & _mask;

    for (JarList ptr = _entries[hash]; ptr != null; ptr = ptr._nextHash) {
      if (ptr.equals(key)) {
	key._next = ptr._next;
	ptr._next = key;

	return ptr;
      }
    }

    _size++;
    
    key.copyName();
    key._nextHash = _entries[hash];
    _entries[hash] = key;

    return key;
  }

  public JarList get(String name)
  {
    JarList key = _key.getAndSet(null);

    if (key == null)
      key = new JarList();
    
    key.init(name);

    int hash = key.hashCode() & _mask;
    
    for (JarList ptr = _entries[hash]; ptr != null; ptr = ptr._nextHash) {
      if (ptr.equals(key)) {
	_key.set(key);
	
	return ptr;
      }
    }

    _key.set(key);

    return null;
  }

  private void resize()
  {
  }

  public void clear()
  {
    _size = 0;

    for (int i = 0; i < _entries.length; i++)
      _entries[i] = null;
  }

  static final class JarList {
    JarList _nextHash;
    
    JarList _next;
    JarEntry _entry;

    char []_name;
    int _length;

    JarList()
    {
    }
    
    JarList(JarEntry entry, JarList next)
    {
      _entry = entry;
      _next = next;
    }

    void init(char []name, int length)
    {
      _name = name;
      _length = length;
    }

    void init(String name)
    {
      int length = name.length();
      
      if (_name == null || _name.length <= length)
	_name = new char[length];

      name.getChars(0, length, _name, 0);

      _length = length;
    }

    int getLength()
    {
      return _length;
    }

    void copyName()
    {
      char []newName = new char[_length];
      System.arraycopy(_name, 0, newName, 0, _length);
      _name = newName;
    }

    JarEntry getEntry()
    {
      return _entry;
    }

    JarList getNext()
    {
      return _next;
    }

    @Override
    public int hashCode()
    {
      char []name = _name;
      int hash = 37;
      
      for (int i = _length - 1; i >= 0; i--) {
	hash = 65521 * hash + name[i];
      }

      return hash;
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
	return true;
      else if (! (o instanceof JarList))
	return false;

      JarList entry = (JarList) o;

      int length = _length;

      if (length != entry._length)
	return false;

      char []nameA = _name;
      char []nameB = entry._name;

      for (int i = length - 1; i >= 0; i--) {
	if (nameA[i] != nameB[i])
	  return false;
      }

      return true;
    }

    public String toString()
    {
      return (getClass().getSimpleName()
	      + "[" + new String(_name, 0, _length) + "]");
    }
  }
}
