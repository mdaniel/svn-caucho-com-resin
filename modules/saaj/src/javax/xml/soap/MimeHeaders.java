/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.xml.soap;

import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Implements the mime headers.
 */
public class MimeHeaders {
  private ArrayList _headers = new ArrayList();

  /**
   * Returns the values matching the string.
   */
  public String []getHeader(String name)
  {
    ArrayList values = null;

    for (int i = 0; i < _headers.size(); i++) {
      MimeHeader header = (MimeHeader) _headers.get(i);

      if (header.getName().equals(name)) {
	if (values == null)
	  values = new ArrayList();

	values.add(header.getValue());
      }
    }

    if (values == null)
      return null;
    else
      return (String []) values.toArray(new String[values.size()]);
  }

  /**
   * Sets the header value.
   */
  public void setHeader(String name, String value)
  {
    removeHeader(name);
    
    _headers.add(new MimeHeader(name, value));
  }

  /**
   * Adds the header value.
   */
  public void addHeader(String name, String value)
  {
    _headers.add(new MimeHeader(name, value));
  }

  /**
   * Removes the header.
   */
  public void removeHeader(String name)
  {
    for (int i = _headers.size() - 1; i >= 0; i--) {
      MimeHeader header = (MimeHeader) _headers.get(i);

      if (header.getName().equals(name))
	_headers.remove(i);
    }
  }

  /**
   * Removes all headers.
   */
  public void removeAllHeaders()
  {
    _headers.clear();
  }

  /**
   * Returns all the headers.
   */
  public Iterator getAllHeaders()
  {
    return _headers.iterator();
  }

  /**
   * Returns all matching headers.
   */
  public Iterator getMatchingHeaders(String []names)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns non matching headers.
   */
  public Iterator getNonMatchingHeaders(String []names)
  {
    throw new UnsupportedOperationException();
  }
}
