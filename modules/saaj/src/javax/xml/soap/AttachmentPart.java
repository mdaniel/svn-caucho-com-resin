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

package javax.xml.soap;

import java.util.Iterator;

import java.io.OutputStream;
import java.io.IOException;

import javax.activation.DataHandler;

/**
 * Implements an attachment.
 */
public abstract class AttachmentPart {
  /**
   * Returns the number of bytes in the attachment.
   */
  abstract public int getSize()
    throws SOAPException;
  
  /**
   * Gets the content.
   */
  abstract public Object clearContent();
  
  /**
   * Sets the content.
   */
  abstract public void setContent(Object object, String contentType);
  
  /**
   * Gets the DataHandler.
   */
  abstract public DataHandler getDataHandler()
    throws SOAPException;
  
  /**
   * Sets the DataHandler.
   */
  abstract public void setDataHandler(DataHandler handler);

  /**
   * Returns the contentId header.
   */
  abstract public String getContentId();
  
  /**
   * Returns the content location header.
   */
  public String getContentLocation()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content type header.
   */
  public String getContentType()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the contentId header.
   */
  public void setContentId(String id)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content location header.
   */
  public void setContentLocation(String location)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content type header.
   */
  public void setContentType(String type)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes a mime header.
   */
  abstract public void removeMimeHeader(String header);

  /**
   * Removes all the mime header.
   */
  abstract public void removeAllMimeHeaders();

  /**
   * Removes the values matching the string.
   */
  abstract public String []getMimeHeader(String name);

  /**
   * Sets the values matching the string.
   */
  abstract public void setMimeHeader(String name, String value);

  /**
   * Adds the values matching the string.
   */
  abstract public void addMimeHeader(String name, String value);

  /**
   * Gets the headers matching the string.
   */
  abstract public Iterator getAllMimeHeaders();

  /**
   * Gets the headers matching the string.
   */
  abstract public Iterator getMatchingMimeHeaders(String []name);

  /**
   * Gets the headers not matching the strings.
   */
  abstract public Iterator getNonMatchingMimeHeaders(String []name);
}
