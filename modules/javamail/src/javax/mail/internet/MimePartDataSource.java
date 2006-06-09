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
 * A utility class that implements a DataSource out of a
 * MimePart. This class is primarily meant for service providers.  See
 * Also:MimePart, javax.activation.DataSource
 */
public class MimePartDataSource implements MessageAware {

  /**
   * The MimePart that provides the data for this DataSource.
   * Since: JavaMail 1.4
   */
  protected MimePart part;

  /**
   * Constructor, that constructs a DataSource from a MimePart.
   */
  public MimePartDataSource(MimePart part)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the content-type of this DataSource.  This implementation
   * just invokes the getContentType method on the MimePart.
   */
  public String getContentType()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns an input stream from this MimePart.
   * This method applies the appropriate transfer-decoding, based on
   * the Content-Transfer-Encoding attribute of this MimePart. Thus
   * the returned input stream is a decoded stream of bytes.
   *
   * This implementation obtains the raw content from the Part using
   * the getContentStream() method and decodes it using the
   * MimeUtility.decode() method.
   */
  public InputStream getInputStream() throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the MessageContext for the current part.
   */
  public MessageContext getMessageContext()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * DataSource method to return a name.
   * This implementation just returns an empty string.
   */
  public String getName()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * DataSource method to return an output stream.
   * This implementation throws the UnknownServiceException.
   */
  public OutputStream getOutputStream() throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
