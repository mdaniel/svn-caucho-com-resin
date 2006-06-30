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
 * A container for multiple @XmlElement annotations. Multiple annotations of
 * the same type are not allowed on a program element. This annotation
 * therefore serves as a container annotation for multiple XmlElements as
 * follows: XmlElements({ @XmlElement(...),@XmlElement(...) }) The @XmlElements
 * annnotation can be used with the following program elements: Usage The usage
 * is subject to the following constraints: This annotation can be used with
 * the following annotations: @XmlIDREF, @XmlElementWrapper. If @XmlIDREF is
 * also specified on the JavaBean property, then each XmlElement.type() must
 * contain a JavaBean property annotated with XmlID. See "Package
 * Specification" in javax.xml.bind.package javadoc for additional common
 * information.
 */
public interface XmlElements {
}

