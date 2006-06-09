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
import javax.mail.*;
import java.io.*;

/**
 * A MimeBodyPart that handles data that has already been
 * encoded. This class is useful when constructing a message and
 * attaching data that has already been encoded (for example, using
 * base64 encoding). The data may have been encoded by the
 * application, or may have been stored in a file or database in
 * encoded form. The encoding is supplied when this object is
 * created. The data is attached to this object in the usual fashion,
 * by using the setText, setContent, or setDataHandler methods.
 * Since: JavaMail 1.4
 */
public class PreencodedMimeBodyPart extends MimeBodyPart {

  /**
   * Create a PreencodedMimeBodyPart that assumes the data is encoded
   * using the specified encoding. The encoding must be a MIME
   * supported Content-Transfer-Encoding.
   */
  public PreencodedMimeBodyPart(String encoding)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the content transfer encoding specified when this object
   * was created.
   */
  public String getEncoding() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Force the Content-Transfer-Encoding header to use the encoding
   * that was specified when this object was created.
   */
  protected void updateHeaders() throws MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Output the body part as an RFC 822 format stream.
   */
  public void writeTo(OutputStream os) throws IOException, MessagingException
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
