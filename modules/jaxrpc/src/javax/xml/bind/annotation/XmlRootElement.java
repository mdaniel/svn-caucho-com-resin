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
 * Maps a class or an enum type to an XML element. Usage The XmlRootElement
 * annotation can be used with the following program elements: a top level
 * class an enum type See "Package Specification" in javax.xml.bind.package
 * javadoc for additional common information. When a top level class or an enum
 * type is annotated with the XmlRootElement annotation, then its value is
 * represented as XML element in an XML document. This annotation can be used
 * with the following annotations: XmlType, XmlEnum, XmlAccessorType,
 * XmlAccessorOrder. Example 1: Associate an element with XML Schema type //
 * Example: Code fragment XmlRootElement class Point { int x; int y; Point(int
 * _x,int _y) {x=_x;y=_y;} } //Example: Code fragment corresponding to XML
 * output marshal( new Point(3,5), System.out); <!-- Example: XML output -->
 * <point> <x> 3 </x> <y> 5 </y> </point> The annotation causes an global
 * element declaration to be produced in the schema. The global element
 * declaration is associated with the XML schema type to which the class is
 * mapped. <!-- Example: XML schema definition --> <xs:element name="point"
 * type="point"/> <xs:complexType name="point"> <xs:sequence> <xs:element
 * name="x" type="xs:int"/> <xs:element name="y" type="xs:int"/> </xs:sequence>
 * </xs:complexType> Example 2: Orthogonality to type inheritance An element
 * declaration annotated on a type is not inherited by its derived types. The
 * following example shows this. // Example: Code fragment XmlRootElement class
 * Point3D extends Point { int z; Point3D(int _x,int _y,int _z)
 * {super(_x,_y);z=_z;} } //Example: Code fragment corresponding to XML output
 * * marshal( new Point3D(3,5,0), System.out ); !-- Example: XML output --> !--
 * The element name is point3D not point --> point3D> x>3/x> y>5/y> z>0/z>
 * /point3D> !-- Example: XML schema definition --> xs:element name="point3D"
 * type="point3D"/> xs:complexType name="point3D"> xs:complexContent>
 * xs:extension base="point"> xs:sequence> xs:element name="z" type="xs:int"/>
 * /xs:sequence> /xs:extension> /xs:complexContent> /xs:complexType> Example 3:
 * Associate a global element with XML Schema type to which the class is
 * mapped. //Example: Code fragment XmlRootElement(name="PriceElement") public
 * class USPrice { XmlElement public java.math.BigDecimal price; } !-- Example:
 * XML schema definition --> xs:element name="PriceElement" type="USPrice"/>
 * xs:complexType name="USPrice"> xs:sequence> xs:element name="price"
 * type="xs:decimal"/> /sequence> /xs:complexType> Since: JAXB2.0 Author:
 * Sekhar Vajjhala, Sun Microsystems, Inc.
 */
public interface XmlRootElement {
}

