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
import java.util.*;

/**
 * A container for MimeHeader objects, which represent the MIME headers present
 * in a MIME part of a message. This class is used primarily when an
 * application wants to retrieve specific attachments based on certain MIME
 * headers and values. This class will most likely be used by implementations
 * of AttachmentPart and other MIME dependent parts of the SAAJ API. See
 * Also:SOAPMessage.getAttachments(), AttachmentPart
 */
public class MimeHeaders {

  /**
   * Constructs a default MimeHeaders object initialized with an empty Vector
   * object.
   */
  public MimeHeaders()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Adds a MimeHeader object with the specified name and value to this
   * MimeHeaders object's list of headers. Note that RFC822 headers can contain
   * only US-ASCII characters.
   */
  public void addHeader(String name, String value)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns all the MimeHeaders in this MimeHeaders object.
   */
  public Iterator getAllHeaders()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns all of the values for the specified header as an array of String
   * objects.
   */
  public String[] getHeader(String name)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns all the MimeHeader objects whose name matches a name in the given
   * array of names.
   */
  public Iterator getMatchingHeaders(String[] names)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns all of the MimeHeader objects whose name does not match a name in
   * the given array of names.
   */
  public Iterator getNonMatchingHeaders(String[] names)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Removes all the header entries from this MimeHeaders object.
   */
  public void removeAllHeaders()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Remove all MimeHeader objects whose name matches the given name.
   */
  public void removeHeader(String name)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Replaces the current value of the first header entry whose name matches
   * the given name with the given value, adding a new header if no existing
   * header name matches. This method also removes all matching headers after
   * the first one. Note that RFC822 headers can contain only US-ASCII
   * characters.
   */
  public void setHeader(String name, String value)
  {
    throw new UnsupportedOperationException();
  }

}

