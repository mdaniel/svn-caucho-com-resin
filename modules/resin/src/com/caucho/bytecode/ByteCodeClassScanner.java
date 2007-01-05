/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scans for matching classes.
 */
public class ByteCodeClassScanner {
  static private final Logger log = Log.open(ByteCodeClassScanner.class);
  static private final L10N L = new L10N(ByteCodeClassScanner.class);

  private final String _className;

  private final byte []_buffer;
  private final int _length;

  private final ByteCodeClassMatcher _matcher;

  private int []_cpOffset = new int[256];
  private int _index;

  private CharBuffer _cb = new CharBuffer();

  public ByteCodeClassScanner(String className,
			      byte []buffer, int offset, int length,
			      ByteCodeClassMatcher matcher)
  {
    _className = className;
    
    _buffer = buffer;
    _index = offset;
    _length = length;

    _matcher = matcher;
  }

  public boolean scan()
  {
    try {
      int magic = readInt();

      if (magic != JavaClass.MAGIC)
	throw error(L.l("bad magic number in class file"));

      int minor = readShort();
      int major = readShort();

      parseConstantPool();

      int accessFlags = readShort();
      int thisClassIndex = readShort();

      String thisName = parseClass(thisClassIndex).toString();

      if (_matcher.isClassMatch(thisName))
	return true;
    
      int superClassIndex = readShort();

      int interfaceCount = readShort();
      for (int i = 0; i < interfaceCount; i++) {
	int classIndex = readShort();
      }

      int fieldCount = readShort();
      for (int i = 0; i < fieldCount; i++) {
	if (parseField())
	  return true;
      }

      int methodCount = readShort();
      for (int i = 0; i < methodCount; i++) {
	if (parseMethod())
	  return true;
      }

      int attrCount = readShort();
      for (int i = 0; i < attrCount; i++) {
	if (parseAttribute())
	  return true;
      }

      return false;
    } catch (Exception e) {
      log.warning("failed scanning class " + _className);
      log.log(Level.WARNING, e.toString(), e);

      return false;
    }
  }

  /**
   * Parses the constant pool.
   */
  public boolean parseConstantPool()
  {
    int count = readShort();

    int i = 1;
    while (i < count) {
      int code = read();
      
      parseConstantPoolEntry(code, i);

      if (code == ByteCodeParser.CP_LONG || code == ByteCodeParser.CP_DOUBLE)
	i += 2;
      else
	i += 1;
    }

    /*
    for (i = 1; i < count; i++) {
      int offset = _cpOffset[i];

      if (offset == 0)
	continue;

      int code = _buffer[offset];

      if (code == ByteCodeParser.CP_CLASS) {
	int nameIndex = ((_buffer[offset + 1] & 0xff) * 256 +
			 (_buffer[offset + 2] & 0xff));

	CharBuffer name = parseUTF8(nameIndex);

	if (_matcher.isMatch(name))
	  return true;
      }
    }
    */

    return false;
  }

  /**
   * Parses a constant pool entry.
   */
  private boolean parseConstantPoolEntry(int tag, int i)
  {
    switch (tag) {
    case ByteCodeParser.CP_CLASS:
      {
	if (_cpOffset.length <= i) {
	  int []offset = new int[2 * i];
	  System.arraycopy(_cpOffset, 0, offset, 0, _cpOffset.length);
	  _cpOffset = offset;
	}
	
	_cpOffset[i] = _index - 1;

	_index += 2;

	return false;
      }
      
    case ByteCodeParser.CP_FIELD_REF:
      {
	// int classIndex = readShort();
	// int nameAndTypeIndex = readShort();
	
	_index += 4;

	return false;
      }
      
    case ByteCodeParser.CP_METHOD_REF:
      {
	// int classIndex = readShort();
	// int nameAndTypeIndex = readShort();

	_index += 4;
	
	return false;
      }
      
    case ByteCodeParser.CP_INTERFACE_METHOD_REF:
      {
	// int classIndex = readShort();
	// int nameAndTypeIndex = readShort();

	_index += 4;
	
	return false;
      }
      
    case ByteCodeParser.CP_STRING:
      {
	// int stringIndex = readShort();
	
	_index += 2;

	return false;
      }
      
    case ByteCodeParser.CP_INTEGER:
      {
	_index += 4;

	return false;
      }
      
    case ByteCodeParser.CP_FLOAT:
      {
	_index += 4;

	return false;
      }
      
    case ByteCodeParser.CP_LONG:
      {
	_index += 8;

	return false;
      }
      
    case ByteCodeParser.CP_DOUBLE:
      {
	_index += 8;

	return false;
      }
      
    case ByteCodeParser.CP_NAME_AND_TYPE:
      {
	// int nameIndex = readShort();
	// int descriptorIndex = readShort();
	
	_index += 4;

	return false;
      }
      
    case ByteCodeParser.CP_UTF8:
      {
	if (_cpOffset.length <= i) {
	  int []offset = new int[2 * i + 1];
	  System.arraycopy(_cpOffset, 0, offset, 0, _cpOffset.length);
	  _cpOffset = offset;
	}
	
	_cpOffset[i] = _index - 1;
	
	int length = readShort();

	_index += length;

	return false;
      }

    default:
      throw error(L.l("'{0}' is an unknown constant pool type.",
		      tag));
    }
  }

  /**
   * Parses a field entry.
   */
  private boolean parseField()
  {
    int accessFlags = readShort();
    int nameIndex = readShort();
    int descriptorIndex = readShort();

    int attributesCount = readShort();

    for (int i = 0; i < attributesCount; i++) {
      if (parseAttribute())
	return true;
    }

    return false;
  }

