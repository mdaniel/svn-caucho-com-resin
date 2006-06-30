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
 * Maps a JavaBean property to a XML element derived from property name. Usage
 * @XmlElement annotation can be used with the following program elements: a
 * JavaBean property non static, non transient field within XmlElements The
 * usage is subject to the following constraints: This annotation can be used
 * with following annotations: XmlID, XmlIDREF, XmlList, XmlSchemaType,
 * XmlValue, XmlAttachmentRef, XmlMimeType, XmlInlineBinaryData,
 * XmlElementWrapper, XmlJavaTypeAdapter if the type of JavaBean property is a
 * collection type of array, an indexed property, or a parameterized list, and
 * this annotation is used with XmlElements then, @XmlElement.type() must be
 * DEFAULT.class since the collection item type is already known. A JavaBean
 * property, when annotated with @XmlElement annotation is mapped to a local
 * element in the XML Schema complex type to which the containing class is
 * mapped. Example 1: Map a public non static non final field to local element
 * //Example: Code fragment public class USPrice { XmlElement(name="itemprice")
 * public java.math.BigDecimal price; } !-- Example: Local XML Schema element
 * --> xs:complexType name="USPrice"/> xs:sequence> xs:element name="itemprice"
 * type="xs:decimal" minOccurs="0"/> /sequence> /xs:complexType> Example 2: Map
 * a field to a nillable element. //Example: Code fragment public class USPrice
 * { XmlElement(nillable=true) public java.math.BigDecimal price; } !--
 * Example: Local XML Schema element --> xs:complexType name="USPrice">
 * xs:sequence> xs:element name="price" type="xs:decimal" nillable="true"
 * minOccurs="0"/> /sequence> /xs:complexType> Example 3: Map a field to a
 * nillable, required element. //Example: Code fragment public class USPrice {
 * XmlElement(nillable=true, required=true) public java.math.BigDecimal price;
 * } !-- Example: Local XML Schema element --> xs:complexType name="USPrice">
 * xs:sequence> xs:element name="price" type="xs:decimal" nillable="true"
 * minOccurs="1"/> /sequence> /xs:complexType> Example 4: Map a JavaBean
 * property to an XML element with anonymous type. See Example 6 in @XmlType.
 * Since: JAXB2.0 Version: $Revision: 1.19 $ Author: Sekhar Vajjhala, Sun
 * Microsystems, Inc.
 */
public interface XmlElement {

  /**
   * Used in XmlElement.type() to signal that the type be inferred from the
   * signature of the property.
   */
  public static final class DEFAULT {
    public DEFAULT()
    {
      throw new UnsupportedOperationException();
    }

  }
}

