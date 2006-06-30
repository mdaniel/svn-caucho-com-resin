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
 * Maps a JavaBean property to XML infoset representation and/or JAXB element.
 * This annotation serves as a "catch-all" property while unmarshalling xml
 * content into a instance of a JAXB annotated class. It typically annotates a
 * multi-valued JavaBean property, but it can occur on single value JavaBean
 * property. During unmarshalling, each xml element that does not match a
 * static XmlElement or XmlElementRef annotation for the other JavaBean
 * properties on the class, is added to this "catch-all" property. This
 * annotation is mutually exclusive with XmlElement, XmlAttribute, XmlValue,
 * XmlElements, XmlID, and XmlIDREF. There can be only one XmlAnyElement
 * annotated JavaBean property in a class and its super classes. This
 * annotation can be used with XmlJavaTypeAdapter, so that users can map their
 * own data structure to DOM, which in turn can be composed into XML. This
 * annotation can be used with XmlMixed like this: // List of java.lang.String
 * or DOM nodes. XmlAnyElement XmlMixed ListObject> others; The XmlAnyElement
 * annotation can be used with XmlElementRefs to designate additional elements
 * that can participate in the content tree. The following schema would produce
 * the following Java class: <xs:complexType name="foo"> <xs:choice
 * maxOccurs="unbounded" minOccurs="0"> <xs:element name="a" type="xs:int" />
 * <xs:element name="b" type="xs:int" /> <xs:any namespace="##other"
 * processContents="lax" /> </xs:choice> </xs:complexType> class Foo {
 * XmlAnyElement(lax="true") XmlElementRefs({ XmlElementRef(name="a",
 * type="JAXBElement.class") XmlElementRef(name="b", type="JAXBElement.class")
 * }) ListObject> others; } XmlRegistry class ObjectFactory { ...
 * XmlElementDecl(name = "a", namespace = "", scope = Foo.class)
 * JAXBElementInteger> createFooA( Integer i ) { ... } XmlElementDecl(name =
 * "b", namespace = "", scope = Foo.class) JAXBElementInteger> createFooB(
 * Integer i ) { ... } It can unmarshal instances like <foo xmlns:e="extra">
 * <a>1</a> // this will unmarshal to a <A
 * HREF="../../../../javax/xml/bind/JAXBElement.html" title="class in
 * javax.xml.bind"><CODE>JAXBElement</CODE></A> instance whose value is 1.
 * <e:other /> // this will unmarshal to a DOM <A
 * HREF="http://java.sun.com/j2se/1.5/docs/api/org/w3c/dom/Element.html"
 * title="class or interface in org.w3c.dom"><CODE>Element</CODE></A>. <b>3</b>
 * // this will unmarshal to a <A
 * HREF="../../../../javax/xml/bind/JAXBElement.html" title="class in
 * javax.xml.bind"><CODE>JAXBElement</CODE></A> instance whose value is 1.
 * </foo> Since: JAXB2.0 Author: Kohsuke Kawaguchi
 */
public interface XmlAnyElement {
}

