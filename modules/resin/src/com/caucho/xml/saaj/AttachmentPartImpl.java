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
import javax.xml.soap.*;

public class AttachmentPartImpl extends AttachmentPart {
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
    throw new UnsupportedOperationException();
  }

  public InputStream getBase64Content() 
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public Object getContent() 
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public DataHandler getDataHandler() 
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public InputStream getRawContent() 
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public byte[] getRawContentBytes() 
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public int getSize() 
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public void setBase64Content(InputStream content, String contentType) 
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public void setContent(Object object, String contentType)
  {
    setContentType(contentType);

    throw new UnsupportedOperationException();
  }

  public void setDataHandler(DataHandler dataHandler)
  {
    throw new UnsupportedOperationException();
  }

  public void setRawContent(InputStream content, String contentType) 
    throws SOAPException
  {
    setContentType(contentType);

    throw new UnsupportedOperationException();
  }

  public void setRawContentBytes(byte[] content, int offset, int len, 
                                 String contentType) 
    throws SOAPException
  {
    setContentType(contentType);

    throw new UnsupportedOperationException();
  }
}

