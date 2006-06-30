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
 * Maps a factory method to a XML element. Usage Example 1: Annotation on a
 * factory method // Example: code fragment XmlRegistry class ObjectFactory {
 * XmlElementDecl(name="foo") JAXBElementString> createFoo(String s) { ... } }
 * <!-- XML input --> <foo>string</foo> // Example: code fragment corresponding
 * to XML input JAXBElement<String> o =
 * (JAXBElement<String>)unmarshaller.unmarshal(aboveDocument); // print
 * JAXBElement instance to show values System.out.println(o.getName()); //
 * prints "{}foo" System.out.println(o.getValue()); // prints "string"
 * System.out.println(o.getValue().getClass()); // prints "java.lang.String"
 * <!-- Example: XML schema definition --> <xs:element name="foo"
 * type="xs:string"/> Example 2: Element declaration with non local scope The
 * following example illustrates the use of scope annotation parameter in
 * binding of element declaration in schema derived code. The following example
 * may be replaced in a future revision of this javadoc. <!-- Example: XML
 * schema definition --> <xs:schema> <xs:complexType name="pea"> <xs:choice
 * maxOccurs="unbounded"> <xs:element name="foo" type="xs:string"/> <xs:element
 * name="bar" type="xs:string"/> </xs:choice> </xs:complexType> <xs:element
 * name="foo" type="xs:int"/> </xs:schema> // Example: expected default binding
 * class Pea { XmlElementRefs({
 * XmlElementRef(name="foo",type=JAXBElement.class)
 * XmlElementRef(name="bar",type=JAXBElement.class) }) ListJAXBElementString>>
 * fooOrBar; } XmlRegistry class ObjectFactory {
 * XmlElementDecl(scope=Pea.class,name="foo") JAXBElement createPeaFoo(String
 * s); XmlElementDecl(scope=Pea.class,name="bar") JAXBElement
 * createPeaBar(String s); XmlElementDecl(name="foo") JAXBElement
 * createFoo(Integer i); } Without scope createFoo and createPeaFoo would
 * become ambiguous since both of them map to a XML schema element with the
 * same local name "foo". Since: JAXB 2.0 See Also:XmlRegistry
 */
public interface XmlElementDecl {

  /**
   * Used in XmlElementDecl.scope() to signal that the declaration is in the
   * global scope.
   */
  public static final class GLOBAL {
    public GLOBAL()
    {
      throw new UnsupportedOperationException();
    }

  }
}

