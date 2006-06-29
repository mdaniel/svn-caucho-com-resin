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
 * A single attachment to a SOAPMessage object. A SOAPMessage object may
 * contain zero, one, or many AttachmentPart objects. Each AttachmentPart
 * object consists of two parts, application-specific content and associated
 * MIME headers. The MIME headers consists of name/value pairs that can be used
 * to identify and describe the content. An AttachmentPart object must conform
 * to certain standards. It must conform to MIME [RFC2045] standards It MUST
 * contain content The header portion MUST include the following header:
 * Content-Type This header identifies the type of data in the content of an
 * AttachmentPart object and MUST conform to [RFC2045]. The following is an
 * example of a Content-Type header: Content-Type: application/xml The
 * following line of code, in which ap is an AttachmentPart object, sets the
 * header shown in the previous example. ap.setMimeHeader("Content-Type",
 * "application/xml"); There are no restrictions on the content portion of an
 * AttachmentPart object. The content may be anything from a simple plain text
 * object to a complex XML document or image file. An AttachmentPart object is
 * created with the method SOAPMessage.createAttachmentPart. After setting its
 * MIME headers, the AttachmentPart object is added to the message that created
 * it with the method SOAPMessage.addAttachmentPart. The following code
 * fragment, in which m is a SOAPMessage object and contentStringl is a String,
 * creates an instance of AttachmentPart, sets the AttachmentPart object with
 * some content and header information, and adds the AttachmentPart object to
 * the SOAPMessage object. AttachmentPart ap1 = m.createAttachmentPart();
 * ap1.setContent(contentString1, "text/plain"); m.addAttachmentPart(ap1); The
 * following code fragment creates and adds a second AttachmentPart instance to
 * the same message. jpegData is a binary byte buffer representing the jpeg
 * file. AttachmentPart ap2 = m.createAttachmentPart(); byte[] jpegData = ...;
 * ap2.setContent(new ByteArrayInputStream(jpegData), "image/jpeg");
 * m.addAttachmentPart(ap2); The getContent method retrieves the contents and
 * header from an AttachmentPart object. Depending on the DataContentHandler
 * objects present, the returned Object can either be a typed Java object
 * corresponding to the MIME type or an InputStream object that contains the
 * content as bytes. String content1 = ap1.getContent(); java.io.InputStream
 * content2 = ap2.getContent(); The method clearContent removes all the content
 * from an AttachmentPart object but does not affect its header information.
 * ap1.clearContent();
 */
public abstract class AttachmentPart {
  public AttachmentPart()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Adds a MIME header with the specified name and value to this
   * AttachmentPart object. Note that RFC822 headers can contain only US-ASCII
   * characters.
   */
  public abstract void addMimeHeader(String name, String value);


  /**
   * Clears out the content of this AttachmentPart object. The MIME header
   * portion is left untouched.
   */
  public abstract void clearContent();


  /**
   * Retrieves all the headers for this AttachmentPart object as an iterator
   * over the MimeHeader objects.
   */
  public abstract Iterator getAllMimeHeaders();


  /**
   * Returns an InputStream which can be used to obtain the content of
   * AttachmentPart as Base64 encoded character data, this method would base64
   * encode the raw bytes of the attachment and return.
   */
  public abstract InputStream getBase64Content() throws SOAPException;


  /**
   * Gets the content of this AttachmentPart object as a Java object. The type
   * of the returned Java object depends on (1) the DataContentHandler object
   * that is used to interpret the bytes and (2) the Content-Type given in the
   * header. For the MIME content types "text/plain", "text/html" and
   * "text/xml", the DataContentHandler object does the conversions to and from
   * the Java types corresponding to the MIME types. For other MIME types,the
   * DataContentHandler object can return an InputStream object that contains
   * the content data as raw bytes. A SAAJ-compliant implementation must, as a
   * minimum, return a java.lang.String object corresponding to any content
   * stream with a Content-Type value of text/plain, a
   * javax.xml.transform.stream.StreamSource object corresponding to a content
   * stream with a Content-Type value of text/xml, a java.awt.Image object
   * corresponding to a content stream with a Content-Type value of image/gif
   * or image/jpeg. For those content types that an installed
   * DataContentHandler object does not understand, the DataContentHandler
   * object is required to return a java.io.InputStream object with the raw
   * bytes.
   */
  public abstract Object getContent() throws SOAPException;


