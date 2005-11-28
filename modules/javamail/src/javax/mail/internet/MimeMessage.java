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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.mail.internet;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.Date;
import java.util.Enumeration;

import javax.activation.DataHandler;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.MessagingException;

/**
 * Represents a mime-type mail message.
 */
public class MimeMessage extends Message {
  /**
   * Creates a message with a session
   */
  public MimeMessage(Session session)
  {
    super(session);
  }

  /**
   * Returns a from address.
   */
  public Address []getFrom()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the from attribute using mail.user.
   */
  public void setFrom()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the from attribute with an address.
   */
  public void setFrom(Address address)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds new from addresses.
   */
  public void addFrom(Address []addresses)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the recipient addresses.
   */
  public Address []getRecipients(RecipientType type)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the recipients of the given type.
   */
  public void setRecipients(RecipientType type, Address []addresses)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds a set of recipients.
   */
  public void addRecipients(RecipientType type, Address []addresses)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the message subject.
   */
  public String getSubject()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the subject.
   */
  public void setSubject(String subject)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Gets the date the message was sent.
   */
  public Date getSentDate()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the date the message was sent.
   */
  public void setSentDate(Date date)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the date the message was received.
   */
  public Date getReceivedDate()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the message flags.
   */
  public Flags getFlags()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets a given set of flags.
   */
  public void setFlags(Flags flags, boolean set)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generate a message as a reply-to.
   */
  public Message reply(boolean replyToAll)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Saves changes to the message to the store.
   */
  public void saveChanges()
    throws MessagingException
  {
  }

  /**
   * Returns the size of the part in bytes.
   */
  public int getSize()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the number of lines of the part.
   */
  public int getLineCount()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the content-type of the part.
   */
  public String getContentType()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the part is of the specified mime-type.
   */
  public boolean isMimeType(String mimeType)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the disposition.
   */
  public String getDisposition()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the part's disposition.
   */
  public void setDisposition(String disposition)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the part's description.
   */
  public String getDescription()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the part's description.
   */
  public void setDescription(String description)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the part's filename.
   */
  public String getFileName()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the p art's filename.
   */
  public void setFileName(String filename)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns an input stream to the content.
   */
  public InputStream getInputStream()
    throws IOException, MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns a DataHandler for the part.
   */
  public DataHandler getDataHandler()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the content as an object.
   */
  public Object getContent()
    throws IOException, MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the handler for the part's content.
   */
  public void setDataHandler(DataHandler handler)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the part's content.
   */
  public void setContent(Object obj, String type)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the content as a text string.
   */
  public void setText(String text)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the multipart-content as the content.
   */
  public void setContent(Multipart multipart)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Output a bytestream for the part.
   */
  public void writeTo(OutputStream os)
    throws IOException, MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the headers for the given name.
   */
  public String []getHeader(String headerName)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the given header.
   */
  public void setHeader(String headerName,
			String headerValue)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds a header.
   */
  public void addHeader(String headerName,
			String headerValue)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Removes a header.
   */
  public void removeHeader(String headerName)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns a part's headers.
   */
  public Enumeration getAllHeaders()
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns matching headers.
   */
  public Enumeration getMatchingHeaders(String []headerNames)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns non-matching headers.
   */
  public Enumeration getNonMatchingHeaders(String []headerNames)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
