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
 * Annotate a JavaBean multi-valued property to support mixed content. The
 * usage is subject to the following constraints: can be used with
 * XmlElementRef, XmlElementRefs or XmlAnyElement The following can be inserted
 * into XmlMixed annotated multi-valued property XML text information items are
 * added as values of java.lang.String. Children element information items are
 * added as instances of JAXBElement or instances with a class that is
 * annotated with XmlRootElement. Unknown content that is not be bound to a
 * JAXB mapped class is inserted as Element. (Assumes property annotated with
 * XmlAnyElement) Below is an example of binding and creation of mixed content.
 * <!-- schema fragment having mixed content --> <xs:complexType
 * name="letterBody" mixed="true"> <xs:sequence> <xs:element name="name"
 * type="xs:string"/> <xs:element name="quantity" type="xs:positiveInteger"/>
 * <xs:element name="productName" type="xs:string"/> <!-- etc. -->
 * </xs:sequence> </xs:complexType> <xs:element name="letterBody"
 * type="letterBody"/> // Schema-derived Java code: // (Only annotations
 * relevant to mixed content are shown below, // others are ommitted.) import
 * java.math.BigInteger; public class ObjectFactory { // element instance
 * factories JAXBElement<LetterBody> createLetterBody(LetterBody value);
 * JAXBElement<String> createLetterBodyName(String value);
 * JAXBElement<BigInteger> createLetterBodyQuantity(BigInteger value);
 * JAXBElement<String> createLetterBodyProductName(String value); // type
 * instance factory LetterBody> createLetterBody(); } public class LetterBody {
 * // Mixed content can contain instances of Element classes // Name, Quantity
 * and ProductName. Text data is represented as // java.util.String for text.
 * XmlMixed XmlElementRefs({ XmlElementRef(name="productName",
 * type=JAXBElement.class), XmlElementRef(name="quantity",
 * type=JAXBElement.class), XmlElementRef(name="name",
 * type=JAXBElement.class)}) List getContent(){...} } The following is an XML
 * instance document with mixed content <letterBody> Dear Mr.<name>Robert
 * Smith</name> Your order of <quantity>1</quantity> <productName>Baby
 * Monitor</productName> shipped from our warehouse. .... </letterBody> that
 * can be constructed using following JAXB API calls. LetterBody lb =
 * ObjectFactory.createLetterBody(); JAXBElement<LetterBody> lbe =
 * ObjectFactory.createLetterBody(lb); List gcl = lb.getContent(); //add mixed
 * content to general content property. gcl.add("Dear Mr."); // add text
 * information item as a String. // add child element information item
 * gcl.add(ObjectFactory.createLetterBodyName("Robert Smith")); gcl.add("Your
 * order of "); // add text information item as a String // add children
 * element information items gcl.add(ObjectFactory.
 * createLetterBodyQuantity(new BigInteger("1")));
 * gcl.add(ObjectFactory.createLetterBodyProductName("Baby Monitor"));
 * gcl.add("shipped from our warehouse"); // add text information item See
 * "Package Specification" in javax.xml.bind.package javadoc for additional
 * common information. Since: JAXB2.0 Author: Kohsuke Kawaguchi
 */
public interface XmlMixed {
}

