/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.vfs.Vfs;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.ReadOnly;
import com.caucho.quercus.module.UsesSymbolTable;

import com.caucho.quercus.env.*;

import com.caucho.vfs.WriteStream;

public final class UnserializeReader {
  private static final L10N L = new L10N(QuercusVariableModule.class);
  
  private final char []_buffer;
  private final int _length;
    
  private int _index;

  public UnserializeReader(String s)
  {
    _buffer = s.toCharArray();
    _length = _buffer.length;
  }

  public Value unserialize(Env env)
    throws Throwable
  {
    int ch = read();

    switch (ch) {
    case 'b':
      {
	expect(':');
	long v = readInt();
	expect(';');

	return v == 0 ? BooleanValue.FALSE : BooleanValue.TRUE;
      }
      
    case 's':
      {
	expect(':');
	int len = (int) readInt();
	expect(':');
	expect('"');

	String s = readString(len);

	expect('"');
	expect(';');

	return new StringValue(s);
      }
      
    case 'i':
      {
	expect(':');
	
	long value = readInt();
	
	expect(';');

	return new LongValue(value);
      }
      
    case 'd':
      {
	expect(':');

	StringBuilder sb = new StringBuilder();
	for (ch = read(); ch >= 0 && ch != ';'; ch = read()) {
	  sb.append((char) ch);
	}

	if (ch != ';')
	  throw new IOException(L.l("expected ';'"));

	return new DoubleValue(Double.parseDouble(sb.toString()));
      }
      
    case 'a':
      {
	expect(':');
	long len = readInt();
	expect(':');
	expect('{');

	ArrayValue array = new ArrayValueImpl((int) len);
	for (int i = 0; i < len; i++) {
	  Value key = unserialize(env);
	  Value value = unserialize(env);

	  array.put(key, value);
	}

	expect('}');

	return array;
      }
      
    case 'O':
      {
	expect(':');
	int len = (int) readInt();
	expect(':');
	expect('"');

	String className = readString(len);

	expect('"');
	expect(':');
	long count = readInt();
	expect(':');
	expect('{');

	Value obj = env.getClass(className).evalNew(env, new Value[0]);
	for (int i = 0; i < count; i++) {
	  Value key = unserialize(env);
	  Value value = unserialize(env);

	  obj.putField(env, key.toString(), value);
	}

	expect('}');

	return obj;
      }
      
    case 'N':
      {
	expect(';');

	return NullValue.NULL;
      }
      
    default:
      return BooleanValue.FALSE;
    }
  }

  public final void expect(int expectCh)
    throws IOException
  {
    if (_length <= _index)
      throw new IOException(L.l("expected '{0}' at end of string",
				String.valueOf((char) expectCh)));
    
    int ch = _buffer[_index++];

    if (ch != expectCh)
      throw new IOException(L.l("expected '{0}' at '{1}'",
				String.valueOf((char) expectCh),
				String.valueOf((char) ch)));
  }

  public final long readInt()
  {
    int ch = read();
      
    long sign = 1;
    long value = 0;

    if (ch == '-') {
      sign = -1;
      ch = read();
    }
    else if (ch == '+') {
      ch = read();
    }

    for (; '0' <= ch && ch <= '9'; ch = read()) {
      value = 10 * value + ch - '0';
    }

    unread();
    
    return sign * value;
  }

  public final String readString(int len)
  {
    String s = new String(_buffer, _index, len);

    _index += len;

    return s;
  }
    
  public final int read()
  {
    if (_index < _length)
      return _buffer[_index++];
    else
      return -1;
  }
    
  public final int read(char []buffer, int offset, int length)
  {
    System.arraycopy(_buffer, _index, buffer, offset, length);

    _index += length;

    return length;
  }

  public final void unread()
  {
    _index--;
  }
}

