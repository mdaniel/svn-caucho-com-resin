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

package javax.xml.bind;

/**
 * This event indicates that a problem was encountered while validating the
 * incoming XML data during an unmarshal operation, while performing on-demand
 * validation of the Java content tree, or while marshalling the Java content
 * tree back to XML data. Since: JAXB1.0 Version: $Revision: 1.1 $ Author: Ryan
 * Shoemaker, Sun Microsystems, Inc.Kohsuke Kawaguchi, Sun Microsystems,
 * Inc.Joe Fialli, Sun Microsystems, Inc. See Also:Validator,
 * ValidationEventHandler
 */
public interface ValidationEvent {

  /**
   * Conditions that correspond to the definition of "error" in section 1.2 of
   * the W3C XML 1.0 Recommendation See Also:Constant Field Values
   */
  static final int ERROR=1;


  /**
   * Conditions that correspond to the definition of "fatal error" in section
   * 1.2 of the W3C XML 1.0 Recommendation See Also:Constant Field Values
   */
  static final int FATAL_ERROR=2;


  /**
   * Conditions that are not errors or fatal errors as defined by the XML 1.0
   * recommendation See Also:Constant Field Values
   */
  static final int WARNING=0;


  /**
   * Retrieve the linked exception for this warning/error.
   */
  abstract Throwable getLinkedException();


  /**
   * Retrieve the locator for this warning/error.
   */
  abstract ValidationEventLocator getLocator();


  /**
   * Retrieve the text message for this warning/error.
   */
  abstract String getMessage();


  /**
   * Retrieve the severity code for this warning/error. Must be one of
   * ValidationError.WARNING, ValidationError.ERROR, or
   * ValidationError.FATAL_ERROR.
   */
  abstract int getSeverity();

}

