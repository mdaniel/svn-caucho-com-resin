/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.db.sql;

import java.io.InputStream;
import java.sql.SQLException;

import com.caucho.db.table.Column;
import com.caucho.util.QDate;

public class Data {
  private static final int NULL = Column.NONE;
  private static final int BOOLEAN = Column.BOOLEAN;
  private static final int STRING = Column.VARCHAR;
  private static final int INTEGER = Column.INT;
  private static final int LONG = Column.LONG;
  private static final int DOUBLE = Column.DOUBLE;
  private static final int DATE = Column.DATE;
  private static final int BYTES = Column.BINARY;
  private static final int BINARY_STREAM = Column.VARBINARY;
  // private static final int EXPR = BINARY + 1;

  private Column _column;

  private int _type;
  private boolean _booleanData;
  private String _stringData;
  private int _intData;
  private long _longData;
  private double _doubleData;
  // private Expr _expr;
  
  private InputStream _binaryStream;
  private int _streamLength;
  private byte []_bytes;
  
  public void clear()
  {
    _type = NULL;
  }

  public void setColumn(Column column)
  {
    _column = column;
  }
  
  public Column getColumn()
  {
    return _column;
  }
  
  public int getType()
  {
    return _type;
  }

  /**
   * Returns true for a null value.
   */
  public boolean isNull()
  {
    return _type == NULL;
  }

  /**
   * Sets the value as a string.
   */
  public void setString(String value)
  {
    if (value == null)
      _type = NULL;
    else {
      _type = STRING;
      _stringData = value;
    }
  }
  
  public void setDate(long value)
  {
    _type = DATE;
    _longData = value;
  }
  
  public boolean isBinaryStream()
  {
    return _type == BINARY_STREAM;
  }

  /**
   * Sets the value as a stream.
   */
  public void setBinaryStream(InputStream is, int length)
  {
    _type = BINARY_STREAM;
    _binaryStream = is;
    _streamLength = length;
  }
  
  public InputStream getBinaryStream()
  {
    switch (_type) {
    case NULL:
      return null;

    case BINARY_STREAM:
      return _binaryStream;

    default:
      throw new UnsupportedOperationException(String.valueOf(_type));
    }
  }

  public void setBytes(byte []bytes)
  {
    _type = BYTES;
    _bytes = bytes;
  }
  
  public byte []getBytes()
  {
    switch (_type) {
    case NULL:
      return null;
      
    case BYTES:
      return _bytes;
      
    default:
      throw new UnsupportedOperationException(_type + " " + toString());
    }
  }

  /**
   * Returns the value as a string.
   */
  public String getString()
  {
    switch (_type) {
    case NULL:
      return null;
      
    case BOOLEAN:
      return _booleanData ? "true" : "false";
      
    case INTEGER:
      return String.valueOf(_intData);
      
    case LONG:
      return String.valueOf(_longData);
      
    case DOUBLE:
      return String.valueOf(_doubleData);

    case STRING:

    case DATE:
      return QDate.formatISO8601(_longData);

    case BYTES:
      {
        StringBuilder sb = new StringBuilder();
        int len = _bytes.length;
        for (int i = 0; i < len; i++) {
          sb.append((char) (_bytes[i] & 0xff));
        }

        return sb.toString();
      }

    default:
      throw new UnsupportedOperationException(String.valueOf(_type));
    }
  }

  /**
   * Sets the value as a boolean.
   */
  public void setBoolean(boolean value)
  {
    _type = BOOLEAN;
    _booleanData = value;
  }

