/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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

package com.caucho.iiop;

import java.io.IOException;

public class UnmarshallObject {
  public final static int BOOLEAN = 0;
  public final static int BYTE = 1;
  public final static int SHORT = 2;
  public final static int CHAR = 3;
  public final static int INT = 4;
  public final static int LONG = 5;
  public final static int FLOAT = 6;
  public final static int DOUBLE = 7;
  
  public final static int STRING = 8;
  
  public final static int BOOLEAN_ARRAY = 9;
  public final static int BYTE_ARRAY = 10;
  public final static int SHORT_ARRAY = 11;
  public final static int CHAR_ARRAY = 12;
  public final static int INT_ARRAY = 13;
  public final static int LONG_ARRAY = 14;
  public final static int FLOAT_ARRAY = 15;
  public final static int DOUBLE_ARRAY = 16;
  public final static int STRING_ARRAY = 17;

  private int code;

  private UnmarshallObject(int code)
  {
    this.code = code;
  }

  public Object unmarshall(IiopReader reader)
    throws IOException
  {
    switch (code) {
    case BOOLEAN:
      return new Boolean(reader.read_boolean());
    case BYTE:
      return new Byte(reader.read_octet());
    case SHORT:
      return new Short(reader.read_short());
    case CHAR:
      return new Character(reader.read_wchar());
    case INT:
      return new Integer(reader.read_long());
    case LONG:
      return new Long(reader.read_longlong());
    case FLOAT:
      return new Float(reader.read_float());
    case DOUBLE:
      return new Double(reader.read_double());
    case STRING:
      return reader.read_wstring();
    case BOOLEAN_ARRAY:
    {
      boolean []array = new boolean[reader.read_sequence_length()];
      reader.read_boolean_array(array, 0, array.length);
      return array;
    }
    case BYTE_ARRAY:
    {
      byte []array = new byte[reader.read_sequence_length()];
      reader.read_octet_array(array, 0, array.length);
      return array;
    }
    case CHAR_ARRAY:
    {
      char []array = new char[reader.read_sequence_length()];
      reader.read_wchar_array(array, 0, array.length);
      return array;
    }
    case SHORT_ARRAY:
    {
      short []array = new short[reader.read_sequence_length()];
      reader.read_short_array(array, 0, array.length);
      return array;
    }
    case INT_ARRAY:
    {
      int []array = new int[reader.read_sequence_length()];
      reader.read_long_array(array, 0, array.length);
      return array;
    }
    case LONG_ARRAY:
    {
      long []array = new long[reader.read_sequence_length()];
      reader.read_longlong_array(array, 0, array.length);
      return array;
    }
    case FLOAT_ARRAY:
    {
      float []array = new float[reader.read_sequence_length()];
      reader.read_float_array(array, 0, array.length);
      return array;
    }
    case DOUBLE_ARRAY:
    {
      double []array = new double[reader.read_sequence_length()];
      reader.read_double_array(array, 0, array.length);
      return array;
    }
    case STRING_ARRAY:
    {
      String []array = new String[reader.read_sequence_length()];
      for (int i = 0; i < array.length; i++)
        array[i] = reader.read_wstring();
      return array;
    }
    default:
      throw new IOException("unknown type");
    }
  }
}
