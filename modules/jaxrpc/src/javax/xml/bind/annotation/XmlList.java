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
 * Used to map a property to a list simple type. Usage The @XmlList annotation
 * can be used with the following program elements: JavaBean property field
 * When a collection property is annotated just with @XmlElement, each item in
 * the collection will be wrapped by an element. For example, XmlRootElement
 * class Foo { XmlElement ListString> data; } would produce XML like this:
 * <foo> <data>abc</data> <data>def</data> </foo> XmlList annotation, on the
 * other hand, allows multiple values to be represented as whitespace-separated
 * tokens in a single element. For example, XmlRootElement class Foo {
 * XmlElement XmlList ListString> data; } the above code will produce XML like
 * this: <foo> <data>abc def</data> </foo> This annotation can be used with the
 * following annotations: XmlElement, XmlAttribute, XmlValue, XmlIDREF. The use
 * of @XmlList with XmlValue while allowed, is redundant since XmlList maps a
 * collection type to a simple schema type that derives by list just as
 * XmlValue would. The use of @XmlList with XmlAttribute while allowed, is
 * redundant since XmlList maps a collection type to a simple schema type that
 * derives by list just as XmlAttribute would. Since: JAXB2.0 Author: Kohsuke
 * Kawaguchi, Sun Microsystems, Inc.Sekhar Vajjhala, Sun Microsystems, Inc.
 */
public interface XmlList {
}

