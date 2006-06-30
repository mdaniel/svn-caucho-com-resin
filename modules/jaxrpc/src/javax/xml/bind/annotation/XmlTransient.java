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
 * Prevents the mapping of a JavaBean property to XML representation. The
 * @XmlTransient annotation is useful for resolving name collisions between a
 * JavaBean property name and a field name or preventing the mapping of a
 * field/property. A name collision can occur when the decapitalized JavaBean
 * property name and a field name are the same. If the JavaBean property refers
 * to the field, then the name collision can be resolved by preventing the
 * mapping of either the field or the JavaBean property using the @XmlTransient
 * annotation. Usage The @XmlTransient annotation can be used with the
 * following program elements: a JavaBean property field @XmlTransientis
 * mutually exclusive with all other JAXB defined annotations. See "Package
 * Specification" in javax.xml.bind.package javadoc for additional common
 * information. Example: Resolve name collision between JavaBean property and
 * field name Since: JAXB2.0 Version: $Revision: 1.8 $ Author: Sekhar Vajjhala,
 * Sun Microsystems, Inc.
 */
public interface XmlTransient {
}

