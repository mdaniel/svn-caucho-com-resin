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
 * public interface Validator As of JAXB 2.0, this class is deprecated and
 * optional. The Validator class is responsible for controlling the validation
 * of content trees during runtime. Three Forms of Validation The Validator
 * class is responsible for managing On-Demand Validation. The Unmarshaller
 * class is responsible for managing Unmarshal-Time Validation during the
 * unmarshal operations. Although there is no formal method of enabling
 * validation during the marshal operations, the Marshaller may detect errors,
 * which will be reported to the ValidationEventHandler registered on it. Using
 * the Default EventHandler Handling Validation Events Validation and
 * Well-Formedness Validation events are handled differently depending on how
 * the client application is configured to process them as described in the
 * previous section. However, there are certain cases where a JAXB Provider
 * indicates that it is no longer able to reliably detect and report errors. In
 * these cases, the JAXB Provider will set the severity of the ValidationEvent
 * to FATAL_ERROR to indicate that the unmarshal, validate, or marshal
 * operations should be terminated. The default event handler and
 * ValidationEventCollector utility class must terminate processing after being
 * notified of a fatal error. Client applications that supply their own
 * ValidationEventHandler should also terminate processing after being notified
 * of a fatal error. If not, unexpected behaviour may occur. Supported
 * Properties There currently are not any properties required to be supported
 * by all JAXB Providers on Validator. However, some providers may support
 * their own set of provider specific properties. Since: JAXB1.0 Version:
 * $Revision: 1.4 $ $Date: 2005/07/29 20:56:02 $ Author: Ryan Shoemaker, Sun
 * Microsystems, Inc.Kohsuke Kawaguchi, Sun Microsystems, Inc.Joe Fialli, Sun
 * Microsystems, Inc. See Also:JAXBContext, Unmarshaller,
 * ValidationEventHandler, ValidationEvent, ValidationEventCollector
 */
interface Validator {

  /**
   * Deprecated. Return the current event handler or the default event handler
   * if one hasn't been set.
   */
  abstract ValidationEventHandler getEventHandler() throws JAXBException;


  /**
   * Deprecated. Get the particular property in the underlying implementation
   * of Validator. This method can only be used to get one of the standard JAXB
   * defined properties above or a provider specific property. Attempting to
   * get an undefined property will result in a PropertyException being thrown.
   * See .
   */
  abstract Object getProperty(String name) throws PropertyException;


  /**
   * Deprecated. Allow an application to register a validation event handler.
   * The validation event handler will be called by the JAXB Provider if any
   * validation errors are encountered during calls to validate. If the client
   * application does not register a validation event handler before invoking
   * the validate method, then validation events will be handled by the default
   * event handler which will terminate the validate operation after the first
   * error or fatal error is encountered. Calling this method with a null
   * parameter will cause the Validator to revert back to the default default
   * event handler.
   */
  abstract void setEventHandler(ValidationEventHandler handler) throws JAXBException;


  /**
   * Deprecated. Set the particular property in the underlying implementation
   * of Validator. This method can only be used to set one of the standard JAXB
   * defined properties above or a provider specific property. Attempting to
   * set an undefined property will result in a PropertyException being thrown.
   * See .
   */
  abstract void setProperty(String name, Object value) throws PropertyException;


  /**
   * Deprecated. Validate the Java content tree starting at subrootObj. Client
   * applications can use this method to validate Java content trees on-demand
   * at runtime. This method can be used to validate any arbitrary subtree of
   * the Java content tree. Global constraint checking will not be performed as
   * part of this operation (i.e. ID/IDREF constraints).
   */
  abstract boolean validate(Object subrootObj) throws JAXBException;


  /**
   * Deprecated. Validate the Java content tree rooted at rootObj. Client
   * applications can use this method to validate Java content trees on-demand
   * at runtime. This method is used to validate an entire Java content tree.
   * Global constraint checking will be performed as part of this operation
   * (i.e. ID/IDREF constraints).
   */
  abstract boolean validateRoot(Object rootObj) throws JAXBException;

}

