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

package com.caucho.vfs;

import java.io.*;
import java.util.logging.*;

/**
 * Scans a zip file, returning the names
 */
public class ZipScanner
{
  private static Logger _log;
  
  private char []_cbuf = new char[256];

  private Path _path;
  private ReadStream _is;
  
  private boolean _isValid;

  private int _entries;
  private int _offset;

  private int _index;
  
  private String _name;

  /**
   * Creates a new Jar.
   *
   * @param path canonical path
   */
  public ZipScanner(Path path)
  {
    try {
      _path = path;
    
      int length = (int) path.getLength();
    
      ReadStream is = path.openRead();

      try {
	// PACK200 is a standard comment, so try skipping it first
	is.skip(length - 22 - 7);

	if (is.read() != 0x50) {
	  is.skip(6);

	  if (is.read() != 0x50)
	    return;
	
	}
      
	if (is.read() == 0x4b
	    && is.read() == 0x05
	    && is.read() == 0x06) {
	  _isValid = true;
	}

	if (_isValid) {
	  is.skip(6);

	  _entries = is.read() + (is.read() << 8);
	  is.skip(4);
	  _offset = (is.read()
		     + (is.read() << 8)
		     + (is.read() << 16)
		     + (is.read() << 24));
	}
      } finally {
	is.close();
      }
    } catch (Exception e) {
      log().log(Level.FINER, e.toString(), e);
    }
  }

  public boolean open()
    throws IOException
  {
    if (! _isValid)
      return false;

    _is = _path.openRead();
    _is.skip(_offset);
    _index = 0;

    return true;
  }

  public boolean next()
    throws IOException
  {
    if (_entries <= _index)
      return false;

    _index++;

    ReadStream is = _is;
    
    if (is.read() != 0x50
	|| is.read() != 0x4b
	|| is.read() != 0x01
	|| is.read() != 0x02) {
      throw new IOException("illegal zip format");
    }

    is.skip(2 + 2 + 2 + 2 + 2 + 2 + 4 + 4 + 4);

    int nameLen = is.read() + (is.read() << 8);
    int extraLen = is.read() + (is.read() << 8);
    int commentLen = is.read() + (is.read() << 8);

    is.skip(2 + 2 + 4 + 4);

    if (_cbuf.length < nameLen)
      _cbuf = new char[nameLen];

    char []cbuf = _cbuf;

    int k = 0;
    for (int i = 0; i < nameLen; i++) {
      int ch = is.read();

      if (ch < 0x80)
	cbuf[k++] = (char) ch;
      else if ((ch & 0xe0) == 0xc0) {
	int c2 = is.read();
	i += 1;
	cbuf[k++] = (char) (((ch & 0x1f) << 6) + (c2 & 0x3f));
      }
      else {
	int c2 = is.read();
	int c3 = is.read();
	
	i += 2;
	cbuf[k++] = (char) (((ch & 0x1f) << 12)
			    + ((c2 & 0x3f) << 6)
			    + ((c3 & 0x3f)));
      }
    }

    _name = new String(cbuf, 0, k);

    is.skip(extraLen);
    is.skip(commentLen);
    
    return true;
  }

  public String getName()
  {
    return _name;
  }

  public void close()
  {
    InputStream is = _is;
    _is = null;

    if (is != null) {
      try {
	is.close();
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(ZipScanner.class.getName());

    return _log;
  }
}
