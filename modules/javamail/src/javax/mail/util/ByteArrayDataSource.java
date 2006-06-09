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

package javax.mail.util;
import javax.mail.*;
import java.io.*;

/**
 * A DataSource backed by a byte array. The byte array may be passed
 * in directly, or may be initialized from an InputStream or a String.
 * Since: JavaMail 1.4
 */
public class ByteArrayDataSource {

  /**
   * Create a ByteArrayDataSource with data from the specified byte
   * array and with the specified MIME type.  data - the datatype -
   * the MIME type
   */
  public ByteArrayDataSource(byte[] data, String type)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Create a ByteArrayDataSource with data from the specified
   * InputStream and with the specified MIME type. The InputStream is
   * read completely and the data is stored in a byte array.  is - the
   * InputStreamtype - the MIME type IOException - errors reading the
   * stream
   */
  public ByteArrayDataSource(InputStream is, String type) throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Create a ByteArrayDataSource with data from the specified String
   * and with the specified MIME type. The MIME type should include a
   * charset parameter specifying the charset to be used for the
   * string. If the parameter is not included, the default charset is
   * used.  data - the Stringtype - the MIME type IOException - errors
   * reading the String
   */
  public ByteArrayDataSource(String data, String type) throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the MIME content type of the data.
   */
  public String getContentType()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return an InputStream for the data. Note that a new stream is
   * returned each time this method is called.
   */
  public InputStream getInputStream() throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the name of the data. By default, an empty string ("") is returned.
   */
  public String getName()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return an OutputStream for the data. Writing the data is not
   * supported; an IOException is always thrown.
   */
  public OutputStream getOutputStream() throws IOException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the name of the data.
   */
  public void setName(String name)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
