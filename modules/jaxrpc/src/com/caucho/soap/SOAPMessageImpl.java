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

package com.caucho.soap;
import javax.xml.soap.*;
import java.io.*;
import javax.activation.*;
import java.util.*;

/**
 * Implementation of SOAPMessage
 */
public abstract class SOAPMessageImpl extends SOAPMessage {

  public SOAPMessageImpl()
  {
  }

  public void addAttachmentPart(AttachmentPart attachmentPart)
  {
    throw new UnsupportedOperationException();
  }


  public int countAttachments()
  {
    throw new UnsupportedOperationException();
  }


  public AttachmentPart createAttachmentPart()
  {
    throw new UnsupportedOperationException();
  }


  public AttachmentPart createAttachmentPart(DataHandler dataHandler)
  {
    throw new UnsupportedOperationException();
  }


  public AttachmentPart createAttachmentPart(Object content, String contentType)
  {
    throw new UnsupportedOperationException();
  }

  public AttachmentPart getAttachment(SOAPElement element)
      throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public Iterator getAttachments()
  {
    throw new UnsupportedOperationException();
  }

  public Iterator getAttachments(MimeHeaders headers)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Retrieves a description of this SOAPMessage object's content.
   */
  public String getContentDescription()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns all the transport-specific MIME headers for this SOAPMessage
   * object in a transport-independent fashion.
   */
  public MimeHeaders getMimeHeaders()
  {
    throw new UnsupportedOperationException();
  }


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
    /*
    return new SOAPBodyImpl(this);
    */
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
  public SOAPPart getSOAPPart()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Removes all AttachmentPart objects that have been added to this
   * SOAPMessage object. This method does not touch the SOAP part.
   */
  public void removeAllAttachments()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Removes all the AttachmentPart objects that have header entries that match
   * the specified headers.
   */
  public void removeAttachments(MimeHeaders headers)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Updates this SOAPMessage object with all the changes that have been made
   * to it.
   */
  public void saveChanges() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Indicates whether this SOAPMessage object needs to have the method
   * saveChanges called on it.
   */
  public boolean saveRequired()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Sets the description of this SOAPMessage object's content with the given
   * description.
   */
  public void setContentDescription(String description)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Associates the specified value with the specified property.
   */
  public void setProperty(String property, Object value) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  public void writeTo(OutputStream out)
      throws SOAPException, IOException
  {
  }

}

