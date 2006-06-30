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

/**
 * The DatatypeConverterInterface is for JAXB provider use only. A JAXB
 * provider must supply a class that implements this interface. JAXB Providers
 * are required to call the DatatypeConverter.setDatatypeConverter api at some
 * point before the first marshal or unmarshal operation (perhaps during the
 * call to JAXBContext.newInstance). This step is necessary to configure the
 * converter that should be used to perform the print and parse functionality.
 * Calling this api repeatedly will have no effect - the DatatypeConverter
 * instance passed into the first invocation is the one that will be used from
 * then on. This interface defines the parse and print methods. There is one
 * parse and print method for each XML schema datatype specified in the the
 * default binding Table 5-1 in the JAXB specification. The parse and print
 * methods defined here are invoked by the static parse and print methods
 * defined in the DatatypeConverter class. A parse method for a XML schema
 * datatype must be capable of converting any lexical representation of the XML
 * schema datatype ( specified by the XML Schema Part2: Datatypes specification
 * into a value in the value space of the XML schema datatype. If an error is
 * encountered during conversion, then an IllegalArgumentException or a
 * subclass of IllegalArgumentException must be thrown by the method. A print
 * method for a XML schema datatype can output any lexical representation that
 * is valid with respect to the XML schema datatype. If an error is encountered
 * during conversion, then an IllegalArgumentException, or a subclass of
 * IllegalArgumentException must be thrown by the method. Since: JAXB1.0
 * Version: $Revision: 1.5 $ Author: Sekhar Vajjhala, Sun Microsystems, Inc.Joe
 * Fialli, Sun Microsystems Inc.Kohsuke Kawaguchi, Sun Microsystems, Inc.Ryan
 * Shoemaker,Sun Microsystems Inc. See Also:DatatypeConverter,
 * ParseConversionEvent, PrintConversionEvent
 */
public interface DatatypeConverterInterface {

  /**
   * Return a string containing the lexical representation of the simple type.
   */
  abstract String parseAnySimpleType(String lexicalXSDAnySimpleType);


  /**
   * Converts the string argument into an array of bytes.
   */
  abstract byte[] parseBase64Binary(String lexicalXSDBase64Binary);


  /**
   * Converts the string argument into a boolean value.
   */
  abstract boolean parseBoolean(String lexicalXSDBoolean);


  /**
   * Converts the string argument into a byte value.
   */
  abstract byte parseByte(String lexicalXSDByte);


  /**
   * Converts the string argument into a Calendar value.
   */
  abstract Calendar parseDate(String lexicalXSDDate);


  /**
   * Converts the string argument into a Calendar value.
   */
  abstract Calendar parseDateTime(String lexicalXSDDateTime);


  /**
   * Converts the string argument into a BigDecimal value.
   */
  abstract BigDecimal parseDecimal(String lexicalXSDDecimal);


  /**
   * Converts the string argument into a double value.
   */
  abstract double parseDouble(String lexicalXSDDouble);


  /**
   * Converts the string argument into a float value.
   */
  abstract float parseFloat(String lexicalXSDFloat);


  /**
   * Converts the string argument into an array of bytes.
   */
  abstract byte[] parseHexBinary(String lexicalXSDHexBinary);


  /**
   * Convert the string argument into an int value.
   */
  abstract int parseInt(String lexicalXSDInt);


  /**
   * Convert the string argument into a BigInteger value.
   */
  abstract BigInteger parseInteger(String lexicalXSDInteger);


  /**
   * Converts the string argument into a long value.
   */
  abstract long parseLong(String lexicalXSDLong);


  /**
   * Converts the string argument into a QName value. String parameter
   * lexicalXSDQname must conform to lexical value space specifed at XML Schema
   * Part 2:Datatypes specification:QNames
   */
  abstract QName parseQName(String lexicalXSDQName, NamespaceContext nsc);


  /**
   * Converts the string argument into a short value.
   */
  abstract short parseShort(String lexicalXSDShort);


  /**
   * Convert the string argument into a string.
   */
  abstract String parseString(String lexicalXSDString);


  /**
   * Converts the string argument into a Calendar value.
   */
  abstract Calendar parseTime(String lexicalXSDTime);


  /**
   * Converts the string argument into a long value.
   */
  abstract long parseUnsignedInt(String lexicalXSDUnsignedInt);


  /**
   * Converts the string argument into an int value.
   */
  abstract int parseUnsignedShort(String lexicalXSDUnsignedShort);


  /**
   * Converts a string value into a string.
   */
  abstract String printAnySimpleType(String val);


  /**
   * Converts an array of bytes into a string.
   */
  abstract String printBase64Binary(byte[] val);


  /**
   * Converts a boolean value into a string.
   */
  abstract String printBoolean(boolean val);


  /**
   * Converts a byte value into a string.
   */
  abstract String printByte(byte val);


  /**
   * Converts a Calendar value into a string.
   */
  abstract String printDate(Calendar val);


  /**
   * Converts a Calendar value into a string.
   */
  abstract String printDateTime(Calendar val);


  /**
   * Converts a BigDecimal value into a string.
   */
  abstract String printDecimal(BigDecimal val);


  /**
   * Converts a double value into a string.
   */
  abstract String printDouble(double val);


  /**
   * Converts a float value into a string.
   */
  abstract String printFloat(float val);


  /**
   * Converts an array of bytes into a string.
   */
  abstract String printHexBinary(byte[] val);


  /**
   * Converts an int value into a string.
   */
  abstract String printInt(int val);


  /**
   * Converts a BigInteger value into a string.
   */
  abstract String printInteger(BigInteger val);


  /**
   * Converts a long value into a string.
   */
  abstract String printLong(long val);


  /**
   * Converts a QName instance into a string.
   */
  abstract String printQName(QName val, NamespaceContext nsc);


  /**
   * Converts a short value into a string.
   */
  abstract String printShort(short val);


  /**
   * Converts the string argument into a string.
   */
  abstract String printString(String val);


  /**
   * Converts a Calendar value into a string.
   */
  abstract String printTime(Calendar val);


  /**
   * Converts a long value into a string.
   */
  abstract String printUnsignedInt(long val);


  /**
   * Converts an int value into a string.
   */
  abstract String printUnsignedShort(int val);

}

