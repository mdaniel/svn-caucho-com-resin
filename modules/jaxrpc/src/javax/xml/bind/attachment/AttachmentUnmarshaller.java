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
 * Enables JAXB unmarshalling of a root document containing optimized binary
 * data formats. This API enables an efficient cooperative processing of
 * optimized binary data formats between a JAXB 2.0 implementation and
 * MIME-based package processor (MTOM/XOP and WS-I AP 1.0). JAXB unmarshals the
 * body of a package, delegating the understanding of the packaging format
 * being used to a MIME-based package processor that implements this abstract
 * class. This abstract class identifies if a package requires XOP processing,
 * isXOPPackage() and provides retrieval of binary content stored as
 * attachments by content-id. Since: JAXB 2.0 Author: Marc Hadley, Kohsuke
 * Kawaguchi, Joseph Fialli See
 * Also:Unmarshaller.setAttachmentUnmarshaller(AttachmentUnmarshaller),
 * XML-binary Optimized Packaging, WS-I Attachments Profile Version 1.0.,
 * Describing Media Content of Binary Data in XML
 */
public abstract class AttachmentUnmarshaller {
  public AttachmentUnmarshaller()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Retrieve the attachment identified by content-id, cid, as a byte[] .
   */
  public abstract byte[] getAttachmentAsByteArray(String cid);


  /**
   * Lookup MIME content by content-id, cid, and return as a DataHandler. The
   * returned DataHandler instance must be configured to meet the following
   * required mapping constaint. Required Mappings between MIME and Java Types
   * MIME Type Java Type DataHandler.getContentType() instanceof
   * DataHandler.getContent() image/gif java.awt.Image image/jpeg
   * java.awt.Image text/xml or application/xml javax.xml.transform.Source Note
   * that it is allowable to support additional mappings.
   */
  public abstract DataHandler getAttachmentAsDataHandler(String cid);


  /**
   * Read-only property that returns true if JAXB unmarshaller needs to perform
   * XOP processing. This method returns true when the constraints specified in
   * Identifying XOP Documents are met. This value must not change during the
   * unmarshalling process.
   */
  public boolean isXOPPackage()
  {
    throw new UnsupportedOperationException();
  }

}

