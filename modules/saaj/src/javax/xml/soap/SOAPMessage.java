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

package javax.xml.soap;
import java.io.*;
import javax.activation.*;
import java.util.*;

/**
 * The root class for all SOAP messages.
 */
public abstract class SOAPMessage {

  /**
   * Specifies the character type encoding for the SOAP Message.
   */
  public static final String CHARACTER_SET_ENCODING =
      "javax.xml.soap.character-set-encoding";


  /**
   * Specifies whether the SOAP Message will contain an XML declaration when it
   * is sent.
   */
  public static final String WRITE_XML_DECLARATION =
      "javax.xml.soap.write-xml-declaration";

  public SOAPMessage()
  {
  }


  /**
   * Adds the given AttachmentPart object to this SOAPMessage object.
   */
  public abstract void addAttachmentPart(AttachmentPart attachmentPart);


  /**
   * Gets a count of the number of attachments in this message.
   */
  public abstract int countAttachments();


  /**
   * Creates a new empty AttachmentPart object.
   */
  public abstract AttachmentPart createAttachmentPart();


  /**
   * Creates an AttachmentPart object and populates it using the given
   * DataHandler object.
   */
  public AttachmentPart createAttachmentPart(DataHandler dataHandler)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates an AttachmentPart object and populates it with the specified data
   * of the specified content type.
   */
  public AttachmentPart createAttachmentPart(Object content, String contentType)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns an AttachmentPart object that is associated with an attachment
   * that is referenced by this SOAPElement or null if no such attachment
   * exists.
   */
  public abstract AttachmentPart getAttachment(SOAPElement element)
      throws SOAPException;


  /**
   * Retrieves all the AttachmentPart objects that are part of this SOAPMessage
   * object.
   */
  public abstract Iterator getAttachments();


  /**
   * Retrieves all the AttachmentPart objects that have header entries that
   * match the specified headers. Note that a returned attachment could have
   * headers in addition to those specified.
   */
  public abstract Iterator getAttachments(MimeHeaders headers);


  /**
   * Retrieves a description of this SOAPMessage object's content.
   */
  public abstract String getContentDescription();


  /**
   * Returns all the transport-specific MIME headers for this SOAPMessage
   * object in a transport-independent fashion.
   */
  public abstract MimeHeaders getMimeHeaders();


  /**
   * Retrieves value of the specified property.
   */
  public Object getProperty(String property) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the SOAP Body contained in this SOAPMessage object.
   */
  public SOAPBody getSOAPBody() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the SOAP Header contained in this SOAPMessage object.
   */
  public SOAPHeader getSOAPHeader() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the SOAP part of this SOAPMessage object.
   */
  public abstract SOAPPart getSOAPPart();


  /**
   * Removes all AttachmentPart objects that have been added to this
   * SOAPMessage object. This method does not touch the SOAP part.
   */
  public abstract void removeAllAttachments();


  /**
   * Removes all the AttachmentPart objects that have header entries that match
   * the specified headers.
   */
  public abstract void removeAttachments(MimeHeaders headers);


  /**
   * Updates this SOAPMessage object with all the changes that have been made
   * to it.
   */
  public abstract void saveChanges() throws SOAPException;


  /**
   * Indicates whether this SOAPMessage object needs to have the method
   * saveChanges called on it.
   */
  public abstract boolean saveRequired();


  /**
   * Sets the description of this SOAPMessage object's content with the given
   * description.
   */
  public abstract void setContentDescription(String description);


  /**
   * Associates the specified value with the specified property.
   */
  public void setProperty(String property, Object value) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Writes this SOAPMessage object to the given output stream. The
   * externalization format is as defined by the SOAP 1.1 with Attachments
   * specification. If there are no attachments, just an XML stream is written
   * out. For those messages that have attachments, writeTo writes a
   * MIME-encoded byte stream. Note that this method does not write the
   * transport-specific MIME Headers of the Message
   */
  public abstract void writeTo(OutputStream out)
      throws SOAPException, IOException;

}

