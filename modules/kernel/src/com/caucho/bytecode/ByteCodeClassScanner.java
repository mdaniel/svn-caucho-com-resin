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

package com.caucho.bytecode;

import com.caucho.loader.enhancer.ScanMatch;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.*;

/**
 * Scans for matching classes.
 */
public class ByteCodeClassScanner {
  private static final Logger log
    = Logger.getLogger(ByteCodeClassScanner.class.getName());
  private static final L10N L = new L10N(ByteCodeClassScanner.class);

  private final String _className;

  private final InputStream _is;

  private final ByteCodeClassMatcher _matcher;

  private CharBuffer _cb = new CharBuffer();

  public ByteCodeClassScanner(String className,
			      InputStream is,
			      ByteCodeClassMatcher matcher)
  {
    _className = className;

    _is = is;

    _matcher = matcher;
  }

  public boolean scan()
  {
    try {
      int magic = readInt();

      if (magic != JavaClass.MAGIC)
	throw error(L.l("bad magic number in class file"));

      _is.skip(2); // major
      _is.skip(2); // minor

      boolean isMatch = parseConstantPool();

      int modifiers = readShort();

      boolean isClassMatch = _matcher.isClassMatch(_className, modifiers);

      return isClassMatch;
    } catch (Exception e) {
      log.log(Level.WARNING,
	      "failed scanning class " + _className + "\n" + e.toString(),
	      e);

      return false;
    }
  }

  /**
   * Parses the constant pool.
   */
  public boolean parseConstantPool()
    throws IOException
  {
    int count = readShort();

    int i = 1;
    while (i < count) {
      int code = _is.read();

      if (code == ByteCodeParser.CP_LONG || code == ByteCodeParser.CP_DOUBLE)
	i += 2;
      else
	i += 1;
      
      switch (code) {
      case ByteCodeParser.CP_CLASS:
	// int utf8Index = readShort();
	
	_is.skip(2);
	break;
      
      case ByteCodeParser.CP_FIELD_REF:
	// int classIndex = readShort();
	// int nameAndTypeIndex = readShort();
	
	_is.skip(4);
	break;
      
      case ByteCodeParser.CP_METHOD_REF:
	// int classIndex = readShort();
	// int nameAndTypeIndex = readShort();

	_is.skip(4);
	break;
      
      case ByteCodeParser.CP_INTERFACE_METHOD_REF:
	// int classIndex = readShort();
	// int nameAndTypeIndex = readShort();

	_is.skip(4);
	break;
	
      case ByteCodeParser.CP_STRING:
	// int stringIndex = readShort();

	_is.skip(2);
	break;
      
      case ByteCodeParser.CP_INTEGER:
	_is.skip(4);
	break;
      
      case ByteCodeParser.CP_FLOAT:
	_is.skip(4);
	break;
      
      case ByteCodeParser.CP_LONG:
	_is.skip(8);
	break;
      
      case ByteCodeParser.CP_DOUBLE:
	_is.skip(8);
	break;
      
      case ByteCodeParser.CP_NAME_AND_TYPE:
	// int nameIndex = readShort();
	// int descriptorIndex = readShort();
	
	_is.skip(4);
	break;
      
      case ByteCodeParser.CP_UTF8:
	{
	  int length = readShort();

	  if (parseUtf8ForAnnotation(_cb, length)) {
	    if (_matcher.isAnnotationMatch(_cb))
	      return true;
	  }

	  break;
	}

      default:
	throw error(L.l("'{0}' is an unknown constant pool type.", code));
      }
    }

    return false;
  }
  
  /**
   * Parses the UTF.
   */
  private boolean parseUtf8ForAnnotation(CharBuffer cb, int len)
    throws IOException
  {
    if (len <= 0)
      return false;
    
    InputStream is = _is;

    int ch = is.read();
    len -= 1;

    // only scan annotations
    if (ch != 'L') {
      is.skip(len);
      return false;
    }
    
    cb.ensureCapacity(len);

    char []cBuf = cb.getBuffer();
    int cLen = 0;

    while (len > 0) {
      int d1 = is.read();

      if (d1 == '/') {
	cBuf[cLen++] = '.';
	
	len--;
      }
      else if (d1 < 0x80) {
	cBuf[cLen++] = (char) d1;
	
	len--;
      }
      else if (d1 < 0xe0) {
	int d2 = is.read() & 0x3f;

	cBuf[cLen++] = (char) (((d1 & 0x1f) << 6) + (d2));
	
	len -= 2;
      }
      else if (d1 < 0xf0) {
	int d2 = is.read() & 0x3f;
	int d3 = is.read() & 0x3f;

	cBuf[cLen++] = (char) (((d1 & 0xf) << 12) + (d2 << 6) + d3);
	
	len -= 3;
      }
      else
	throw new IllegalStateException();
    }

    if (cLen > 0 && cBuf[cLen - 1] == ';') {
      cb.setLength(cLen - 1);
    
      return true;
    }
    else
      return false;
  }

  /**
   * Parses a 32-bit int.
   */
  private int readInt()
    throws IOException
  {
    return ((_is.read() << 24)
	    | (_is.read() << 16)
	    | (_is.read() << 8)
	    | (_is.read()));
  }

  /**
   * Parses a 16-bit int.
   */
  private int readShort()
    throws IOException
  {
    int c1 = _is.read();
    int c2 = _is.read();

    return ((c1 << 8) | c2);
  }

  /**
   * Returns an error message.
   */
  private IllegalStateException error(String message)
  {
    return new IllegalStateException(_className + ": " + message);
  }
}
