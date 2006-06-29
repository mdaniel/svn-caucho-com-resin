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

/**
 * The definition of constants pertaining to the SOAP protocol.
 */
public interface SOAPConstants {

  /**
   * The default protocol: SOAP 1.1 for backwards compatibility. Since: SAAJ
   * 1.3 See Also:Constant Field Values
   */
  static final String DEFAULT_SOAP_PROTOCOL="SOAP 1.1 Protocol";


  /**
   * Used to create MessageFactory instances that create SOAPMessages whose
   * concrete type is based on the Content-Type MIME header passed to the
   * createMessage method. If no Content-Type header is passed then the
   * createMessage may throw an IllegalArgumentException or, in the case of the
   * no argument version of createMessage, an UnsupportedOperationException.
   * Since: SAAJ 1.3 See Also:Constant Field Values
   */
  static final String DYNAMIC_SOAP_PROTOCOL="Dynamic Protocol";


  /**
   * The media type of the Content-Type MIME header in SOAP 1.1. Since: SAAJ
   * 1.3 See Also:Constant Field Values
   */
  static final String SOAP_1_1_CONTENT_TYPE="text/xml";


  /**
   * Used to create MessageFactory instances that create SOAPMessages whose
   * behavior supports the SOAP 1.1 specification. Since: SAAJ 1.3 See
   * Also:Constant Field Values
   */
  static final String SOAP_1_1_PROTOCOL="SOAP 1.1 Protocol";


  /**
   * The media type of the Content-Type MIME header in SOAP 1.2. Since: SAAJ
   * 1.3 See Also:Constant Field Values
   */
  static final String SOAP_1_2_CONTENT_TYPE="application/soap+xml";


  /**
   * Used to create MessageFactory instances that create SOAPMessages whose
   * behavior supports the SOAP 1.2 specification Since: SAAJ 1.3 See
   * Also:Constant Field Values
   */
  static final String SOAP_1_2_PROTOCOL="SOAP 1.2 Protocol";


  /**
   * SOAP 1.2 DataEncodingUnknown Fault Since: SAAJ 1.3
   */
  static final QName SOAP_DATAENCODINGUNKNOWN_FAULT=null;


  /**
   * The default namespace prefix for http://www.w3.org/2003/05/soap-envelope
   * Since: SAAJ 1.3 See Also:Constant Field Values
   */
  static final String SOAP_ENV_PREFIX="env";


  /**
   * SOAP 1.2 MustUnderstand Fault Since: SAAJ 1.3
   */
  static final QName SOAP_MUSTUNDERSTAND_FAULT=null;


  /**
   * SOAP 1.2 Receiver Fault Since: SAAJ 1.3
   */
  static final QName SOAP_RECEIVER_FAULT=null;


  /**
   * SOAP 1.2 Sender Fault Since: SAAJ 1.3
   */
  static final QName SOAP_SENDER_FAULT=null;


  /**
   * SOAP 1.2 VersionMismatch Fault Since: SAAJ 1.3
   */
  static final QName SOAP_VERSIONMISMATCH_FAULT=null;


  /**
   * The namespace identifier for the SOAP 1.1 envelope. Since: SAAJ 1.3 See
   * Also:Constant Field Values
   */
  static final String URI_NS_SOAP_1_1_ENVELOPE="http://schemas.xmlsoap.org/soap/envelope/";


  /**
   * The namespace identifier for the SOAP 1.2 encoding. Since: SAAJ 1.3 See
   * Also:Constant Field Values
   */
  static final String URI_NS_SOAP_1_2_ENCODING="http://www.w3.org/2003/05/soap-encoding";


  /**
   * The namespace identifier for the SOAP 1.2 envelope. Since: SAAJ 1.3 See
   * Also:Constant Field Values
   */
  static final String URI_NS_SOAP_1_2_ENVELOPE="http://www.w3.org/2003/05/soap-envelope";


  /**
   * The namespace identifier for the SOAP 1.1 encoding. An attribute named
   * encodingStyle in the URI_NS_SOAP_ENVELOPE namespace and set to the value
   * URI_NS_SOAP_ENCODING can be added to an element to indicate that it is
   * encoded using the rules in section 5 of the SOAP 1.1 specification. See
   * Also:Constant Field Values
   */
  static final String URI_NS_SOAP_ENCODING="http://schemas.xmlsoap.org/soap/encoding/";


  /**
   * The namespace identifier for the SOAP 1.1 envelope, All SOAPElements in
   * this namespace are defined by the SOAP 1.1 specification. See
   * Also:Constant Field Values
   */
  static final String URI_NS_SOAP_ENVELOPE="http://schemas.xmlsoap.org/soap/envelope/";


  /**
   * The URI identifying the next application processing a SOAP request as the
   * intended role for a SOAP 1.2 header entry (see section 2.2 of part 1 of
   * the SOAP 1.2 specification). Since: SAAJ 1.3 See Also:Constant Field Values
   */
  static final String URI_SOAP_1_2_ROLE_NEXT="http://www.w3.org/2003/05/soap-envelope/role/next";


  /**
   * The URI specifying the role None in SOAP 1.2. Since: SAAJ 1.3 See
   * Also:Constant Field Values
   */
  static final String URI_SOAP_1_2_ROLE_NONE="http://www.w3.org/2003/05/soap-envelope/role/none";


  /**
   * The URI identifying the ultimate receiver of the SOAP 1.2 message. Since:
   * SAAJ 1.3 See Also:Constant Field Values
   */
  static final String URI_SOAP_1_2_ROLE_ULTIMATE_RECEIVER="http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver";


  /**
   * The URI identifying the next application processing a SOAP request as the
   * intended actor for a SOAP 1.1 header entry (see section 4.2.2 of the SOAP
   * 1.1 specification). This value can be passed to
   * SOAPHeader.examineMustUnderstandHeaderElements(String),
   * SOAPHeader.examineHeaderElements(String) and
   * SOAPHeader.extractHeaderElements(String) See Also:Constant Field Values
   */
  static final String URI_SOAP_ACTOR_NEXT="http://schemas.xmlsoap.org/soap/actor/next";

}

