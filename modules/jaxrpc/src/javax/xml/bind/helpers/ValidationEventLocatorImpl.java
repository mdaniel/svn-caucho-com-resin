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

package javax.xml.bind.helpers;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.bind.*;
import java.net.*;

public class ValidationEventLocatorImpl implements ValidationEventLocator {

  private URL _url;
  private Locator _loc;
  private Node _node;
  private Object _object;
  private int _offset;
  private SAXParseException _exception;

  public ValidationEventLocatorImpl()
  {
  }

  public ValidationEventLocatorImpl(Locator loc)
  {
    this._loc = loc;
  }

  public ValidationEventLocatorImpl(Node node)
  {
    this._node = node;
  }

  public ValidationEventLocatorImpl(Object object)
  {
    this._object = object;
  }

  public ValidationEventLocatorImpl(SAXParseException e)
  {
    this._exception = e;
  }

  public int getColumnNumber()
  {
    return _loc.getColumnNumber();
  }

  public int getLineNumber()
  {
    return _loc.getLineNumber();
  }

  public Node getNode()
  {
    return _node;
  }

  public Object getObject()
  {
    return _object;
  }

  public int getOffset()
  {
    return _offset;
  }

  public URL getURL()
  {
    return _url;
  }

  public void setColumnNumber(int columnNumber)
  {
    throw new UnsupportedOperationException();
  }

  public void setLineNumber(int lineNumber)
  {
    throw new UnsupportedOperationException();
  }

  public void setNode(Node node)
  {
    _node = node;
  }

  public void setObject(Object object)
  {
    object = _object;
  }

  public void setOffset(int offset)
  {
    _offset = offset;
  }

  public void setURL(URL url)
  {
    _url = url;
  }

}