  /**
   * Gets the value of the MIME header whose name is "Content-ID".
   */
  public String getContentId()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the value of the MIME header whose name is "Content-Location".
   */
  public String getContentLocation()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the value of the MIME header whose name is "Content-Type".
   */
  public String getContentType()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the DataHandler object for this AttachmentPart object.
   */
  public abstract DataHandler getDataHandler() throws SOAPException;


  /**
   * Retrieves all MimeHeader objects that match a name in the given array.
   */
  public abstract Iterator getMatchingMimeHeaders(String[] names);


  /**
   * Gets all the values of the header identified by the given String.
   */
  public abstract String[] getMimeHeader(String name);


  /**
   * Retrieves all MimeHeader objects whose name does not match a name in the
   * given array.
   */
  public abstract Iterator getNonMatchingMimeHeaders(String[] names);


  /**
   * Gets the content of this AttachmentPart object as an InputStream as if a
   * call had been made to getContent and no DataContentHandler had been
   * registered for the content-type of this AttachmentPart. Note that reading
   * from the returned InputStream would result in consuming the data in the
   * stream. It is the responsibility of the caller to reset the InputStream
   * appropriately before calling a Subsequent API. If a copy of the raw
   * attachment content is required then the getRawContentBytes() API should be
   * used instead.
   */
  public abstract InputStream getRawContent() throws SOAPException;


  /**
   * Gets the content of this AttachmentPart object as a byte[] array as if a
   * call had been made to getContent and no DataContentHandler had been
   * registered for the content-type of this AttachmentPart.
   */
  public abstract byte[] getRawContentBytes() throws SOAPException;


  /**
   * Returns the number of bytes in this AttachmentPart object.
   */
  public abstract int getSize() throws SOAPException;


  /**
   * Removes all the MIME header entries.
   */
  public abstract void removeAllMimeHeaders();


  /**
   * Removes all MIME headers that match the given name.
   */
  public abstract void removeMimeHeader(String header);


  /**
   * Sets the content of this attachment part from the Base64 source
   * InputStream and sets the value of the Content-Type header to the value
   * contained in contentType, This method would first decode the base64 input
   * and write the resulting raw bytes to the attachment. A subsequent call to
   * getSize() may not be an exact measure of the content size.
   */
  public abstract void setBase64Content(InputStream content, String contentType) throws SOAPException;


  /**
   * Sets the content of this attachment part to that of the given Object and
   * sets the value of the Content-Type header to the given type. The type of
   * the Object should correspond to the value given for the Content-Type. This
   * depends on the particular set of DataContentHandler objects in use.
   */
  public abstract void setContent(Object object, String contentType);


  /**
   * Sets the MIME header whose name is "Content-ID" with the given value.
   */
  public void setContentId(String contentId)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Sets the MIME header whose name is "Content-Location" with the given value.
   */
  public void setContentLocation(String contentLocation)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Sets the MIME header whose name is "Content-Type" with the given value.
   */
  public void setContentType(String contentType)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Sets the given DataHandler object as the data handler for this
   * AttachmentPart object. Typically, on an incoming message, the data handler
   * is automatically set. When a message is being created and populated with
   * content, the setDataHandler method can be used to get data from various
   * data sources into the message.
   */
  public abstract void setDataHandler(DataHandler dataHandler);


  /**
   * Changes the first header entry that matches the given name to the given
   * value, adding a new header if no existing header matches. This method also
   * removes all matching headers but the first. Note that RFC822 headers can
   * only contain US-ASCII characters.
   */
  public abstract void setMimeHeader(String name, String value);


  /**
   * Sets the content of this attachment part to that contained by the
   * InputStream content and sets the value of the Content-Type header to the
   * value contained in contentType. A subsequent call to getSize() may not be
   * an exact measure of the content size.
   */
  public abstract void setRawContent(InputStream content, String contentType) throws SOAPException;


  /**
   * Sets the content of this attachment part to that contained by the byte[]
   * array content and sets the value of the Content-Type header to the value
   * contained in contentType.
   */
  public abstract void setRawContentBytes(byte[] content, int offset, int len, String contentType) throws SOAPException;

}

