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

package javax.mail.internet;
import javax.activation.*;
import javax.mail.*;
import java.io.*;

/**
 * The MimeMultipart class is an implementation of the abstract
 * Multipart class that uses MIME conventions for the multipart data.
 *
 * A MimeMultipart is obtained from a MimePart whose primary type is
 * "multipart" (by invoking the part's getContent() method) or it can
 * be created by a client as part of creating a new MimeMessage.
 *
 * The default multipart subtype is "mixed". The other multipart
 * subtypes, such as "alternative", "related", and so on, can be
 * implemented as subclasses of MimeMultipart with additional methods
 * to implement the additional semantics of that type of multipart
 * content. The intent is that service providers, mail JavaBean
 * writers and mail clients will write many such subclasses and their
 * Command Beans, and will install them into the JavaBeans Activation
 * Framework, so that any JavaMail implementation and its clients can
 * transparently find and use these classes. Thus, a MIME multipart
 * handler is treated just like any other type handler, thereby
 * decoupling the process of providing multipart handlers from the
 * JavaMail API. Lacking these additional MimeMultipart subclasses,
 * all subtypes of MIME multipart data appear as MimeMultipart
 * objects.
 *
 * An application can directly construct a MIME multipart object of
 * any subtype by using the MimeMultipart(String subtype)
 * constructor. For example, to create a "multipart/alternative"
 * object, use new MimeMultipart("alternative").
 *
 * The mail.mime.multipart.ignoremissingendboundary property may be
 * set to false to cause a MessagingException to be thrown if the
 * multipart data does not end with the required end boundary line. If
 * this property is set to true or not set, missing end boundaries are
 * not considered an error and the final body part ends at the end of
 * the data.
 *
 * The mail.mime.multipart.ignoremissingboundaryparameter System
 * property may be set to false to cause a MessagingException to be
 * thrown if the Content-Type of the MimeMultipart does not include a
 * boundary parameter. If this property is set to true or not set, the
 * multipart parsing code will look for a line that looks like a
 * bounary line and use that as the boundary separating the parts.
 */
public class MimeMultipart extends Multipart {

  /**
   * The DataSource supplying our InputStream.
   */
  protected DataSource ds;

  /**
   * Have we parsed the data from our InputStream yet? Defaults to
   * true; set to false when our constructor is given a DataSource
   * with an InputStream that we need to parse.
   */
  protected boolean parsed;

  /**
   * Default constructor. An empty MimeMultipart object is
   * created. Its content type is set to "multipart/mixed". A unique
   * boundary string is generated and this string is setup as the
   * "boundary" parameter for the contentType field.  MimeBodyParts
   * may be added later.
   */
  public MimeMultipart()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Construct a MimeMultipart object of the given subtype. A unique
   * boundary string is generated and this string is setup as the
   * "boundary" parameter for the contentType field.  MimeBodyParts
   * may be added later.
   */
  public MimeMultipart(String subtype)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Create and return an InternetHeaders object that loads the
   * headers from the given InputStream. Subclasses can override this
   * method to return a subclass of InternetHeaders, if
   * necessary. This implementation simply constructs and returns an
   * InternetHeaders object.
   */
  protected InternetHeaders createInternetHeaders(InputStream is)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Create and return a MimeBodyPart object to represent a body part
   * parsed from the InputStream. Subclasses can override this method
   * to return a subclass of MimeBodyPart, if necessary. This
   * implementation simply constructs and returns a MimeBodyPart
   * object.
   */
  protected MimeBodyPart createMimeBodyPart(InputStream is)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Create and return a MimeBodyPart object to represent a body part
   * parsed from the InputStream. Subclasses can override this method
   * to return a subclass of MimeBodyPart, if necessary. This
   * implementation simply constructs and returns a MimeBodyPart
   * object.
   */
  protected MimeBodyPart createMimeBodyPart(InternetHeaders headers,
					    byte[] content)
    throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the specified BodyPart. BodyParts are numbered starting at 0.
   */
  public BodyPart getBodyPart(int index) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the MimeBodyPart referred to by the given ContentID
   * (CID). Returns null if the part is not found.
   */
  public BodyPart getBodyPart(String CID) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the number of enclosed BodyPart objects.
   */
  public int getCount() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the preamble text, if any, that appears before the first body
   * part of this multipart. Some protocols, such as IMAP, will not
   * allow access to the preamble text.
   */
  public String getPreamble() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return true if the final boundary line for this multipart was
   * seen. When parsing multipart content, this class will (by
   * default) terminate parsing with no error if the end of input is
   * reached before seeing the final multipart boundary line. In such
   * a case, this method will return false. (If the System property
   * "mail.mime.multipart.ignoremissingendboundary" is set to false,
   * parsing such a message will instead throw a MessagingException.)
   */
  public boolean isComplete() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Parse the InputStream from our DataSource, constructing the
   * appropriate MimeBodyParts. The parsed flag is set to true, and if
   * true on entry nothing is done. This method is called by all other
   * methods that need data for the body parts, to make sure the data
   * has been parsed.
   */
  protected void parse() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the preamble text to be included before the first body
   * part. Applications should generally not include any preamble
   * text. In some cases it may be helpful to include preamble text
   * with instructions for users of pre-MIME software. The preamble
   * text should be complete lines, including newlines.
   */
  public void setPreamble(String preamble) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the subtype. This method should be invoked only on a new
   * MimeMultipart object created by the client. The default subtype
   * of such a multipart object is "mixed".
   */
  public void setSubType(String subtype) throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Update headers. The default implementation here just calls the
   * updateHeaders method on each of its children BodyParts.
   *
   * Note that the boundary parameter is already set up when a new and
   * empty MimeMultipart object is created.
   *
   * This method is called when the saveChanges method is invoked on
   * the Message object containing this Multipart. This is typically
   * done as part of the Message send process, however note that a
   * client is free to call it any number of times. So if the header
   * updating process is expensive for a specific MimeMultipart
   * subclass, then it might itself want to track whether its internal
   * state actually did change, and do the header updating only if
   * necessary.
   */
  protected void updateHeaders() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Iterates through all the parts and outputs each Mime part
   * separated by a boundary.
   */
  public void writeTo(OutputStream os) throws IOException, MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }
}
