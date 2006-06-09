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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.mail.internet;
import javax.mail.*;

import java.io.InputStream;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.mail.Header;
import javax.mail.MessagingException;

/**
 * Represents the headers for a smtp message
 */
public class InternetHeaders {
  protected List headers = new ArrayList();
  
  /**
   * Creates an empty set of headers.
   */
  public InternetHeaders()
  {
  }
  
  /**
   * Parses an set of headers from an input stream.
   */
  public InternetHeaders(InputStream is)
    throws MessagingException
  {
    load(is);
  }

  /**
   * Parses the headers from an RFC822 message stream until the blank line.
   */
  public void load(InputStream is)
    throws MessagingException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds a header.
   */
  public void addHeader(String name, String value)
  {
    headers.add(new Header(name, value));
  }

  /**
   * Replaces the header with a new one.
   */
  public void setHeader(String name, String value)
  {
    removeHeader(name);
    
    headers.add(new Header(name, value));
  }

  /**
   * Removes the named header.
   */
  public void removeHeader(String name)
  {
    for (int i = headers.size() - 1; i >= 0; i--) {
      Header header = (Header) headers.get(i);

      if (header.getName().equalsIgnoreCase(name))
	headers.remove(i);
    }
  }

  /**
   * Returns the header as an arrays.
   */
  public String []getHeader(String name)
  {
    ArrayList list = new ArrayList();

    for (int i = 0; i < headers.size(); i++) {
      Header header = (Header) headers.get(i);

      if (header.getName().equals(name))
	list.add(header.getValue());
    }

    return (String []) list.toArray(new String[list.size()]);
  }

  /**
   * Returns the headers.
   */
  public Enumeration getAllHeaders()
  {
    Vector list = new Vector();

    for (int i = 0; i < headers.size(); i++) {
      Header header = (Header) headers.get(i);

      list.add(header);
    }

    return list.elements();
  }

  /**
   * Add an RFC822 header line to the header store. If the line starts
   * with a space or tab (a continuation line), add it to the last
   * header line in the list. Otherwise, append the new header line to
   * the list.  Note that RFC822 headers can only contain US-ASCII
   * characters
   */
  public void addHeaderLine(String line)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the matching headers.
   */
  public Enumeration getMatchingHeaders(String []names)
  {
    Vector list = new Vector();

    for (int i = 0; i < headers.size(); i++) {
      Header header = (Header) headers.get(i);

      if (isNameInArray(header.getName(), names)) {
	list.add(header);
      }
    }

    return list.elements();
  }

  /**
   * Returns the non-matching headers.
   */
  public Enumeration getNonMatchingHeaders(String []names)
  {
    Vector list = new Vector();

    for (int i = 0; i < headers.size(); i++) {
      Header header = (Header) headers.get(i);

      if (! isNameInArray(header.getName(), names)) {
	list.add(header);
      }
    }

    return list.elements();
  }

  /**
   * Returns the headers as string lines.
   */
  public Enumeration getAllHeaderLines()
  {
    Vector list = new Vector();

    for (int i = 0; i < headers.size(); i++) {
      Header header = (Header) headers.get(i);

      String line = header.getName() + ": " + header.getValue();

      list.add(line);
    }

    return list.elements();
  }

  /**
   * Returns the matching headers as string lines.
   */
  public Enumeration getMatchingHeaderLines(String []names)
  {
    Vector list = new Vector();

    for (int i = 0; i < headers.size(); i++) {
      Header header = (Header) headers.get(i);

      if (isNameInArray(header.getName(), names)) {
	String line = header.getName() + ": " + header.getValue();

	list.add(line);
      }
    }

    return list.elements();
  }

  /**
   * Returns the non-matching headers as string lines.
   */
  public Enumeration getNonMatchingHeaderLines(String []names)
  {
    Vector list = new Vector();

    for (int i = 0; i < headers.size(); i++) {
      Header header = (Header) headers.get(i);

      if (! isNameInArray(header.getName(), names)) {
	String line = header.getName() + ": " + header.getValue();

	list.add(line);
      }
    }

    return list.elements();
  }

  private boolean isNameInArray(String name, String []names)
  {
    for (int i = 0; i < names.length; i++) {
      if (names.equals(name))
	return true;
    }

    return false;
  }

  /**
   * An individual internet header. This class is only used by
   * subclasses of InternetHeaders.  An InternetHeader object with a
   * null value is used as a placeholder for headers of that name, to
   * preserve the order of headers. A placeholder InternetHeader
   * object with a name of ":" marks the location in the list of
   * headers where new headers are added by default.  Since: JavaMail
   * 1.4
   */
  protected static final class InternetHeader extends Header
  {
    /**
     * Constructor that takes a line and splits out the header name.
     */
    public InternetHeader(String l)
    {
      super(null,null);
      throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Constructor that takes a header name and value.
     */
    public InternetHeader(String n, String v)
    {
      super(null,null);
      throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Return the "value" part of the header line.
     */
    public String getValue()
    {
      throw new UnsupportedOperationException("not implemented");
    }

  }

}
