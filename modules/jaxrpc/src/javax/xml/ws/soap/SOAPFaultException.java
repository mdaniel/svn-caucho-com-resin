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

package javax.xml.ws.soap;
import javax.xml.soap.*;
import javax.xml.ws.*;

/**
 * The SOAPFaultException exception represents a SOAP 1.1 or 1.2 fault. A
 * SOAPFaultException wraps a SAAJ SOAPFault that manages the SOAP-specific
 * representation of faults. The createFault method of
 * javax.xml.soap.SOAPFactory may be used to create an instance of
 * javax.xml.soap.SOAPFault for use with the constructor. SOAPBinding contains
 * an accessor for the SOAPFactory used by the binding instance. Note that the
 * value of getFault is the only part of the exception used when searializing a
 * SOAP fault. Refer to the SOAP specification for a complete description of
 * SOAP faults. Since: JAX-WS 2.0 See Also:SOAPFault,
 * SOAPBinding.getSOAPFactory(), ProtocolException, Serialized Form
 */
public class SOAPFaultException extends ProtocolException {

  /**
   * Constructor for SOAPFaultException Parameters:fault - SOAPFault
   * representing the faultSee Also:SOAPFactory.createFault(java.lang.String,
   * javax.xml.namespace.QName)
   */
  public SOAPFaultException(SOAPFault fault)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the embedded SOAPFault instance.
   */
  public SOAPFault getFault()
  {
    throw new UnsupportedOperationException();
  }

}

