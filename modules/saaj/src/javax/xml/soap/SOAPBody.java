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

import java.util.Locale;

import org.w3c.dom.Document;

/**
 * Implements the SOAP body.
 */
public interface SOAPBody extends SOAPElement {
  /**
   * Adds a fault.
   */
  public SOAPFault addFault()
    throws SOAPException;
  
  /**
   * Adds a fault.
   */
  public SOAPFault addFault(Name faultCode,
			    String faultString,
			    Locale locale)
    throws SOAPException;
  
  /**
   * Adds a fault.
   */
  public SOAPFault addFault(Name faultCode,
			    String faultString)
    throws SOAPException;

  /**
   * Returns true if a fault exists.
   */
  public boolean hasFault();

  /**
   * Returns the fault.
   */
  public SOAPFault getFault();

  /**
   * Adds a body element.
   */
  public SOAPBodyElement addBodyElement(Name name)
    throws SOAPException;

  /**
   * Adds the document
   */
  public SOAPBodyElement addDocument(Document doc)
    throws SOAPException;
}
