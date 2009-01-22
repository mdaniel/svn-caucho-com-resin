/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.bam;

import java.io.Serializable;
import java.util.HashMap;

/**
 * BamError encapsulates error responses
 *
 * The errors are defined in RFC-3920, XMPP
 */
public class BamError implements Serializable {
  /**
   * Retry after providing credentials
   */
  public static final String TYPE_AUTH = "auth";
  
  /**
   * Do not retry, i.e. unrecoverable
   */
  public static final String TYPE_CANCEL = "cancel";

  /**
   * proceed, i.e. this was a warning
   */
  public static final String TYPE_CONTINUE = "continue";

  /**
   * change the request data and try again
   */
  public static final String TYPE_MODIFY = "modify";

  /**
   * Retry after waiting
   */
  public static final String TYPE_WAIT = "wait";

  // error groups urn:ietf:params:xml:ns:xmpp-streams

  /*
  public static final String BAD_FORMAT = "bad-format";
  public static final String BAD_NAMESPACE_PREFIX = "bad-namespace-prefix";
  public static final String CONFLICT = "conflict";
  */

  /**
   * Error if the connection to the server failed.
   */
  public static final String CONNECTION_TIMEOUT = "connection-timeout";

  /*
  public static final String HOST_GONE = "host-gone";
  public static final String HOST_UNKNOWN = "host-unknown";
  public static final String IMPROPER_ADDRESSING = "improper-addressing";
  public static final String INTERNAL_SERVER_ERROR = "internal-server-error";
  public static final String INVALID_FROM = "invalid-from";
  public static final String INVALID_ID = "invalid-id";
  public static final String INVALID_NAMESPACE = "invalid-namespace";
  public static final String INVALID_XML = "invalid-xml";
  public static final String NOT_AUTHORIZED = "not-authorized";
  public static final String POLICY_VIOLATION = "policy-violation";
  */

  /**
   * Error if the connection to the remote connection failed.
   */
  public static final String REMOTE_CONNECTION_FAILED
    = "remote-connection-failed";
  
  /*
  public static final String RESOURCE_CONSTRAINT = "resource-constraint";
  public static final String RESTRICTED_XML = "restricted-xml";
  public static final String SEE_OTHER_HOST = "see-other-host";
  public static final String SYSTEM_SHUTDOWN = "system-shutdown";
  public static final String UNDEFINED_CONDITION = "undefined-condition";
  public static final String UNSUPPORTED_ENCODING = "unsupported-encoding";
  public static final String UNSUPPORTED_STANZA_TYPE
    = "unsupported-stanza-type";
  public static final String UNSUPPORTED_VERSION = "unsupported-version";
  public static final String XML_NOT_WELL_FORMED = "xml-not-well-formed";
  */

  // stanza error urn:ietf:params:xml:ns:xmpp-stanzas

  /**
   * Malformed or unprocessable request, e.g. bad type.  ("cancel")
   */
  public static final String BAD_REQUEST = "bad-request";
  
  /**
   * An existing resource or session already exists.  ("cancel")
   */
  public static final String CONFLICT = "conflict";
  
  /**
   * The feature is nor implemented, e.g. bad query class.  ("cancel")
   */
  public static final String FEATURE_NOT_IMPLEMENTED
    = "feature-not-implemented";
  
  /**
   * The requestor does not have proper authorization.  ("auth")
   */
  public static final String FORBIDDEN = "forbidden";
  
  /**
   * The target or server is no longer at this address.  ("modify")
   */
  public static final String GONE = "gone"; // xs:string
  
  /**
   * An internal server error ("wait")
   */
  public static final String INTERNAL_SERVER_ERROR = "internal-server-error";
  
  /**
   * The target jid doesn't exist ("cancel")
   */
  public static final String ITEM_NOT_FOUND = "item-not-found";
  
  /**
   * The target jid syntax is invalid ("modify")
   */
  public static final String JID_MALFORMED = "jid-malformed";
  
  /**
   * The request is forbidden because of application policy, i.e. it's
   * a valid request, but not allowed ("modify")
   */
  public static final String NOT_ACCEPTABLE = "not-acceptable";
  
  /**
   * The server does not any entity to perform the action ("cancel")
   */
  public static final String NOT_ALLOWED = "not-allowed";

  /**
   * The entity does not have the proper credentials ("auth")
   */
  public static final String NOT_AUTHORIZED = "not-authorized";

  /**
   * The entity does not have payment to the service ("auth")
   */
  public static final String PAYMENT_REQUIRED = "payment-required";

  /**
   * The recipient exists, but is currently not attached ("wait")
   */
  public static final String RECIPIENT_UNAVAILABLE = "recipient-unavailable";

  /**
   * The recipient is at a new address ("modify")
   */
  public static final String REDIRECT = "redirect"; // xs:string

  /**
   * The entity does not have proper registration for the service ("auth")
   */
  public static final String REGISTRATION_REQUIRED = "registration-required";

  /**
   * The remote server does not exist ("cancel")
   */
  public static final String REMOTE_SERVER_NOT_FOUND
    = "remote-server-not-found";

  /**
   * The remote server could not be contacted ("wait")
   */
  public static final String REMOTE_SERVER_TIMEOUT = "remote-server-timeout";

