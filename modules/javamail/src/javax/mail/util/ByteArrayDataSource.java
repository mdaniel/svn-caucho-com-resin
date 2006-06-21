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
import javax.activation.*;
import java.util.logging.*;

/**
 * A DataSource backed by a byte[]
 */
public class ByteArrayDataSource implements DataSource {

  private static Logger log =
    Logger.getLogger("javax.mail.util.ByteArrayDataSource");

  private byte[] _data;
  private String _type;
  private String _name = "";

  public ByteArrayDataSource(byte[] data, String type)
  {
    this._data = data;
    this._type = type;
  }

  public ByteArrayDataSource(InputStream is, String type) throws IOException
  {
    this._type = type;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    byte[] buf = new byte[1024];

    while(true)
      {
	int numread = is.read(buf, 0, buf.length);

	if (numread == -1) {
	  _data = baos.toByteArray();
	  return;
	}

	baos.write(buf, 0, numread);
      }
  }

  public ByteArrayDataSource(String data, String type) throws IOException
  {
    this._type = type;

    String charset = null;

    try {
      MimeType mimeType = new MimeType(type);
      charset = mimeType.getParameter("charset");
    }
    catch (Exception e) {
      log.log(Level.FINER, "ignoring exception", e);
    }

    if (charset == null) {
	_data = data.getBytes();
    }
    else {
	_data = data.getBytes(charset);
    }
  }

  public String getContentType()
  {
    return _type;
  }

  public InputStream getInputStream() throws IOException
  {
    return new ByteArrayInputStream(_data);
  }

  public String getName()
  {
    return _name;
  }

  public OutputStream getOutputStream() throws IOException
  {
    throw
      new IOException("you are not allowed to write to a ByteArrayDataSource");
  }

  public void setName(String name)
  {
    _name = name;
  }

}
