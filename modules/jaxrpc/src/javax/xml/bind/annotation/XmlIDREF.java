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
 * Maps a JavaBean property to XML IDREF. To preserve referential integrity of
 * an object graph across XML serialization followed by a XML deserialization,
 * requires an object reference to be marshalled by reference or containment
 * appropriately. Annotations XmlID and XmlIDREF together allow a customized
 * mapping of a JavaBean property's type by containment or reference. Usage See
 * "Package Specification" in javax.xml.bind.package javadoc for additional
 * common information. The usage is subject to the following constraints: If
 * the type of the field or property is a collection type, then the collection
 * item type must contain a property or field annotated with XmlID. If the
 * field or property is single valued, then the type of the property or field
 * must contain a property or field annotated with XmlID. Note: If the
 * collection item type or the type of the property (for non collection type)
 * is java.lang.Object, then the instance must contain a property/field
 * annotated with XmlID attribute. This annotation can be used with the
 * following annotations: XmlElement, XmlAttribute, XmlList, and XmlElements.
 * Example: Map a JavaBean property to xs:IDREF (i.e. by reference rather than
 * by containment) Example 2: The following is a complete example of
 * containment versus reference. // By default, Customer maps to complex type
 * xs:Customer public class Customer { // map JavaBean property type to xs:ID
 * XmlID public String getCustomerID(); public void setCustomerID(String id);
 * // .... other properties not shown } // By default, Invoice maps to a
 * complex type xs:Invoice public class Invoice { // map by reference XmlIDREF
 * public Customer getCustomer(); public void setCustomer(Customer customer);
 * // .... other properties not shown here } // By default, Shipping maps to
 * complex type xs:Shipping public class Shipping { // map by reference
 * XmlIDREF public Customer getCustomer(); public void setCustomer(Customer
 * customer); } // at least one class must reference Customer by containment;
 * // Customer instances won't be marshalled. XmlElement(name="CustomerData")
 * public class CustomerData { // map reference to Customer by containment by
 * default. public Customer getCustomer(); // maps reference to Shipping by
 * containment by default. public Shipping getShipping(); // maps reference to
 * Invoice by containment by default. public Invoice getInvoice(); } !-- XML
 * Schema mapping for above code frament --> xs:complexType name="Invoice">
 * xs:complexContent> xs:sequence> xs:element name="customer" type="xs:IDREF"/>
 * .... /xs:sequence> /xs:complexContent> /xs:complexType> xs:complexType
 * name="Shipping"> xs:complexContent> xs:sequence> xs:element name="customer"
 * type="xs:IDREF"/> .... /xs:sequence> /xs:complexContent> /xs:complexType>
 * xs:complexType name="Customer"> xs:complexContent> xs:sequence> ....
 * /xs:sequence> xs:attribute name="CustomerID" type="xs:ID"/>
 * /xs:complexContent> /xs:complexType> xs:complexType name="CustomerData">
 * xs:complexContent> xs:sequence> xs:element name="customer"
 * type="xs:Customer"/> xs:element name="shipping" type="xs:Shipping"/>
 * xs:element name="invoice" type="xs:Invoice"/> /xs:sequence>
 * /xs:complexContent> /xs:complexType> xs:element name"customerData"
 * type="xs:CustomerData"/> !-- Instance document conforming to the above XML
 * Schema --> customerData> customer customerID="Alice"> .... /customer>
 * shipping customer="Alice"> .... /shipping> invoice customer="Alice"> ....
 * /invoice> /customerData> Example 3: Mapping List to repeating element of
 * type IDREF // Code fragment public class Shipping { XmlIDREF
 * XmlElement(name="Alice") public List customers; } !-- XML schema fragment
 * --> xs:complexType name="Shipping"> xs:sequence> xs:choice minOccurs="0"
 * maxOccurs="unbounded"> xs:element name="Alice" type="xs:IDREF"/> /xs:choice>
 * /xs:sequence> /xs:complexType> Example 4: Mapping a List to a list of
 * elements of type IDREF. //Code fragment public class Shipping { XmlIDREF
 * XmlElements( XmlElement(name="Alice", type="Customer.class")
 * XmlElement(name="John", type="InternationalCustomer.class") public List
 * customers; } !-- XML Schema fragment --> xs:complexType name="Shipping">
 * xs:sequence> xs:choice minOccurs="0" maxOccurs="unbounded"> xs:element
 * name="Alice" type="xs:IDREF"/> xs:element name="John" type="xs:IDREF"/>
 * /xs:choice> /xs:sequence> /xs:complexType> Since: JAXB2.0 Version:
 * $Revision: 1.12 $ Author: Sekhar Vajjhala, Sun Microsystems, Inc. See
 * Also:XmlID
 */
public interface XmlIDREF {
}

