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

import javax.xml.transform.Source;

import org.w3c.dom.Document;

/**
 * Implements a SOAP part.
 */
public abstract class SOAPPart implements Document {
  /**
   * Returns the envelope.
   */
  abstract public SOAPEnvelope getEnvelope()
    throws SOAPException;

  /**
   * Returns the content id.
   */
  public String getContentId()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content location.
   */
  public String getContentLocation()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the content id.
   */
  public void setContentId(String id)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content location.
   */
  public void setContentLocation(String location)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes the header.
   */
  abstract void removeMimeHeader(String header);

  /**
   * Removes all header.
   */
  abstract void removeAllMimeHeaders();

  /**
   * Gets the mime header.
   */
  abstract String []getMimeHeader(String name);

  /**
   * Sets the mime header.
   */
  abstract void setMimeHeader(String name, String value);

  /**
   * Adds the mime header.
   */
  abstract void addMimeHeader(String name, String value);

  /**
   * Returns all the mime header.
   */
  abstract Iterator getAllMimeHeaders();

  /**
   * Returns the matching mime header.
   */
  abstract Iterator getMatchingMimeHeaders();

  /**
   * Returns the non-matching mime header.
   */
  abstract Iterator getNonMatchingMimeHeaders();

  /**
   * Sets the content.
   */
  abstract public void setContent(Source source)
    throws SOAPException;

  /**
   * Gets the content.
   */
  abstract public Source getContent()
    throws SOAPException;
}
