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

package javax.xml.bind.attachment;
import javax.activation.*;

/**
 * Enable JAXB marshalling to optimize storage of binary data. This API enables
 * an efficient cooperative creation of optimized binary data formats between a
 * JAXB marshalling process and a MIME-based package processor. A JAXB
 * implementation marshals the root body of a MIME-based package, delegating
 * the creation of referenceable MIME parts to the MIME-based package processor
 * that implements this abstraction. XOP processing is enabled when
 * isXOPPackage() is true. See addMtomAttachment(DataHandler, String, String)
 * for details. WS-I Attachment Profile 1.0 is supported by
 * addSwaRefAttachment(DataHandler) being called by the marshaller for each
 * JAXB property related to {http://ws-i.org/profiles/basic/1.1/xsd}swaRef.
 * Since: JAXB 2.0 Author: Marc Hadley, Kohsuke Kawaguchi, Joseph Fialli See
 * Also:Marshaller.setAttachmentMarshaller(AttachmentMarshaller), XML-binary
 * Optimized Packaging, WS-I Attachments Profile Version 1.0.
 */
public abstract class AttachmentMarshaller {
  public AttachmentMarshaller()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Consider binary data for optimized binary storage as an attachment. Since
   * content type is not known, the attachment's MIME content type must be set
   * to "application/octet-stream". The elementNamespace and elementLocalName
   * parameters provide the context that contains the binary data. This
   * information could be used by the MIME-based package processor to determine
   * if the binary data should be inlined or optimized as an attachment.
   */
  public abstract String addMtomAttachment(byte[] data, int offset, int length, String mimeType, String elementNamespace, String elementLocalName);


  /**
   * Consider MIME content data for optimized binary storage as an attachment.
   * This method is called by JAXB marshal process when isXOPPackage() is true,
   * for each element whose datatype is "base64Binary", as described in Step 3
   * in Creating XOP Packages. The method implementor determines whether data
   * shall be attached separately or inlined as base64Binary data. If the
   * implementation chooses to optimize the storage of the binary data as a
   * MIME part, it is responsible for attaching data to the MIME-based package,
   * and then assigning an unique content-id, cid, that identifies the MIME
   * part within the MIME message. This method returns the cid, which enables
   * the JAXB marshaller to marshal a XOP element that refers to that cid in
   * place of marshalling the binary data. When the method returns null, the
   * JAXB marshaller inlines data as base64binary data. The caller of this
   * method is required to meet the following constraint. If the element
   * infoset item containing data has the attribute xmime:contentType or if the
   * JAXB property/field representing datais annotated with a known MIME type,
   * data.getContentType() should be set to that MIME type. The
   * elementNamespace and elementLocalName parameters provide the context that
   * contains the binary data. This information could be used by the MIME-based
   * package processor to determine if the binary data should be inlined or
   * optimized as an attachment.
   */
  public abstract String addMtomAttachment(DataHandler data, String elementNamespace, String elementLocalName);


  /**
   * Add MIME data as an attachment and return attachment's content-id, cid.
   * This method is called by JAXB marshal process for each element/attribute
   * typed as {http://ws-i.org/profiles/basic/1.1/xsd}swaRef. The MIME-based
   * package processor implementing this method is responsible for attaching
   * the specified data to a MIME attachment, and generating a content-id, cid,
   * that uniquely identifies the attachment within the MIME-based package.
   * Caller inserts the returned content-id, cid, into the XML content being
   * marshalled.
   */
  public abstract String addSwaRefAttachment(DataHandler data);


  /**
   * Read-only property that returns true if JAXB marshaller should enable XOP
   * creation. This value must not change during the marshalling process. When
   * this value is true, the addMtomAttachment(...) method is invoked when the
   * appropriate binary datatypes are encountered by the marshal process.
   * Marshaller.marshal() must throw IllegalStateException if this value is
   * true and the XML content to be marshalled violates Step 1 in Creating XOP
   * Pacakges
   * http://www.w3.org/TR/2005/REC-xop10-20050125/#creating_xop_packages.
   * "Ensure the Original XML Infoset contains no element information item with
   * a [namespace name] of "http://www.w3.org/2004/08/xop/include" and a [local
   * name] of Include" When this method returns true and during the marshal
   * process at least one call to addMtomAttachment(...) returns a content-id,
   * the MIME-based package processor must label the root part with the
   * application/xop+xml media type as described in Step 5 of Creating XOP
   * Pacakges.
   */
  public boolean isXOPPackage()
  {
    throw new UnsupportedOperationException();
  }

}

