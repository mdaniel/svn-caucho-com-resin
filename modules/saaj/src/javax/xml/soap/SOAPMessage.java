/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.xml.soap;

import java.util.Iterator;

import java.io.OutputStream;
import java.io.IOException;

/**
 * Implements the full SOAP message.
 */
public abstract class SOAPMessage {
  public final static String CHARACTER_SET_ENCODING =
    "javax.xml.soap.character-set-encoding";
  public final static String WRITE_XML_DECLARATION =
    "javax.xml.soap.write-xml-declaration";
  
  /**
   * Sets the message description.
   */
  abstract public void setContentDescription(String description);
  
  /**
   * Gets the message description.
   */
  abstract public String getContentDescription();
  
  /**
   * Gets the SOAP part.
   */
  abstract public SOAPPart getSOAPPart();
  
  /**
   * Gets the SOAP body.
   */
  public SOAPBody getSOAPBody()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Gets the SOAP header.
   */
  public SOAPHeader getSOAPHeader()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Removes the attachments.
   */
  abstract public void removeAllAttachments();
  
  /**
   * Returns the number of attachments.
   */
  abstract public int countAttachments();
  
  /**
   * Returns an iteration of the attachments.
   */
  abstract public Iterator getAttachments();
  
  /**
   * Returns an iteration with the given headers.
   */
  abstract public Iterator getAttachments(MimeHeaders headers);
  
  /**
   * Adds a new attachment.
   */
  abstract public void addAttachmentPart(AttachmentPart part);
  
  /**
   * Creates a new attachment.
   */
  abstract public AttachmentPart createAttachmentPart();
  
  /**
   * Creates a new attachment.
   */
  // public AttachmentPart createAttachmentPart(javax.activation.DataHandler handler)
  // { throw new UnsupportedOperationException(); }

  /**
   * Returns the mime headers.
   */
  abstract public MimeHeaders getMimeHeaders();

  /**
   * Creates an attachment part.
   */
  public AttachmentPart createAttachmentPart(Object content,
					     String contentType)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Updates the changes.
   */
  abstract public void saveChanges()
    throws SOAPException;

  /**
   * Returns true if a save is required.
   */
  abstract public boolean saveRequired();

  /**
   * Writes to the output stream.
   */
  abstract public void writeTo(OutputStream out)
    throws SOAPException, IOException;

  /**
   * Sets a property.
   */
  public void setProperty(String property,
			  Object value)
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets a property.
   */
  public Object getProperty(String property)
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }
}
