/*
* Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
* @author Emil Ong
*/

package com.caucho.xml.saaj;

import java.io.*;
import java.util.*;

import javax.activation.*;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.soap.*;
import javax.xml.transform.stream.StreamSource;

import com.caucho.util.Base64;

public class AttachmentPartImpl extends AttachmentPart {
  private DataHandler _dataHandler;
  private MimeHeaders _headers = new MimeHeaders();

  public void addMimeHeader(String name, String value)
  {
    _headers.addHeader(name, value);
  }

  public Iterator getAllMimeHeaders()
  {
    return _headers.getAllHeaders();
  }

  public Iterator getMatchingMimeHeaders(String[] names)
  {
    return _headers.getMatchingHeaders(names);
  }

  public String[] getMimeHeader(String name)
  {
    return _headers.getHeader(name);
  }

  public Iterator getNonMatchingMimeHeaders(String[] names)
  {
    return _headers.getNonMatchingHeaders(names);
  }

  public void removeAllMimeHeaders()
  {
    _headers.removeAllHeaders();
  }

  public void removeMimeHeader(String header)
  {
    _headers.removeHeader(header);
  }

  public void setMimeHeader(String name, String value)
  {
    _headers.setHeader(name, value);
  }

  //////////////////

  public void clearContent()
  {
    _dataHandler = null;
  }

  public InputStream getBase64Content() 
    throws SOAPException
  {
    try {
      InputStream is = getRawContent();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      OutputStreamWriter w = new OutputStreamWriter(os);

      Base64.encode(w, is);

      return new ByteArrayInputStream(os.toByteArray());
    }
    catch (IOException e) {
      throw new SOAPException(e);
    }
  }

  public Object getContent() 
    throws SOAPException
  {
    if (_dataHandler == null)
      throw new SOAPException("No content available");

    try {
      // XXX
      // seems like we shouldn't have to do this, but dataHandler.getContent()
      // doesn't do what i expected.
      /*
      if ("text/xml".equals(getContentType()))
        return new StreamSource(_dataHandler.getInputStream());
      else*/
        return _dataHandler.getContent();
    }
    catch (IOException e) {
      throw new SOAPException(e);
    }
  }

  public DataHandler getDataHandler() 
    throws SOAPException
  {
    if (_dataHandler == null)
      throw new SOAPException("DataHandler not set");

    return _dataHandler;
  }

  public InputStream getRawContent() 
    throws SOAPException
  {
    if (_dataHandler == null)
      throw new SOAPException("Content not set");

    try {
      return _dataHandler.getInputStream();
    }
    catch (IOException e) {
      throw new SOAPException(e);
    }
  }

  public byte[] getRawContentBytes() 
    throws SOAPException
  {
    InputStream is = getRawContent();

    try {
      if (is.available() < 0)
        throw new SOAPException("No content available");

      ByteArrayOutputStream os = new ByteArrayOutputStream();

      for (int b = is.read(); b >= 0; b = is.read())
        os.write(b);

      // XXX reset datahandler with this byte array?

      return os.toByteArray();
    }
    catch (IOException e) {
      throw new SOAPException(e);
    }
  }

  public int getSize() 
    throws SOAPException
  {
    if (_dataHandler == null)
      return 0;

    try {
      InputStream is = _dataHandler.getInputStream();
      return is.available();
    }
    catch (IOException e) {
      return -1;
    }
  }

  public void setBase64Content(InputStream content, String contentType) 
    throws SOAPException
  {
    if (content == null)
      throw new SOAPException("Content InputStream cannot be null");

    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      Base64.decode(new InputStreamReader(content), os);

      byte[] buffer = os.toByteArray();
      setRawContentBytes(buffer, 0, buffer.length, contentType);
    }
    catch (Exception e) {
      // IOException and any Exceptions thrown by Base64 (e.g. ArrayOutOfBounds)
      throw new SOAPException(e);
    }
  }

  public void setContent(Object object, String contentType)
  {
    setContentType(contentType);
    _dataHandler = new DataHandler(object, contentType);
  }

  public void setDataHandler(DataHandler dataHandler)
  {
    if (dataHandler == null)
      throw new IllegalArgumentException("DataHandler cannot be null");

    _dataHandler = dataHandler;
  }

  public void setRawContent(InputStream content, String contentType) 
    throws SOAPException
  {
    setContentType(contentType);
  
    try {
      DataSource source = new ByteArrayDataSource(content, contentType);
      _dataHandler = new DataHandler(source);
    }
    catch (IOException e) {
      throw new SOAPException(e);
    }
  }

  public void setRawContentBytes(byte[] content, int offset, int len, 
                                 String contentType) 
    throws SOAPException
  {
    setContentType(contentType);

    try {
      InputStream stream = new ByteArrayInputStream(content, offset, len);
      DataSource source = new ByteArrayDataSource(stream, contentType);
      _dataHandler = new DataHandler(source);
    }
    catch (IOException e) {
      throw new SOAPException(e);
    }
  }
}

