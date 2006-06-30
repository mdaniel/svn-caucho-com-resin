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
 * A basic event handler interface for validation errors. If an application
 * needs to implement customized event handling, it must implement this
 * interface and then register it with either the Unmarshaller, the Validator,
 * or the Marshaller. The JAXB Provider will then report validation errors and
 * warnings encountered during the unmarshal, marshal, and validate operations
 * to these event handlers. If the handleEvent method throws an unchecked
 * runtime exception, the JAXB Provider must treat that as if the method
 * returned false, effectively terminating whatever operation was in progress
 * at the time (unmarshal, validate, or marshal). Modifying the Java content
 * tree within your event handler is undefined by the specification and may
 * result in unexpected behaviour. Failing to return false from the handleEvent
 * method after encountering a fatal error is undefined by the specification
 * and may result in unexpected behavior. Default Event Handler Since: JAXB1.0
 * Version: $Revision: 1.1 $ Author: Ryan Shoemaker, Sun Microsystems,
 * Inc.Kohsuke Kawaguchi, Sun Microsystems, Inc.Joe Fialli, Sun Microsystems,
 * Inc. See Also:Unmarshaller, Validator, Marshaller, ValidationEvent,
 * ValidationEventCollector
 */
public interface ValidationEventHandler {

  /**
   * Receive notification of a validation warning or error. The ValidationEvent
   * will have a embedded in it that indicates where the error or warning
   * occurred. If an unchecked runtime exception is thrown from this method,
   * the JAXB provider will treat it as if the method returned false and
   * interrupt the current unmarshal, validate, or marshal operation.
   */
  abstract boolean handleEvent(ValidationEvent event);

}

