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
 * Maps a JavaBean property to a map of wildcard attributes. Usage The
 * XmlAnyAttribute annotation can be used with the following program elements:
 * JavaBean property non static, non transient field See "Package
 * Specification" in javax.xml.bind.package javadoc for additional common
 * information. While processing attributes to be unmarshalled into a value
 * class, each attribute that is not statically associated with another
 * JavaBean property, via XmlAttribute, is entered into the wildcard attribute
 * map represented by MapQName,Object>. The attribute QName is the map's key.
 * The key's value is the String value of the attribute. Since: JAXB2.0 Author:
 * Kohsuke Kawaguchi, Sun Microsystems, Inc.
 */
public interface XmlAnyAttribute {
}