  /**
   * The remote service does not have resources to process the request ("wait")
   */
  public static final String RESOURCE_CONSTRAINT = "resource-constraint";

  /**
   * The remote server does not provide the service ("cancel")
   */
  public static final String SERVICE_UNAVAILABLE = "service-unavailable";

  /**
   * The resource required a subscription before use ("auth")
   */
  public static final String SUBSCRIPTION_REQUIRED = "subscription-required";

  /**
   * An error outside this list (should have an _extra field)
   */
  public static final String UNDEFINED_CONDITION = "undefined-condition";

  /**
   * The request was syntactically correct, but out-of-order ("wait")
   */
  public static final String UNEXPECTED_REQUEST = "unexpected-request";

  private static final HashMap<String,ErrorGroup> _errorMap
    = new HashMap<String,ErrorGroup>();
  
  private final String _type;
  private final String _group;
  private final String _text;
  
  private Serializable _data;
  private Serializable _extra;
  
  /**
   * zero-arg constructor for Hessian
   */
  private BamError()
  {
    _type = null;
    _group = null;
    _text = null;
  }

  /**
   * Misc error
   *
   * @param text an error text
   */
  public BamError(String text)
  {
    _type = TYPE_CANCEL;
    _group = INTERNAL_SERVER_ERROR;
    _text = text;
  }

  /**
   * An error response
   *
   * @param type the error type
   * @param group the error group
   */
  public BamError(String type,
		  String group)
  {
    _type = type;
    _group = group;
    _text = null;
  }

  /**
   * An error response
   *
   * @param type the error type
   * @param group the error group
   * @param text the error text
   */
  public BamError(String type,
		   String group,
		   String text)
  {
    _type = type;
    _group = group;
    _text = text;
  }

  /**
   * Returns the error type
   */
  public String getType()
  {
    return _type;
  }

  /**
   * Returns the error group
   */
  public String getGroup()
  {
    return _group;
  }

  /**
   * Returns the error text
   */
  public String getText()
  {
    return _text;
  }

  /**
   * Returns any data for the error
   */
  public Serializable getData()
  {
    return _data;
  }

  /**
   * Sets any data for the error
   */
  public void setData(Serializable data)
  {
    _data = data;
  }

  /**
   * Extra information for UNDEFINED_CONDITION
   */
  public Serializable getExtra()
  {
    return _extra;
  }

  /**
   * Extra information for UNDEFINED_CONDITION
   */
  public void setExtra(Serializable extra)
  {
    _extra = extra;
  }

  public BamErrorPacketException createException()
  {
    ErrorGroup group = _errorMap.get(getGroup());

    if (group == null)
      return new BamErrorPacketException(this);

    switch (group) {
    case FEATURE_NOT_IMPLEMENTED:
      return new BamFeatureNotImplementedException(this);
      
    case NOT_AUTHORIZED:
      return new BamNotAuthorizedException(this);
      
    case REMOTE_CONNECTION_FAILED:
      return new BamRemoteConnectionFailedException(this);
      
    case SERVICE_UNAVAILABLE:
      return new BamServiceUnavailableException(this);

    default:
      return new BamErrorPacketException(this);
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[type=");
    sb.append(_type);
    sb.append(",group=");
    sb.append(_group);

    if (_data != null) {
      sb.append(",data=");
      sb.append(_data);
    }
    
    if (_text != null) {
      sb.append(",text=");
      sb.append(_text);
    }

    sb.append("]");

    return sb.toString();
  }

  public enum ErrorGroup {
    BAD_FORMAT,
      BAD_NAMESPACE_PREFIX,
      CONFLICT,
      CONNECTION_TIMEOUT,
      HOST_GONE,
      HOST_UNKNOWN,
      IMPROPER_ADDRESSING,
      INTERNAL_SERVER_ERROR,
      INVALID_FROM,
      INVALID_ID,
      INVALID_NAMESPACE,
      INVALID_XML,
      NOT_AUTHORIZED,
      POLICY_VIOLATION,
      REMOTE_CONNECTION_FAILED,
      RESOURCE_CONSTRAINT,
      RESTRICTED_XML,
      SEE_OTHER_HOST,
      SYSTEM_SHUTDOWN,
      UNDEFINED_CONDITION,
      UNSUPPORTED_ENCODING,
      UNSUPPORTED_STANZA_TYPE,
      UNSUPPORTED_VERSION,
      XML_NOT_WELL_FORMED,

      FEATURE_NOT_IMPLEMENTED,
      SERVICE_UNAVAILABLE,
  }

  static {
    _errorMap.put(FEATURE_NOT_IMPLEMENTED,
		  ErrorGroup.FEATURE_NOT_IMPLEMENTED);
    _errorMap.put(NOT_AUTHORIZED,
		  ErrorGroup.NOT_AUTHORIZED);
    _errorMap.put(REMOTE_CONNECTION_FAILED,
		  ErrorGroup.REMOTE_CONNECTION_FAILED);
    _errorMap.put(SERVICE_UNAVAILABLE,
		  ErrorGroup.SERVICE_UNAVAILABLE);
  }
}
