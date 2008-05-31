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

package com.caucho.jms.xmpp;

import com.caucho.hmtp.*;
import com.caucho.hmtp.im.*;
import com.caucho.hmtp.spi.*;
import com.caucho.server.connection.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.stream.*;
import java.io.*;
import java.util.logging.*;
import javax.servlet.*;
import javax.xml.stream.*;

/**
 * Protocol handler from the TCP/XMPP stream forwarding to the broker
 */
public class XmppBrokerStream
  implements TcpDuplexHandler, HmtpStream
{
  private static final Logger log
    = Logger.getLogger(XmppBrokerStream.class.getName());
  
  private HmtpBroker _broker;
  private HmtpConnection _conn;
  private HmtpStream _toBroker;

  private HmtpStream _callbackHandler;
  private HmtpStream _authHandler;

  private ReadStream _is;
  private WriteStream _os;
  
  private XMLStreamReaderImpl _in;

  private String _jid;
  private long _requestId;

  private String _name;
  private boolean _isFinest;

  XmppBrokerStream(HmtpBroker broker,
		   XMLStreamReaderImpl in, WriteStream os)
  {
    _broker = broker;

    _in = in;
    _os = os;

    _callbackHandler = null;//new ServerAgentStream(this, _out);
    _authHandler = null;//new AuthBrokerStream(this, _callbackHandler);

    _isFinest = log.isLoggable(Level.FINEST);

    System.out.println("START-ME-UP:");

    login("test", "dummy", null);
  }

  protected String getJid()
  {
    return _jid;
  }
  
  public boolean serviceRead(ReadStream is,
			     TcpDuplexController controller)
    throws IOException
  {
    XMLStreamReaderImpl in = _in;
    
    System.out.println("READs for an ex-leper");

    if (in == null)
      return false;

    try {
      int tag;
      
      while ((tag = _in.next()) > 0) {
	if (_isFinest)
	  debug(_in);
	
	if (tag == XMLStreamConstants.END_ELEMENT) {
	  if ("stream".equals(_in.getLocalName())) {
	    if (log.isLoggable(Level.FINE))
	      log.fine(this + " end-stream");
	  }
	  else {
	    log.warning(this + " " + _in.getLocalName());
	  }
	}

	if (tag == XMLStreamConstants.START_ELEMENT) {
	  boolean valid = false;
	  
	  if ("auth".equals(_in.getLocalName()))
	    valid = handleAuth();
	  else if ("stream".equals(_in.getLocalName()))
	    valid = handleStream();
	  else if ("iq".equals(_in.getLocalName()))
	    valid = handleIq();
	  else if ("presence".equals(_in.getLocalName()))
	    valid = handlePresence();
	  else if ("message".equals(_in.getLocalName()))
	    valid = handleMessage();
	  else {
	    return false;
	  }

	  if (! valid)
	    return false;

	  if (_in.available() < 1)
	    return true;
	}
      }

      if (_isFinest)
	log.finest(this + " end of stream");

      return false;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return false;
    }
  }
  
  public boolean serviceWrite(WriteStream os,
			      TcpDuplexController controller)
    throws IOException
  {
    return false;
  }

  String login(String uid, Serializable credentials, String resource)
  {
    String password = (String) credentials;
    
    _conn = _broker.getConnection(uid, password);
    _conn.setMessageHandler(_callbackHandler);
    _conn.setQueryHandler(_callbackHandler);
    _conn.setPresenceHandler(_callbackHandler);

    _jid = _conn.getJid();
    
    _toBroker = _conn.getBrokerStream();

    return _jid;
  }

  private boolean handleStream()
    throws IOException, XMLStreamException
  {
    String name = _in.getLocalName();

    String to = null;
      
    for (int i = _in.getAttributeCount() - 1; i >= 0; i--) {
      String localName = _in.getAttributeLocalName(i);
      String value = _in.getAttributeValue(i);

      if ("to".equals(localName))
	to = value;
    }

    String from = "localhost";
    
    /**
    if (from == null)
      from = to;

    if (from == null)
      from = _broker.getJid();
    */

    long id = _requestId++;
    
    if (log.isLoggable(Level.FINE))
      log.fine(this + " stream open(from=" + from + " id=" + id + ")");
      
    _os.print("<stream:stream xmlns='jabber:client'");
    _os.print(" xmlns:stream='http://etherx.jabber.org/streams'");
    _os.print(" id='" + id + "'");
    _os.print(" from='" + from + "'");
    _os.print(" version='1.0'>");
      
    _os.print("<stream:features>");
    _os.print("<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'/>");
    _os.print("<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>");
    _os.print("</stream:features>");
    _os.flush();

    return true;
  }

  private boolean handleIq()
    throws IOException, XMLStreamException
  {
    String type = _in.getAttributeValue(null, "type");
    String id = _in.getAttributeValue(null, "id");
    String from = _in.getAttributeValue(null, "from");
    String to = _in.getAttributeValue(null, "to");

    int tag = _in.nextTag();

    if (_isFinest)
      debug(_in);

    String localName = _in.getLocalName();
    String uri = _in.getNamespaceURI();

    if ("bind".equals(_in.getLocalName())) {
      tag = _in.nextTag();

      String resource = null;

      if ("resource".equals(_in.getLocalName())) {
	_in.next();
	
	resource = _in.getText();
      }
      
      skipToEnd("iq");

      // _clientBind = "test@" + _streamFrom + "/" + resource;

      if (log.isLoggable(Level.FINE)) {
	log.fine(this + " bind-result(jid=" + _jid
		 + " id=" + id + " to=" + to + " from=" + from + ")");
      }
	
      _os.print("<iq type='result' id='" + id + "' to='" + _jid + "'>");
      _os.print("<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>");
      _os.print("<jid>" + _jid + "</jid>");
      _os.print("</bind></iq>\n");
      _os.flush();

      return true;
    }
    else if ("session".equals(_in.getLocalName())) {
      skipToEnd("iq");
      
      _os.print("<iq type='result' id='" + id + "' from='" + _jid + "'/>");
      _os.flush();

      return true;
    }
    else if ("jabber:iq:roster".equals(_in.getNamespaceURI())
	     && "query".equals(_in.getLocalName())) {
      skipToEnd("iq");

      
      _os.print("<iq type='result' id='" + id + "' from='" + _jid + "'>");
      _os.print("<query xmlns='jabber:iq:roster'>");

      _os.print("<item jid='jimmy@localhost' name='Test' subscription='to'>");
      _os.print("<group>Buddies</group>");
      _os.print("</item>");
      
      _os.print("</query>");
      _os.print("</iq>");
      _os.flush();

      return true;
    }
    else if ("query".equals(_in.getLocalName())
	     && "http://jabber.org/protocol/disco#info".equals(uri)) {
      skipToEnd("iq");

      _os.print("<iq type='result' id='" + id + "'");
      if (to != null)
	_os.print(" from='" + to + "'");
      _os.print(">");
      _os.print("<query xmlns='http://jabber.org/protocol/disco#info'>");
      _os.print("<identity category='pubsub' type='leaf' name='test'/>");
      _os.print("<feature var='http://jabber.org/protocol/disco#info'/>");
      _os.print("<feature var='jabber:iq:time'/>");
      _os.print("<feature var='jabber:iq:search'/>");
      _os.print("<feature var='http://jabber.org/protocol/muc'/>");
      _os.print("<feature var='http://jabber.org/protocol/pubsub'/>");
      _os.print("</query>");
      _os.print("</iq>");
      _os.flush();
      System.out.println("QUERY: " + type + " query:" + _in.getLocalName() + " from:" + from + " to:" + to + " id:" + id);

      return true;
    }
    else {
      skipToEnd("iq");

      _os.print("<iq type='error'>");
      _os.print("<error/>");
      _os.print("</iq>");

      if (log.isLoggable(Level.FINE)) {
	log.fine(this + " <" + _in.getLocalName() + " xmlns="
		 + _in.getNamespaceURI() + "> unknown iq");
      }

      return true;
    }
  }

  private boolean handlePresence()
    throws IOException, XMLStreamException
  {
    String type = _in.getAttributeValue(null, "type");
    String id = _in.getAttributeValue(null, "id");
    String from = _in.getAttributeValue(null, "from");
    String to = _in.getAttributeValue(null, "to");

    int tag;
    
    while ((tag = _in.nextTag()) > 0
	   && ! ("presence".equals(_in.getLocalName())
		 && tag == XMLStreamReader.END_ELEMENT)) {
      if (_isFinest)
	debug(_in);
      
      if (tag != XMLStreamReader.START_ELEMENT)
	continue;

      if ("status".equals(_in.getLocalName())) {
	tag = _in.next();
    
	if (_isFinest)
	  debug(_in);
	
	String status = _in.getText();

	expectEnd("status");
	
	continue;
      }
    }
    
    if (_isFinest)
      debug(_in);

    expectEnd("presence", tag);

    /*
    if (! _isPresent) {
      _isPresent = true;
      _protocol.addClient(this);

      _os.print("<presence from='jimmy@localhost'>");
      _os.print("<status>active</status>");
      _os.print("</presence>");
    }
    */

    return true;
  }

  private boolean handleMessage()
    throws IOException, XMLStreamException
  {
    String type = _in.getAttributeValue(null, "type");
    String id = _in.getAttributeValue(null, "id");
    String from = _in.getAttributeValue(null, "from");
    String to = _in.getAttributeValue(null, "to");

    int tag;
    String body = "";
    
    while ((tag = _in.next()) > 0
	   && ! (tag == XMLStreamReader.END_ELEMENT
		 && "message".equals(_in.getLocalName()))) {
      if (_isFinest)
	debug(_in);
      
      if (tag != XMLStreamReader.START_ELEMENT)
	continue;

      if ("body".equals(_in.getLocalName())
	  && "jabber:client".equals(_in.getNamespaceURI())) {
	tag = _in.next();
	if (_isFinest)
	  debug(_in);
      
	body = _in.getText();

	expectEnd("body");
      }
    }

    expectEnd("message", tag);

    ImMessage message = new ImMessage(type, body);

    System.out.println("SEND-MESSAGE: " + message);
    _toBroker.sendMessage(to, from, message);

    /*
    try {
      ObjectMessageImpl msg = new ObjectMessageImpl();
      msg.setJMSMessageID("ID:xmpp-test");
    
      msg.setObject(body);

      if (log.isLoggable(Level.FINE))
	log.fine(this + " message to " + leaf);

      leaf.send(null, msg, 0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    */

    return true;
  }

  private void skipToEnd(String tagName)
    throws IOException, XMLStreamException
  {
    XMLStreamReader in = _in;
      
    if (in == null)
      return;

    int tag;
    while ((tag = in.next()) > 0) {
      if (_isFinest)
	debug(in);
	
      if (tag == XMLStreamReader.START_ELEMENT) {
      }
      else if (tag == XMLStreamReader.END_ELEMENT) {
	if (tagName.equals(in.getLocalName()))
	  return;
      }
    }
  }

  private void expectEnd(String tagName)
    throws IOException, XMLStreamException
  {
    expectEnd(tagName, _in.nextTag());
  }

  private void expectEnd(String tagName, int tag)
    throws IOException, XMLStreamException
  {
    if (tag != XMLStreamReader.END_ELEMENT)
      throw new IllegalStateException("expected </" + tagName + "> at <" + _in.getLocalName() + ">");

    else if (! tagName.equals(_in.getLocalName()))
      throw new IllegalStateException("expected </" + tagName + "> at </" + _in.getLocalName() + ">");
  }

  private boolean handleAuth()
    throws IOException, XMLStreamException
  {
    String mechanism = _in.getAttributeValue(null, "mechanism");

    if ("PLAIN".equals(mechanism))
      return handleAuthPlain();
    else
      throw new IllegalStateException("Unknown mechanism: " + mechanism);
  }

  private boolean handleAuthPlain()
    throws IOException, XMLStreamException
  {
    String value = null;

    int tag;
    while ((tag = _in.next()) > 0
	   && tag != XMLStreamConstants.START_ELEMENT
	   && tag != XMLStreamConstants.END_ELEMENT) {
      if (_isFinest)
	debug(_in);
      
      if (tag == XMLStreamConstants.CHARACTERS) {
	char []buffer = _in.getTextCharacters();
	int start = _in.getTextStart();
	int len = _in.getTextLength();
	
	value = new String(_in.getTextCharacters(), start, len);
      }
    }

    if (value == null)
      return false;
    
    if (_isFinest)
      debug(_in);

    String decoded = Base64.decode(value);

    int p = decoded.indexOf(0, 1);

    if (p < 0)
      return false;

    String name = decoded.substring(1, p);
    String password = decoded.substring(p + 1);

    boolean isAuth = true;

    if (isAuth) {
      _name = name;

      if (log.isLoggable(Level.FINE))
	log.fine(this + " auth-plain success for " + name);
      
      _os.print("<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'></success>");
      _os.flush();
      
      return true;
    }

    return false;
  }

  private void debug(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    if (XMLStreamReader.START_ELEMENT == in.getEventType()) {
      StringBuilder sb = new StringBuilder();
      sb.append("<").append(in.getLocalName());

      if (in.getNamespaceURI() != null)
	sb.append("{").append(in.getNamespaceURI()).append("}");

      for (int i = 0; i < in.getAttributeCount(); i++) {
	sb.append(" ");
	sb.append(in.getAttributeLocalName(i));
	sb.append("='");
	sb.append(in.getAttributeValue(i));
	sb.append("'");
      }
      sb.append(">");

      log.finest(this + " " + sb);
    }
    else if (XMLStreamReader.END_ELEMENT == in.getEventType()) {
      log.finest(this + " </" + in.getLocalName() + ">");
    }
    else if (XMLStreamReader.CHARACTERS == in.getEventType()) {
      String text = in.getText().trim();

      if (! "".equals(text))
	log.finest(this + " text='" + text + "'");
    }
    else
      log.finest(this + " tag=" + in.getEventType());
  }
  
  /**
   * Handles a message
   */
  public void sendMessage(String to,
			  String from,
			  Serializable value)
  {
    _toBroker.sendMessage(to, _jid, value);
  }
  
  /**
   * Handles a message
   */
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       HmtpError error)
  {
    _toBroker.sendMessageError(to, _jid, value, error);
  }
  
  /**
   * Handles a get query.
   *
   * The get handler must respond with either
   * a QueryResult or a QueryError 
   */
  public boolean sendQueryGet(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    _toBroker.sendQueryGet(id, to, _jid, value);
    
    return true;
  }
  
  /**
   * Handles a set query.
   *
   * The set handler must respond with either
   * a QueryResult or a QueryError 
   */
  public boolean sendQuerySet(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    _toBroker.sendQuerySet(id, to, _jid, value);
    
    return true;
  }
  
  /**
   * Handles a query result.
   *
   * The result id will match a pending get or set.
   */
  public void sendQueryResult(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    _toBroker.sendQueryResult(id, to, _jid, value);
  }
  
  /**
   * Handles a query error.
   *
   * The result id will match a pending get or set.
   */
  public void sendQueryError(long id,
			     String to,
			     String from,
			     Serializable value,
			     HmtpError error)
  {
    _toBroker.sendQueryError(id, to, _jid, value, error);
  }
  
  /**
   * Handles a presence availability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void sendPresence(String to,
			   String from,
			   Serializable []data)

  {
    _toBroker.sendPresence(to, _jid, data);
  }
  
  /**
   * Handles a presence unavailability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void sendPresenceUnavailable(String to,
				      String from,
				      Serializable []data)
  {
    _toBroker.sendPresenceUnavailable(to, _jid, data);
  }
  
  /**
   * Handles a presence probe from another server
   */
  public void sendPresenceProbe(String to,
			      String from,
			      Serializable []data)
  {
    _toBroker.sendPresenceProbe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribe request from a client
   */
  public void sendPresenceSubscribe(String to,
				    String from,
				    Serializable []data)
  {
    _toBroker.sendPresenceSubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribed result to a client
   */
  public void sendPresenceSubscribed(String to,
				     String from,
				     Serializable []data)
  {
    _toBroker.sendPresenceSubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribe request from a client
   */
  public void sendPresenceUnsubscribe(String to,
				      String from,
				      Serializable []data)
  {
    _toBroker.sendPresenceUnsubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void sendPresenceUnsubscribed(String to,
				       String from,
				       Serializable []data)
  {
    _toBroker.sendPresenceUnsubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void sendPresenceError(String to,
			      String from,
			      Serializable []data,
			      HmtpError error)
  {
    _toBroker.sendPresenceError(to, _jid, data, error);
  }

  public void close()
  {
    XMLStreamReaderImpl in = _in;
    _in = null;

    if (in != null) {
      try { in.close(); } catch (Exception e) {}
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _conn + "]";
  }
}
