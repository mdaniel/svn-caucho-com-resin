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
 * Generates a wrapper element around XML representation. This is primarily
 * intended to be used to produce a wrapper XML element around collections. The
 * annotation therefore supports two forms of serialization shown below.
 * //Example: code fragment int[] names; // XML Serialization Form 1 (Unwrapped
 * collection) names> ... /names> names> ... /names> // XML Serialization Form
 * 2 ( Wrapped collection ) wrapperElement> names> value-of-item /names> names>
 * value-of-item /names> .... /wrapperElement> The two serialized XML forms
 * allow a null collection to be represented either by absence or presence of
 * an element with a nillable attribute. Usage The @XmlElementWrapper
 * annotation can be used with the following program elements: JavaBean
 * property non static, non transient field The usage is subject to the
 * following constraints: The property must be a collection property This
 * annotation can be used with the following annotations: XmlElement,
 * XmlElements, XmlElementRef, XmlElementRefs, XmlJavaTypeAdapter. See "Package
 * Specification" in javax.xml.bind.package javadoc for additional common
 * information. Since: JAXB2.0 Author: Kohsuke Kawaguchi, Sun Microsystems,
 * Inc.Sekhar Vajjhala, Sun Microsystems, Inc. See Also:XmlElement,
 * XmlElements, XmlElementRef, XmlElementRefs
 */
public interface XmlElementWrapper {
}

