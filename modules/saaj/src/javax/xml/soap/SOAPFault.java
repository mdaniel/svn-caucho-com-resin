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

package javax.xml.soap;
import javax.xml.namespace.*;
import java.util.*;

/**
 * An element in the SOAPBody object that contains error and/or status
 * information. This information may relate to errors in the SOAPMessage object
 * or to problems that are not related to the content in the message itself.
 * Problems not related to the message itself are generally errors in
 * processing, such as the inability to communicate with an upstream server.
 * Depending on the protocol specified while creating the MessageFactory
 * instance, a SOAPFault has sub-elements as defined in the SOAP 1.1/SOAP 1.2
 * specification.
 */
public interface SOAPFault extends SOAPBodyElement {

  /**
   * Creates an optional Detail object and sets it as the Detail object for
   * this SOAPFault object. It is illegal to add a detail when the fault
   * already contains a detail. Therefore, this method should be called only
   * after the existing detail has been removed.
   */
  abstract Detail addDetail() throws SOAPException;


  /**
   * Appends or replaces a Reason Text item containing the specified text
   * message and an derived from locale. If a Reason Text item with this
   * already exists its text value will be replaced with text. The locale
   * parameter should not be null Code sample: SOAPFault fault = ...;
   * fault.addFaultReasonText("Version Mismatch", Locale.ENGLISH);
   */
  abstract void addFaultReasonText(String text, Locale locale) throws SOAPException;


  /**
   * Adds a Subcode to the end of the sequence of Subcodes contained by this
   * SOAPFault. Subcodes, which were introduced in SOAP 1.2, are represented by
   * a recursive sequence of subelements rooted in the mandatory Code
   * subelement of a SOAP Fault.
   */
  abstract void appendFaultSubcode(QName subcode) throws SOAPException;


  /**
   * Returns the optional detail element for this SOAPFault object. A Detail
   * object carries application-specific error information, the scope of the
   * error information is restricted to faults in the SOAPBodyElement objects
   * if this is a SOAP 1.1 Fault.
   */
  abstract Detail getDetail();


  /**
   * Gets the fault actor for this SOAPFault object. If this SOAPFault supports
   * SOAP 1.2 then this call is equivalent to getFaultRole()
   */
  abstract String getFaultActor();


  /**
   * Gets the fault code for this SOAPFault object.
   */
  abstract String getFaultCode();


  /**
   * Gets the mandatory SOAP 1.1 fault code for this SOAPFault object as a SAAJ
   * Name object. The SOAP 1.1 specification requires the value of the
   * "faultcode" element to be of type QName. This method returns the content
   * of the element as a QName in the form of a SAAJ Name object. This method
   * should be used instead of the getFaultCode method since it allows
   * applications to easily access the namespace name without additional
   * parsing.
   */
  abstract Name getFaultCodeAsName();


  /**
   * Gets the fault code for this SOAPFault object as a QName object.
   */
  abstract QName getFaultCodeAsQName();


  /**
   * Returns the optional Node element value for this SOAPFault object. The
   * Node element is optional in SOAP 1.2.
   */
  abstract String getFaultNode();


  /**
   * Returns an Iterator over a distinct sequence of Locales for which there
   * are associated Reason Text items. Any of these Locales can be used in a
   * call to getFaultReasonText in order to obtain a localized version of the
   * Reason Text string.
   */
  abstract Iterator getFaultReasonLocales() throws SOAPException;


  /**
   * Returns the Reason Text associated with the given Locale. If more than one
   * such Reason Text exists the first matching Text is returned
   */
  abstract String getFaultReasonText(Locale locale) throws SOAPException;


  /**
   * Returns an Iterator over a sequence of String objects containing all of
   * the Reason Text items for this SOAPFault.
   */
  abstract Iterator getFaultReasonTexts() throws SOAPException;


  /**
   * Returns the optional Role element value for this SOAPFault object. The
   * Role element is optional in SOAP 1.2.
   */
  abstract String getFaultRole();