  /**
   * Parses a method entry.
   */
  private boolean parseMethod()
  {
    int accessFlags = readShort();
    int nameIndex = readShort();
    int descriptorIndex = readShort();

    int attributesCount = readShort();
    for (int i = 0; i < attributesCount; i++) {
      if (parseAttribute())
	return true;
    }

    return false;
  }

  /**
   * Parses an attribute.
   */
  boolean parseAttribute()
  {
    int nameIndex = readShort();

    int length = readInt();
    int start = _index;

    CharBuffer name = parseUTF8(nameIndex, 0);

    if (name != null && name.matches("RuntimeVisibleAnnotations")) {
      int n = readShort();

      for (int i = 0; i < n; i++) {
	if (parseAttributeImpl())
	  return true;
      }
    }
    
    _index = start + length;

    return false;
    
    /*
    String name = _cp.getUtf8(nameIndex).getValue();

    if (name.equals("Code")) {
      CodeAttribute code = new CodeAttribute(name);
      code.read(this);
      return code;
    }
    else if (name.equals("Exceptions")) {
      ExceptionsAttribute code = new ExceptionsAttribute(name);
      code.read(this);
      return code;
    }
    */
    
  }

  /**
   * Parses an attribute.
   */
  private boolean parseAttributeImpl()
  {
    int type = readShort();

    CharBuffer name = name = parseUTF8(type, 1);
    name.setLength(name.length() - 1);

    if (_matcher.isMatch(name))
      return true;
      
    int nPairs = readShort();
    for (int j = 0; j < nPairs; j++) {
      int valueName = readShort();
	  
      parseElementValue();
    }

    return false;
  }

  private void parseElementValue()
  {
    int tag = read();

    switch (tag) {
    case 's':
      _index += 2;
      break;
    case 'e':
      _index += 4;
      break;
    case 'c':
      _index += 2;
      break;
    case 'Z':
    case 'B':
    case 'S':
    case 'I':
    case 'J':
    case 'C':
    case 'F':
    case 'D':
      _index += 2;
      break;
    case '@':
      // read annotation value
      parseAttributeImpl();
      break;
    case '[':
      {
	int n = readShort();
	for (int i = 0; i < n; i++)
	  parseElementValue();
      }
      break;
    default:
      System.out.println("UNKNOWN: " + (char) tag);
      throw new IllegalStateException("UNKNOWN TYPE: " + (char) tag);
    }
  }

  /**
   * Parses the UTF, specifically the class.
   */
  private CharBuffer parseClass(int index)
  {
    int offset = _cpOffset[index] + 1;

    return parseUTF8((_buffer[offset] & 0xff) * 256 +
		     (_buffer[offset + 1] & 0xff),
		     0);
  }
  
  /**
   * Parses the UTF, specifically the class.
   */
  private CharBuffer parseUTF8(int index, int skip)
  {
    int offset = _cpOffset[index] + 1;

    byte []bBuf = _buffer;
    
    int len = ((bBuf[offset] & 0xff) << 8) + (bBuf[offset + 1] & 0xff);

    _cb.ensureCapacity(len);

    offset += 2;
    int end = offset + len;

    char []cBuf = _cb.getBuffer();
    int cLen = -skip;

    while (offset < end) {
      int d1 = bBuf[offset] & 0xff;

      if (d1 < 0x80) {
	if (d1 == '/')
	  d1 = '.';

	if (cLen < 0)
	  cLen++;
	else
	  cBuf[cLen++] = (char) d1;

	offset++;
      }
      else if (d1 < 0xe0) {
	int d2 = bBuf[offset + 1] & 0x3f;

	if (cLen < 0)
	  cLen++;
	else
	  cBuf[cLen++] = (char) (((d1 & 0x1f) << 6) +
				 ((d2)));

	offset += 2;
      }
      else if (d1 < 0xf0) {
	int d2 = bBuf[offset + 1] & 0x3f;
	int d3 = bBuf[offset + 1] & 0x3f;

	if (cLen < 0)
	  cLen++;
	else
	  cBuf[cLen++] = (char) (((d1 & 0xf) << 12) +
				 ((d2 << 6)) +
				 ((d3)));

	offset += 2;
      }
      else
	throw new IllegalStateException();
    }
    
    _cb.setLength(cLen);
    
    return _cb;
  }

  /**
   * Parses a 64-bit int.
   */
  private long readLong()
  {
    return (((long) read() << 56) |
	    ((long) read() << 48) |
	    ((long) read() << 40) |
	    ((long) read() << 32) |
	    ((long) read() << 24) |
	    ((long) read() << 16) |
	    ((long) read() << 8) |
	    ((long) read()));
  }

  /**
   * Parses a 32-bit int.
   */
  private int readInt()
  {
    return ((read() << 24) |
	    (read() << 16) |
	    (read() << 8) |
	    (read()));
  }

  /**
   * Parses a 16-bit int.
   */
  private int readShort()
  {
    int c1 = read();
    int c2 = read();

    return ((c1 << 8) | c2);
  }

  /**
   * Reads the next byte.
   */
  private int read()
  {
    if (_index < _length)
      return _buffer[_index++] & 0xff;
    else
      return -1;
  }

  /**
   * Returns an error message.
   */
  private IllegalStateException error(String message)
  {
    return new IllegalStateException(message);
  }
}
