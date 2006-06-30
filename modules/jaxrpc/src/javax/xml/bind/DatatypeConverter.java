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
 * The javaType binding declaration can be used to customize the binding of an
 * XML schema datatype to a Java datatype. Customizations can involve writing a
 * parse and print method for parsing and printing lexical representations of a
 * XML schema datatype respectively. However, writing parse and print methods
 * requires knowledge of the lexical representations ( XML Schema Part2:
 * Datatypes specification ) and hence may be difficult to write. This class
 * makes it easier to write parse and print methods. It defines static parse
 * and print methods that provide access to a JAXB provider's implementation of
 * parse and print methods. These methods are invoked by custom parse and print
 * methods. For example, the binding of xsd:dateTime to a long can be
 * customized using parse and print methods as follows: There is a static parse
 * and print method corresponding to each parse and print method respectively
 * in the DatatypeConverterInterface. The static methods defined in the class
 * can also be used to specify a parse or a print method in a javaType binding
 * declaration. JAXB Providers are required to call the setDatatypeConverter
 * api at some point before the first marshal or unmarshal operation (perhaps
 * during the call to JAXBContext.newInstance). This step is necessary to
 * configure the converter that should be used to perform the print and parse
 * functionality. A print method for a XML schema datatype can output any
 * lexical representation that is valid with respect to the XML schema
 * datatype. If an error is encountered during conversion, then an
 * IllegalArgumentException, or a subclass of IllegalArgumentException must be
 * thrown by the method. Since: JAXB1.0 Version: $Revision: 1.3 $ Author:
 * Sekhar Vajjhala, Sun Microsystems, Inc.Joe Fialli, Sun Microsystems
 * Inc.Kohsuke Kawaguchi, Sun Microsystems, Inc.Ryan Shoemaker,Sun Microsystems
 * Inc. See Also:DatatypeConverterInterface, ParseConversionEvent,
 * PrintConversionEvent
 */
public final class DatatypeConverter {

  /**
   * Return a string containing the lexical representation of the simple type.
   */
  public static String parseAnySimpleType(String lexicalXSDAnySimpleType)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into an array of bytes.
   */
  public static byte[] parseBase64Binary(String lexicalXSDBase64Binary)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a boolean value.
   */
  public static boolean parseBoolean(String lexicalXSDBoolean)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a byte value.
   */
  public static byte parseByte(String lexicalXSDByte)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a Calendar value.
   */
  public static Calendar parseDate(String lexicalXSDDate)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a Calendar value.
   */
  public static Calendar parseDateTime(String lexicalXSDDateTime)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a BigDecimal value.
   */
  public static BigDecimal parseDecimal(String lexicalXSDDecimal)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a double value.
   */
  public static double parseDouble(String lexicalXSDDouble)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a float value.
   */
  public static float parseFloat(String lexicalXSDFloat)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into an array of bytes.
   */
  public static byte[] parseHexBinary(String lexicalXSDHexBinary)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convert the string argument into an int value.
   */
  public static int parseInt(String lexicalXSDInt)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convert the string argument into a BigInteger value.
   */
  public static BigInteger parseInteger(String lexicalXSDInteger)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a long value.
   */
  public static long parseLong(String lexicalXSDLong)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a byte value. String parameter
   * lexicalXSDQname must conform to lexical value space specifed at XML Schema
   * Part 2:Datatypes specification:QNames
   */
  public static QName parseQName(String lexicalXSDQName, NamespaceContext nsc)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a short value.
   */
  public static short parseShort(String lexicalXSDShort)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Convert the lexical XSD string argument into a String value.
   */
  public static String parseString(String lexicalXSDString)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a Calendar value.
   */
  public static Calendar parseTime(String lexicalXSDTime)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a long value.
   */
  public static long parseUnsignedInt(String lexicalXSDUnsignedInt)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into an int value.
   */
  public static int parseUnsignedShort(String lexicalXSDUnsignedShort)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a string value into a string.
   */
  public static String printAnySimpleType(String val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts an array of bytes into a string.
   */
  public static String printBase64Binary(byte[] val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a boolean value into a string.
   */
  public static String printBoolean(boolean val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a byte value into a string.
   */
  public static String printByte(byte val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a Calendar value into a string.
   */
  public static String printDate(Calendar val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a Calendar value into a string.
   */
  public static String printDateTime(Calendar val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a BigDecimal value into a string.
   */
  public static String printDecimal(BigDecimal val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a double value into a string.
   */
  public static String printDouble(double val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a float value into a string.
   */
  public static String printFloat(float val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts an array of bytes into a string.
   */
  public static String printHexBinary(byte[] val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts an int value into a string.
   */
  public static String printInt(int val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a BigInteger value into a string.
   */
  public static String printInteger(BigInteger val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts A long value into a string.
   */
  public static String printLong(long val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a QName instance into a string.
   */
  public static String printQName(QName val, NamespaceContext nsc)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a short value into a string.
   */
  public static String printShort(short val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts the string argument into a string.
   */
  public static String printString(String val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a Calendar value into a string.
   */
  public static String printTime(Calendar val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts a long value into a string.
   */
  public static String printUnsignedInt(long val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Converts an int value into a string.
   */
  public static String printUnsignedShort(int val)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * This method is for JAXB provider use only. JAXB Providers are required to
   * call this method at some point before allowing any of the JAXB client
   * marshal or unmarshal operations to occur. This is necessary to configure
   * the datatype converter that should be used to perform the print and parse
   * conversions. Calling this api repeatedly will have no effect - the
   * DatatypeConverterInterface instance passed into the first invocation is
   * the one that will be used from then on.
   */
  public static void setDatatypeConverter(DatatypeConverterInterface converter)
  {
    throw new UnsupportedOperationException();
  }

}

