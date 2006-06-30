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
 * Maps a JavaBean property to XML ID. To preserve referential integrity of an
 * object graph across XML serialization followed by a XML deserialization,
 * requires an object reference to be marshalled by reference or containment
 * appropriately. Annotations XmlID and XmlIDREF together allow a customized
 * mapping of a JavaBean property's type by containment or reference. Usage See
 * "Package Specification" in javax.xml.bind.package javadoc for additional
 * common information. Example: Map a JavaBean property's type to xs:ID Since:
 * JAXB2.0 Version: $Revision: 1.5 $ Author: Sekhar Vajjhala, Sun Microsystems,
 * Inc. See Also:XmlIDREF
 */
public interface XmlID {
}

