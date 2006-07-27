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

package javax.xml.bind;
import javax.xml.namespace.*;
import java.math.*;
import java.util.*;

/** XXX */
public interface DatatypeConverterInterface {

  /** XXX */
  abstract String parseAnySimpleType(String lexicalXSDAnySimpleType);


  /** XXX */
  abstract byte[] parseBase64Binary(String lexicalXSDBase64Binary);


  /** XXX */
  abstract boolean parseBoolean(String lexicalXSDBoolean);


  /** XXX */
  abstract byte parseByte(String lexicalXSDByte);


  /** XXX */
  abstract Calendar parseDate(String lexicalXSDDate);


  /** XXX */
  abstract Calendar parseDateTime(String lexicalXSDDateTime);


  /** XXX */
  abstract BigDecimal parseDecimal(String lexicalXSDDecimal);


  /** XXX */
  abstract double parseDouble(String lexicalXSDDouble);


  /** XXX */
  abstract float parseFloat(String lexicalXSDFloat);


  /** XXX */
  abstract byte[] parseHexBinary(String lexicalXSDHexBinary);


  /** XXX */
  abstract int parseInt(String lexicalXSDInt);


  /** XXX */
  abstract BigInteger parseInteger(String lexicalXSDInteger);


  /** XXX */
  abstract long parseLong(String lexicalXSDLong);


  /** XXX */
  abstract QName parseQName(String lexicalXSDQName, NamespaceContext nsc);


  /** XXX */
  abstract short parseShort(String lexicalXSDShort);


  /** XXX */
  abstract String parseString(String lexicalXSDString);


  /** XXX */
  abstract Calendar parseTime(String lexicalXSDTime);


  /** XXX */
  abstract long parseUnsignedInt(String lexicalXSDUnsignedInt);


  /** XXX */
  abstract int parseUnsignedShort(String lexicalXSDUnsignedShort);


  /** XXX */
  abstract String printAnySimpleType(String val);


  /** XXX */
  abstract String printBase64Binary(byte[] val);


  /** XXX */
  abstract String printBoolean(boolean val);


  /** XXX */
  abstract String printByte(byte val);


  /** XXX */
  abstract String printDate(Calendar val);


  /** XXX */
  abstract String printDateTime(Calendar val);


  /** XXX */
  abstract String printDecimal(BigDecimal val);


  /** XXX */
  abstract String printDouble(double val);


  /** XXX */
  abstract String printFloat(float val);


  /** XXX */
  abstract String printHexBinary(byte[] val);


  /** XXX */
  abstract String printInt(int val);


  /** XXX */
  abstract String printInteger(BigInteger val);


  /** XXX */
  abstract String printLong(long val);


  /** XXX */
  abstract String printQName(QName val, NamespaceContext nsc);


  /** XXX */
  abstract String printShort(short val);


  /** XXX */
  abstract String printString(String val);


  /** XXX */
  abstract String printTime(Calendar val);


  /** XXX */
  abstract String printUnsignedInt(long val);


  /** XXX */
  abstract String printUnsignedShort(int val);

}

