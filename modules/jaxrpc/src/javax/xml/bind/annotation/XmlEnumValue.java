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
 * Maps an enum constant in Enum type to XML representation. Usage The
 * @XmlEnumValue annotation can be used with the following program elements:
 * enum constant See "Package Specification" in javax.xml.bind.package javadoc
 * for additional common information. This annotation, together with XmlEnum
 * provides a mapping of enum type to XML representation. An enum type is
 * mapped to a schema simple type with enumeration facets. The schema type is
 * derived from the Java type specified in @XmlEnum.value(). Each enum constant
 * @XmlEnumValue must have a valid lexical representation for the type
 * @XmlEnum.value() In the absence of this annotation, Enum.name() is used as
 * the XML representation. Example 1: Map enum constant name -> enumeration
 * facet Example 2: Map enum constant name(value) -> enumeration facet Example
 * 3: Map enum constant name -> enumeration facet Since: JAXB 2.0
 */
public interface XmlEnumValue {
}

