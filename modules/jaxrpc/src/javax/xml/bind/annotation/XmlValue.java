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
 * Enables mapping a class to a XML Schema complex type with a simpleContent or
 * a XML Schema simple type. Usage: The @XmlValue annotation can be used with
 * the following program elements: a JavaBean property. non static, non
 * transient field. See "Package Specification" in javax.xml.bind.package
 * javadoc for additional common information. If the annotated JavaBean
 * property is the sole class member being mapped to XML Schema construct, then
 * the class is mapped to a simple type. If there are additional JavaBean
 * properties (other than the JavaBean property annotated with @XmlValue
 * annotation) that are mapped to XML attributes, then the class is mapped to a
 * complex type with simpleContent. Example 1: Map a class to XML Schema
 * simpleType Example 2: Map a class to XML Schema complexType with with
 * simpleContent. Since: JAXB2.0 Version: $Revision: 1.6 $ Author: Sekhar
 * Vajjhala, Sun Microsystems, Inc. See Also:XmlType
 */
public interface XmlValue {
}

