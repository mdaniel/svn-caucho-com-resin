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
import javax.xml.transform.*;
import org.w3c.dom.*;
import java.util.*;

/**
 * The container for the SOAP-specific portion of a SOAPMessage object.
 */
public abstract class SOAPPart implements Document, Node {

  public SOAPPart()
  {
  }

  /**
   * Creates a MimeHeader object with the specified name and value and adds it
   * to this SOAPPart object.
   */
  public abstract void addMimeHeader(String name, String value);


  /**
   * Retrieves all the headers for this SOAPPart object as an iterator over the
   * MimeHeader objects.
   */
  public abstract Iterator getAllMimeHeaders();


  /**
   * Returns the content of the SOAPEnvelope as a JAXP Source object.
   */
  public abstract Source getContent() throws SOAPException;


  /**
   * Retrieves the value of the MIME header whose name is "Content-Id".
   */
  public String getContentId()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Retrieves the value of the MIME header whose name is "Content-Location".
   */
  public String getContentLocation()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the SOAPEnvelope object associated with this SOAPPart object. Once
   * the SOAP envelope is obtained, it can be used to get its contents.
   */
  public abstract SOAPEnvelope getEnvelope() throws SOAPException;


  /**
   * Retrieves all MimeHeader objects that match a name in the given array.
   */
  public abstract Iterator getMatchingMimeHeaders(String[] names);


  /**
   * Gets all the values of the MimeHeader object in this SOAPPart object that
   * is identified by the given String.
   */
  public abstract String[] getMimeHeader(String name);


  /**
   * Retrieves all MimeHeader objects whose name does not match a name in the
   * given array.
   */
  public abstract Iterator getNonMatchingMimeHeaders(String[] names);


  /**
   * Removes all the MimeHeader objects for this SOAPEnvelope object.
   */
  public abstract void removeAllMimeHeaders();


  /**
   * Removes all MIME headers that match the given name.
   */
  public abstract void removeMimeHeader(String header);


  /**
   * Sets the content of the SOAPEnvelope object with the data from the given
   * Source object. This Source must contain a valid SOAP document.
   */
  public abstract void setContent(Source source) throws SOAPException;


  /**
   * Sets the value of the MIME header named "Content-Id" to the given String.
   */
  public void setContentId(String contentId)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Sets the value of the MIME header "Content-Location" to the given String.
   */
  public void setContentLocation(String contentLocation)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Changes the first header entry that matches the given header name so that
   * its value is the given value, adding a new header with the given name and
   * value if no existing header is a match.
   */
  public abstract void setMimeHeader(String name, String value);

}

