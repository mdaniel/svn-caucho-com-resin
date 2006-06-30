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

package javax.xml.bind.annotation;

/**
 * Maps a Java type to a simple schema built-in type. Usage @XmlSchemaType
 * annotation can be used with the following program elements: a JavaBean
 * property field package @XmlSchemaType annotation defined for Java type
 * applies to all references to the Java type from a property/field. A
 * @XmlSchemaType annotation specified on the property/field overrides the
 * @XmlSchemaType annotation specified at the package level. This annotation
 * can be used with the following annotations: XmlElement, XmlAttribute.
 * Example 1: Customize mapping of XMLGregorianCalendar on the field.
 * //Example: Code fragment public class USPrice { XmlElement
 * XmlSchemaType(name="date") public XMLGregorianCalendar date; } !-- Example:
 * Local XML Schema element --> xs:complexType name="USPrice"/> xs:sequence>
 * xs:element name="date" type="xs:date"/> /sequence> /xs:complexType> Example
 * 2: Customize mapping of XMLGregorianCalendar at package level Since: JAXB2.0
 */
public interface XmlSchemaType {

  /**
   * Used in XmlSchemaType.type() to signal that the type be inferred from the
   * signature of the property.
   */
  public static final class DEFAULT {
    public DEFAULT()
    {
      throw new UnsupportedOperationException();
    }

  }
}

