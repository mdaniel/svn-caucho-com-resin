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

package javax.mail;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.Enumeration;

import javax.activation.DataHandler;

/**
 * Represents part of a mail message.
 */
public interface Part {
  public static final String ATTACHMENT = "attachment";
  public static final String INLINE = "inline";

  /**
   * Returns the size of the part in bytes.
   */
  public int getSize()
    throws MessagingException;

  /**
   * Returns the number of lines of the part.
   */
  public int getLineCount()
    throws MessagingException;

  /**
   * Returns the content-type of the part.
   */
  public String getContextType()
    throws MessagingException;

  /**
   * Returns true if the part is of the specified mime-type.
   */
  public boolean isMimeType(String mimeType)
    throws MessagingException;

  /**
   * Returns the disposition.
   */
  public String getDisposition()
    throws MessagingException;

  /**
   * Sets the part's disposition.
   */
  public void setDisposition(String disposition)
    throws MessagingException;

  /**
   * Returns the part's description.
   */
  public String getDescription()
    throws MessagingException;

  /**
   * Sets the part's description.
   */
  public void setDescription(String description)
    throws MessagingException;

  /**
   * Returns the part's filename.
   */
  public String getFileName()
    throws MessagingException;

  /**
   * Sets the p art's filename.
   */
  public void setFileName(String filename)
    throws MessagingException;

  /**
   * Returns an input stream to the content.
   */
  public InputStream getInputStream()
    throws IOException, MessagingException;

  /**
   * Returns a DataHandler for the part.
   */
  public DataHandler getDataHandler()
    throws MessagingException;

  /**
   * Returns the content as an object.
   */
  public Object getContent()
    throws IOException, MessagingException;

  /**
   * Sets the handler for the part's content.
   */
  public void setDataHandler(DataHandler handler)
    throws MessagingException;

  /**
   * Sets the part's content.
   */
  public void setContent(Object obj, String type)
    throws MessagingException;

  /**
   * Sets the content as a text string.
   */
  public void setText(String text)
    throws MessagingException;

  /**
   * Sets the multipart-content as the content.
   */
  public void setContent(Multipart multipart)
    throws MessagingException;

  /**
   * Output a bytestream for the part.
   */
  public void writeTo(OutputStream os)
    throws IOException, MessagingException;

  /**
   * Returns the headers for the given name.
   */
  public String []getHeader(String headerName)
    throws MessagingException;

  /**
   * Sets the given header.
   */
  public void setHeader(String headerName,
			String headerValue)
    throws MessagingException;

  /**
   * Adds a header.
   */
  public void addHeader(String headerName,
			String headerValue)
    throws MessagingException;

  /**
   * Removes a header.
   */
  public void removeHeader(String headerName)
    throws MessagingException;

  /**
   * Returns a part's headers.
   */
  public Enumeration getAllHeaders()
    throws MessagingException;

  /**
   * Returns matching headers.
   */
  public Enumeration getMatchingHeaders(String []headerNames)
    throws MessagingException;

  /**
   * Returns non-matching headers.
   */
  public Enumeration getNonMatchingHeaders(String []headerNames)
    throws MessagingException;
}