  /**
   * Gets the fault string for this SOAPFault object. If this SOAPFault is part
   * of a message that supports SOAP 1.2 then this call is equivalent to:
   * String reason = null; try { reason = (String)
   * getFaultReasonTexts().next(); } catch (SOAPException e) {} return reason;
   */
  abstract String getFaultString();


  /**
   * Gets the locale of the fault string for this SOAPFault object. If this
   * SOAPFault is part of a message that supports SOAP 1.2 then this call is
   * equivalent to: Locale locale = null; try { locale = (Locale)
   * getFaultReasonLocales().next(); } catch (SOAPException e) {} return locale;
   */
  abstract Locale getFaultStringLocale();


  /**
   * Gets the Subcodes for this SOAPFault as an iterator over QNames.
   */
  abstract Iterator getFaultSubcodes();


  /**
   * Returns true if this SOAPFault has a Detail subelement and false
   * otherwise. Equivalent to (getDetail()!=null).
   */
  abstract boolean hasDetail();


  /**
   * Removes any Subcodes that may be contained by this SOAPFault. Subsequent
   * calls to getFaultSubcodes will return an empty iterator until a call to
   * appendFaultSubcode is made.
   */
  abstract void removeAllFaultSubcodes();


  /**
   * Sets this SOAPFault object with the given fault actor. The fault actor is
   * the recipient in the message path who caused the fault to happen. If this
   * SOAPFault supports SOAP 1.2 then this call is equivalent to
   * setFaultRole(String)
   */
  abstract void setFaultActor(String faultActor) throws SOAPException;


  /**
   * Sets this SOAPFault object with the given fault code. Fault codes, which
   * give information about the fault, are defined in the SOAP 1.1
   * specification. A fault code is mandatory and must be of type Name. This
   * method provides a convenient way to set a fault code. For example,
   * SOAPEnvelope se = ...; // Create a qualified name in the SOAP namespace
   * with a localName // of "Client". Note that prefix parameter is optional
   * and is null // here which causes the implementation to use an appropriate
   * prefix. Name qname = se.createName("Client", null,
   * SOAPConstants.URI_NS_SOAP_ENVELOPE); SOAPFault fault = ...;
   * fault.setFaultCode(qname); It is preferable to use this method over
   * setFaultCode(String).
   */
  abstract void setFaultCode(Name faultCodeQName) throws SOAPException;


  /**
   * Sets this SOAPFault object with the given fault code. It is preferable to
   * use this method over .
   */
  abstract void setFaultCode(QName faultCodeQName) throws SOAPException;


  /**
   * Sets this SOAPFault object with the give fault code. Fault codes, which
   * given information about the fault, are defined in the SOAP 1.1
   * specification. This element is mandatory in SOAP 1.1. Because the fault
   * code is required to be a QName it is preferable to use the
   * setFaultCode(Name) form of this method.
   */
  abstract void setFaultCode(String faultCode) throws SOAPException;


  /**
   * Creates or replaces any existing Node element value for this SOAPFault
   * object. The Node element is optional in SOAP 1.2.
   */
  abstract void setFaultNode(String uri) throws SOAPException;


  /**
   * Creates or replaces any existing Role element value for this SOAPFault
   * object. The Role element is optional in SOAP 1.2.
   */
  abstract void setFaultRole(String uri) throws SOAPException;


  /**
   * Sets the fault string for this SOAPFault object to the given string. If
   * this SOAPFault is part of a message that supports SOAP 1.2 then this call
   * is equivalent to: addFaultReasonText(faultString, Locale.getDefault());
   */
  abstract void setFaultString(String faultString) throws SOAPException;


  /**
   * Sets the fault string for this SOAPFault object to the given string and
   * localized to the given locale. If this SOAPFault is part of a message that
   * supports SOAP 1.2 then this call is equivalent to:
   * addFaultReasonText(faultString, locale);
   */
  abstract void setFaultString(String faultString, Locale locale) throws SOAPException;

}

