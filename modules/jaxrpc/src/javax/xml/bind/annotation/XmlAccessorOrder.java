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
 * Controls the ordering of fields and properties in a class. Usage
 * @XmlAccessorOrder annotation can be used with the following program
 * elements: See "Package Specification" in javax.xml.bind.package javadoc for
 * additional common information. The annotation @XmlAccessorOrder on a package
 * applies to all classes in a package. The following inheritance semantics
 * apply: If there is a @XmlAccessorOrder on a class, then it is used.
 * Otherwise, if a @XmlAccessorOrder exists on one of its super classes, then
 * it is inherited. Otherwise, the @XmlAccessorOrder on a package is inherited.
 * Defaulting Rules: By default, if @XmlAccessorOrder on a package is absent,
 * then the following package level annotation is assumed. By default, if
 * @XmlAccessorOrder on a class is absent and none of super classes is
 * annotated with XmlAccessorOrder , then the following default on the class is
 * assumed: This annotation can be used with the following annotations:
 * XmlType, XmlRootElement, XmlAccessorType, XmlSchema, XmlSchemaType,
 * XmlSchemaTypes, , XmlJavaTypeAdapter. It can also be used with the following
 * annotations at the package level: XmlJavaTypeAdapter. Since: JAXB2.0
 * Version: $Revision: 1.11 $ Author: Sekhar Vajjhala, Sun Microsystems, Inc.
 * See Also:XmlAccessOrder
 */
public interface XmlAccessorOrder {
}

