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

package com.caucho.iiop.any;

import com.caucho.iiop.*;

import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

import java.io.IOException;

public class SequenceTypeCode extends AbstractTypeCode {
  private TypeCode _contentType;
  
  public SequenceTypeCode(TypeCode contentType)
  {
    _contentType = contentType;
  }

  public TCKind kind()
  {
    return TCKind.tk_sequence;
  }

  @Override
  public TypeCode content_type()
  {
    return _contentType;
  }

  @Override
  public int length()
  {
    return 0;
  }

  public static SequenceTypeCode readTypeCode(IiopReader is)
  {
    // XXX: recursive
    int len = is.read_sequence_length();
    int endian = is.read_octet();

    TypeCode elementType = is.read_TypeCode();
    int length = is.read_ulong();

    return new SequenceTypeCode(elementType);
  }

  public static void writeTypeCode(IiopWriter os, TypeCode tc)
  {
    try {
      os.write_long(tc.kind().value());

      EncapsulationMessageWriter encap
	= new EncapsulationMessageWriter();
      Iiop12Writer subOut = new Iiop12Writer();
    
      subOut.init(encap);
      subOut.write(0);
    
      subOut.write_TypeCode(tc.content_type());
      subOut.write_ulong(tc.length());
    
      encap.close();
      os.write_long(encap.getOffset());
      encap.writeToWriter(os);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads the value from the stream.
   */
  public Object readValue(IiopReader is)
  {
    return is.read_value();
  }

  /**
   * Writes the value to the stream.
   */
  public void writeValue(IiopWriter os, Object v)
  {
    os.write_value((java.io.Serializable) v);
  }

  public String toString()
  {
    return "SequenceTypeCode[" + _contentType + "]";
  }
}