  /**
   * Returns the value as a boolean
   */
  public int getBoolean()
  {
    switch (_type) {
    case NULL:
      return Expr.UNKNOWN;
      
    case BOOLEAN:
      return _booleanData ? Expr.TRUE : Expr.FALSE;
      
    case INTEGER:
      return _intData != 0 ? Expr.TRUE : Expr.FALSE;
      
    case LONG:
      return _longData != 0 ? Expr.TRUE : Expr.FALSE;
      
    case DOUBLE:
      return _doubleData != 0 ? Expr.TRUE : Expr.FALSE;

    case STRING:
      return _stringData.equalsIgnoreCase("y") ? Expr.TRUE : Expr.FALSE;

    default:
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Sets the value as an integer.
   */
  public void setInt(int value)
  {
    _type = INTEGER;
    _intData = value;
  }

  /**
   * Returns the value as an integer.
   */
  public int getInt()
  {
    switch (_type) {
    case NULL:
      return 0;
      
    case BOOLEAN:
      return _booleanData ? 1 : 0;
      
    case INTEGER:
      return _intData;
      
    case LONG:
      return (int) _longData;
      
    case DOUBLE:
      return (int) _doubleData;

    case STRING:
      return Integer.parseInt(_stringData);

    default:
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Sets the value as a long.
   */
  public void setLong(long value)
  {
    _type = LONG;
    _longData = value;
  }

  /**
   * Returns the value as a long.
   */
  public long getLong()
  {
    switch (_type) {
    case NULL:
      return 0;
      
    case BOOLEAN:
      return _booleanData ? 1 : 0;
      
    case INTEGER:
      return _intData;
      
    case LONG:
      return _longData;
      
    case DOUBLE:
      return (long) _doubleData;

    case STRING:
      return Long.parseLong(_stringData);

    default:
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns the value as a date.
   */
  public long getDate()
  {
    switch (_type) {
    case NULL:
      return 0;
      
    case BOOLEAN:
      return _booleanData ? 1 : 0;
      
    case INTEGER:
      return _intData;
      
    case LONG:
      return _longData;
      
    case DOUBLE:
      return (long) _doubleData;

    case STRING:
      return Long.parseLong(_stringData);

    default:
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Sets the value as a double.
   */
  public void setDouble(double value)
  {
    _type = DOUBLE;
    _doubleData = value;
  }

  /**
   * Returns the value as a double.
   */
  public double getDouble()
  {
    switch (_type) {
    case NULL:
      return 0;
      
    case BOOLEAN:
      return _booleanData ? 1 : 0;
      
    case INTEGER:
      return _intData;
      
    case LONG:
      return _longData;
      
    case DOUBLE:
      return _doubleData;

    case STRING:
      return Double.parseDouble(_stringData);

    default:
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns the value as a double.
   */
  public void copyTo(Data dst)
  {
    switch (_type) {
    case NULL:
      dst.setString(null);
      break;
      
    case BOOLEAN:
      dst.setBoolean(_booleanData);
      break;
      
    case INTEGER:
      dst.setInt(_intData);
      break;
      
    case LONG:
      dst.setLong(_longData);
      break;
      
    case DOUBLE:
      dst.setDouble(_doubleData);
      break;

    case STRING:
      dst.setString(_stringData);
      break;

    default:
      throw new UnsupportedOperationException();
    }
  }
  
  /*
  public int evalToBuffer(byte []buffer, int offset)
  {
    if (_type == BYTES) {
      System.arraycopy(_bytes, 0, buffer, offset, _bytes.length);

      return _bytes.length;
    }
    else
      return evalToBuffer(buffer, offset, _type);
  }
  */
  
  /**
   * Evaluates the expression to a buffer
   *
   * @param result the result buffer
   *
   * @return the length of the result
   */
  /*
  private int evalToBuffer(byte []buffer,
                           int offset,
                           int typecode)
    throws SQLException
  {
    if (_type == BYTES) {
      System.arraycopy(_bytes, 0, buffer, offset, _bytes.length);

      return _bytes.length;
    }
    else if (_type == NULL) {
      return -1;
    }
    else
      return super.evalToBuffer(context, buffer, offset, typecode);
  }
  */

  /**
   * Returns a hash code
   */
  public int hashCode()
  {
    switch (_type) {
    case NULL:
      return 17;
      
    case BOOLEAN:
      return _booleanData ? 1 : 0;
      
    case INTEGER:
      return _intData;
      
    case LONG:
      return (int) _longData;
      
    case DOUBLE:
      return (int) _doubleData;

    case STRING:
      return _stringData.hashCode();

    default:
      return 97;
    }
  }

  /**
   * Returns the equality test.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! getClass().equals(o.getClass()))
      return false;

    Data data = (Data) o;

    if (_type != data._type)
      return false;

    switch (_type) {
    case NULL:
      return false;
      
    case BOOLEAN:
      return _booleanData == data._booleanData;
      
    case INTEGER:
      return _intData == data._intData;
      
    case LONG:
      return _longData == data._longData;
      
    case DOUBLE:
      return _doubleData == data._doubleData;

    case STRING:
      return _stringData.equals(data._stringData);

    default:
      return false;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getString() + "]";
  }
}
