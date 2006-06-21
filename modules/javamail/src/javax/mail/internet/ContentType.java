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
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.util.*;
import java.util.logging.*;

/**
 * This class represents a MIME ContentType value.
 */
public class ContentType {

  private MimeType _mimeType;

  private static Logger log =
    Logger.getLogger("javax.mail.internet.ContentType");

  public ContentType()
  {
    // RFC 2024 default
    try {
      _mimeType = new MimeType("text", "plain");
      _mimeType.setParameter("charset", "us-ascii");
    }
    catch (MimeTypeParseException e) {
      throw new RuntimeException(e);
    }
  }

  public ContentType(String s) throws ParseException
  {
    try {
      _mimeType = new MimeType(s);
    }
    catch (MimeTypeParseException e) {
      throw new ParseException(e.getMessage());
    }
  }

  /**
   * Constructor.
   * primaryType - primary typesubType - subTypelist - ParameterList
   */
  public ContentType(String primaryType, String subType, ParameterList list)
  {
    try {
      _mimeType = new MimeType(primaryType, subType);
    }
    catch (MimeTypeParseException e) {
      throw new RuntimeException(e);
    }

    setParameterList(list);
  }

  /**
   * Return the MIME type string, without the parameters.
   */
  public String getBaseType()
  {
    return _mimeType.getBaseType();
  }

  /**
   * Return the specified parameter value.
   */
  public String getParameter(String name)
  {
    return _mimeType.getParameter(name);
  }

  /**
   * Return a ParameterList object that holds all the available
   * parameters.
   *  // XXX: does calling set() on this impact the parent ContentType?
   */
  public ParameterList getParameterList()
  {
    try {
      return new ParameterList(_mimeType.getParameters());
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the primary type.
   */
  public String getPrimaryType()
  {
    return _mimeType.getPrimaryType();
  }

  /**
   * Return the subType.
   */
  public String getSubType()
  {
    return _mimeType.getSubType();
  }

  /**
   * Match with the specified ContentType object, ingoring parameters
   */
  public boolean match(ContentType cType)
  {
    try {
      return _mimeType.match(cType._mimeType);
    }
    catch (MimeTypeParseException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean match(String s)
  {
    try {
      return match(new ContentType(s));
    }
    catch (ParseException e) {
      log.log(Level.FINER, "ignoring exception", e);
      return false;
    }
  }

  public void setParameter(String name, String value)
  {
    _mimeType.setParameter(name, value);
  }

  public void setParameterList(ParameterList list)
  {
    for(Enumeration enu = list.getNames();
	enu.hasMoreElements();) {

      String key = (String)enu.nextElement();
      String val = list.get(key);

      _mimeType.setParameter(key, val);
    }
  }

  public void setPrimaryType(String primaryType)
  {
    try {
      _mimeType.setPrimaryType(primaryType);
    }
    catch (MimeTypeParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set the subType. Replaces the existing subType.
   */
  public void setSubType(String subType)
  {
    try {
      _mimeType.setSubType(subType);
    }
    catch (MimeTypeParseException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Retrieve a RFC2045 style string representation of this
   * Content-Type. Returns null if the conversion failed.
   */
  public String toString()
  {
    return _mimeType.toString();
  }

}
