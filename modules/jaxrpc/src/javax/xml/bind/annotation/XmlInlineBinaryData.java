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
 * Disable consideration of XOP encoding for datatypes that are bound to
 * base64-encoded binary data in XML. When XOP encoding is enabled as described
 * in AttachmentMarshaller.isXOPPackage(), this annotation disables datatypes
 * such as Image or Source or byte[] that are bound to base64-encoded binary
 * from being considered for XOP encoding. If a JAXB property is annotated with
 * this annotation or if the JAXB property's base type is annotated with this
 * annotation, neither AttachmentMarshaller.addMtomAttachment(DataHandler,
 * String, String) nor AttachmentMarshaller.addMtomAttachment(byte[], int, int,
 * String, String, String) is ever called for the property. The binary data
 * will always be inlined. Since: JAXB2.0 Author: Joseph Fialli
 */
public interface XmlInlineBinaryData {
}

