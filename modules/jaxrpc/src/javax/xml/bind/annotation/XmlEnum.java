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
 * Maps an enum type Enum to XML representation. This annotation, together with
 * XmlEnumValue provides a mapping of enum type to XML representation. Usage
 * The @XmlEnum annotation can be used with the following program elements:
 * enum type The usage is subject to the following constraints: This annotation
 * can be used the following other annotations: XmlType, XmlRootElement See
 * "Package Specification" in javax.xml.bind.package javadoc for additional
 * common information An enum type is mapped to a schema simple type with
 * enumeration facets. The schema type is derived from the Java type to which
 * @XmlEnum.value(). Each enum constant @XmlEnumValue must have a valid lexical
 * representation for the type @XmlEnum.value() . Examples: See examples in
 * XmlEnumValue Since: JAXB2.0
 */
public interface XmlEnum {
}

