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

package com.caucho.xmpp;

import com.caucho.bam.BamStream;
import com.caucho.bam.BamError;
import com.caucho.bam.BamConnection;
import com.caucho.bam.im.*;
import com.caucho.bam.BamBroker;
import com.caucho.server.connection.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.stream.*;
import com.caucho.xmpp.im.XmppRosterQueryMarshal;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.servlet.*;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

/**
 * Protocol handler from the TCP/XMPP stream forwarding to the broker
 */
public class XmppBrokerStream
  implements TcpDuplexHandler, BamStream
{
  private static final Logger log
    = Logger.getLogger(XmppBrokerStream.class.getName());

  private XmppRequest _request;
  private XmppProtocol _protocol;
  private XmppContext _xmppContext;
  
  private BamBroker _broker;
  private BamConnection _conn;
  private BamStream _toBroker;

  private BamStream _toClient;
  private BamStream _authHandler;

  private ReadStream _is;
  private WriteStream _os;
  
  private XMLStreamReaderImpl _in;
  private XMLStreamWriter _out;

  private XmppReader _reader;

  private String _jid;
  private long _requestId;

  private String _uid = "test@localhost";
  private boolean _isFinest;

  // XXX: needs timeout(?)
  private HashMap<Long,String> _idMap = new HashMap<Long,String>();

  XmppBrokerStream(XmppRequest request, BamBroker broker,
		   ReadStream is, XMLStreamReaderImpl in, WriteStream os)
  {
    _request = request;
    _protocol = request.getProtocol();
    _xmppContext = new XmppContext(_protocol.getMarshalFactory());
    _broker = broker;

    _in = in;
    _os = os;

    _uid = request.getUid();

    _out =  new XMLStreamWriterImpl(os);

    _toClient = new XmppAgentStream(this, _os);
    _authHandler = null;//new AuthBrokerStream(this, _callbackHandler);

    _reader = new XmppReader(_xmppContext,
			     is, _in, _toClient,
			     new XmppBindCallback(this));

    _reader.setUid(_uid);

    _isFinest = log.isLoggable(Level.FINEST);
  }

  protected String getJid()
  {
    return _jid;
  }

  BamStream getAgentStream()
  {
    return _toClient;
  }

  XmppMarshalFactory getMarshalFactory()
  {
    return _protocol.getMarshalFactory();
  }

  XmppContext getXmppContext()
  {
    return _xmppContext;
  }
  
  public boolean serviceRead(ReadStream is,
			     TcpDuplexController controller)
    throws IOException
  {
    if (true)
      return _reader.readNext();
    
    XMLStreamReaderImpl in = _in;
    
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
	  
	  if ("iq".equals(_in.getLocalName()))
	    valid = _reader.handleIq();
	  else if ("presence".equals(_in.getLocalName()))
	    valid = _reader.handlePresence();
	  else if ("message".equals(_in.getLocalName()))
	    valid = _reader.handleMessage();
	  else {
	    if (log.isLoggable(Level.FINE))
	      log.fine(this + " " + _in.getLocalName() + " is an unknown tag");
	    
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
    
    _uid = uid + "@localhost";
    
    _conn = _broker.getConnection(_uid, password);
    _conn.setMessageHandler(_toClient);
    _conn.setQueryHandler(_toClient);
    _conn.setPresenceHandler(_toClient);

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

  /**
   * Processes a message
   */
  private boolean handleMessage()
    throws IOException, XMLStreamException
  {
    String type = _in.getAttributeValue(null, "type");
    String id = _in.getAttributeValue(null, "id");
    String from = _in.getAttributeValue(null, "from");
    String to = _in.getAttributeValue(null, "to");

    int tag;

    from = _jid;

    if (to == null)
      to = _uid;
    
    ArrayList<Text> subjectList = new ArrayList<Text>();
    ArrayList<Text> bodyList = new ArrayList<Text>();
    String thread = null;
    
    while ((tag = _in.next()) > 0
	   && ! (tag == XMLStreamReader.END_ELEMENT
		 && "message".equals(_in.getLocalName()))) {
      if (_isFinest)
	debug(_in);
      
      if (tag != XMLStreamReader.START_ELEMENT)
	continue;

      if ("body".equals(_in.getLocalName())
	  && "jabber:client".equals(_in.getNamespaceURI())) {
	String lang = null;

	if (_in.getAttributeCount() > 0
	    && "lang".equals(_in.getAttributeLocalName(0))) {
	  lang = _in.getAttributeValue(0);
	}
	
	tag = _in.next();
	if (_isFinest)
	  debug(_in);

	String body = _in.getText();

	bodyList.add(new Text(body, lang));

	expectEnd("body");
      }
      else if ("subject".equals(_in.getLocalName())
	       && "jabber:client".equals(_in.getNamespaceURI())) {
	String lang = null;
	
	if (_in.getAttributeCount() > 0
	    && "lang".equals(_in.getAttributeLocalName(0)))
	  lang = _in.getAttributeValue(0);
	
	tag = _in.next();
	if (_isFinest)
	  debug(_in);

	String text = _in.getText();

	subjectList.add(new Text(text, lang));

	expectEnd("subject");
      }
      else if ("thread".equals(_in.getLocalName())
	       && "jabber:client".equals(_in.getNamespaceURI())) {
	tag = _in.next();
	if (_isFinest)
	  debug(_in);

	thread = _in.getText();

	expectEnd("thread");
      }
    }

    expectEnd("message", tag);

    Text []subjectArray = null;

    if (subjectList.size() > 0) {
      subjectArray = new Text[subjectList.size()];
      subjectList.toArray(subjectArray);
    }

    Text []bodyArray = null;

    if (bodyList.size() > 0) {
      bodyArray = new Text[bodyList.size()];
      bodyList.toArray(bodyArray);
    }

    Serializable []extra = null;

    ImMessage message = new ImMessage(to, from, type,
				      subjectArray, bodyArray, thread,
				      extra);

    _toBroker.sendMessage(to, from, message);

    return true;
  }

  String bind(String resource, String jid)
  {
    String password = null;
    
    _conn = _broker.getConnection(_uid, password, resource);
    _conn.setMessageHandler(_toClient);
    _conn.setQueryHandler(_toClient);
    _conn.setPresenceHandler(_toClient);

    _jid = _conn.getJid();
    
    _toBroker = _conn.getBrokerStream();
    
    _reader.setJid(_jid);
    _reader.setHandler(_toBroker);
    
    return _jid;
  }

  /**
   * Processes a query
   */
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
    else {
      QName name = _in.getName();

      Serializable query = null;

      XmppMarshal marshal = _protocol.getUnserialize(name);

      if (marshal != null)
	query = marshal.fromXml(_in);
      else
	query = readAsXmlString(_in);

      BamError error = null;

      if (to == null)
	to = _uid;
      
      skipToEnd("iq");

      if ("get".equals(type)) {
	long bamId = addId(id);
	  
	_toBroker.sendQueryGet(bamId, to, _jid, query);
      }
      else if ("set".equals(type)) {
	long bamId = addId(id);
	  
	_toBroker.sendQuerySet(bamId, to, _jid, query);
      }
      else if ("result".equals(type)) {
	long bamId = Long.parseLong(id);
	  
	_toBroker.sendQueryResult(bamId, to, _jid, query);
      }
      else if ("error".equals(type)) {
	long bamId = Long.parseLong(id);
	  
	_toBroker.sendQueryError(bamId, to, _jid, query, error);
      }
      else {
	if (log.isLoggable(Level.FINE)) {
	  log.fine(this + " <" + _in.getLocalName() + " xmlns="
		   + _in.getNamespaceURI() + "> unknown type");
	}
      }

      return true;
    }
  }

  private long addId(String id)
  {
    long bamId;
    
    synchronized (_idMap) {
      bamId = _requestId++;
      
      _idMap.put(bamId, id);
    }

    return bamId;
  }

  String findId(long bamId)
  {
    synchronized (_idMap) {
      return _idMap.remove(bamId);
    }
  }

  private boolean handlePresence()
    throws IOException, XMLStreamException
  {
    String type = _in.getAttributeValue(null, "type");
    String id = _in.getAttributeValue(null, "id");
    String from = _in.getAttributeValue(null, "from");
    String to = _in.getAttributeValue(null, "to");

    if (type == null)
      type = "";

    int tag;

    String show = null;
    Text status = null;
    int priority = 0;
    ArrayList<Serializable> extraList = new ArrayList<Serializable>();
    BamError error = null;
    
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
	
	status = new Text(_in.getText());

	skipToEnd("status");
      }
      else if ("show".equals(_in.getLocalName())) {
	tag = _in.next();
    
	if (_isFinest)
	  debug(_in);
	
	show = _in.getText();

	skipToEnd("show");
      }
      else if ("priority".equals(_in.getLocalName())) {
	tag = _in.next();
    
	if (_isFinest)
	  debug(_in);
	
	priority = Integer.parseInt(_in.getText());

	skipToEnd("show");
      }
      else {
	
      }
    }
    
    if (_isFinest)
      debug(_in);

    expectEnd("presence", tag);

    from = _jid;

    if (to == null)
      to = _uid;

    ImPresence presence = new ImPresence(to, from,
					 show, status, priority,
					 extraList);

    if ("".equals(type))
      _toBroker.sendPresence(to, from, presence);
    else if ("probe".equals(type))
      _toBroker.sendPresenceProbe(to, from, presence);
    else if ("unavailable".equals(type))
      _toBroker.sendPresenceUnavailable(to, from, presence);
    else if ("subscribe".equals(type))
      _toBroker.sendPresenceSubscribe(to, from, presence);
    else if ("subscribed".equals(type))
      _toBroker.sendPresenceSubscribed(to, from, presence);
    else if ("unsubscribe".equals(type))
      _toBroker.sendPresenceUnsubscribe(to, from, presence);
    else if ("unsubscribed".equals(type))
      _toBroker.sendPresenceUnsubscribed(to, from, presence);
    else if ("error".equals(type))
      _toBroker.sendPresenceError(to, from, presence, error);
    else
      log.warning(this + " " + type + " is an unknown presence type");

    return true;
  }

  void writeValue(Serializable value)
    throws IOException, XMLStreamException
  {
    if (value == null)
      return;
    
    XmppMarshal marshal = _protocol.getSerialize(value.getClass().getName());

    if (marshal != null) {
      marshal.toXml(_out, value);
    }
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

    login(name, password, null);

    boolean isAuth = true;

    if (isAuth) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " auth-plain success for " + name);
      
      _os.print("<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'></success>");
      _os.flush();
      
      return true;
    }

    return false;
  }

  private String readAsXmlString(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    StringBuilder sb = new StringBuilder();
    int depth = 0;

    while (true) {
      if (XMLStreamReader.START_ELEMENT == in.getEventType()) {
	depth++;

	String prefix = in.getPrefix();
	
	sb.append("<");

	if (! "".equals(prefix)) {
	  sb.append(prefix);
	  sb.append(":");
	}
	
	sb.append(in.getLocalName());

	if (in.getNamespaceURI() != null) {
	  if ("".equals(prefix))
	    sb.append(" xmlns");
	  else
	    sb.append(" xmlns:").append(prefix);
	    
	  sb.append("=\"");
	  sb.append(in.getNamespaceURI()).append("\"");
	}

	for (int i = 0; i < in.getAttributeCount(); i++) {
	  sb.append(" ");
	  sb.append(in.getAttributeLocalName(i));
	  sb.append("=\"");
	  sb.append(in.getAttributeValue(i));
	  sb.append("\"");
	}
	sb.append(">");

	log.finest(this + " " + sb);
      }
      else if (XMLStreamReader.END_ELEMENT == in.getEventType()) {
	depth--;

	sb.append("</");

	String prefix = in.getPrefix();
	if (! "".equals(prefix))
	  sb.append(prefix).append(":");
	
	sb.append(in.getLocalName());
	sb.append(">");

	if (depth == 0)
	  return sb.toString();
      }
      else if (XMLStreamReader.CHARACTERS == in.getEventType()) {
	sb.append(in.getText());
      }
      else {
	log.finer(this + " tag=" + in.getEventType());

	return sb.toString();
      }

      if (in.next() < 0) {
	log.finer(this + " unexpected end of file");
	
	return sb.toString();
      }
    }
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
			       BamError error)
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
			     BamError error)
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
			   Serializable data)

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
				      Serializable data)
  {
    _toBroker.sendPresenceUnavailable(to, _jid, data);
  }
  
  /**
   * Handles a presence probe from another server
   */
  public void sendPresenceProbe(String to,
			      String from,
			      Serializable data)
  {
    _toBroker.sendPresenceProbe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribe request from a client
   */
  public void sendPresenceSubscribe(String to,
				    String from,
				    Serializable data)
  {
    _toBroker.sendPresenceSubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribed result to a client
   */
  public void sendPresenceSubscribed(String to,
				     String from,
				     Serializable data)
  {
    _toBroker.sendPresenceSubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribe request from a client
   */
  public void sendPresenceUnsubscribe(String to,
				      String from,
				      Serializable data)
  {
    _toBroker.sendPresenceUnsubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void sendPresenceUnsubscribed(String to,
				       String from,
				       Serializable data)
  {
    _toBroker.sendPresenceUnsubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void sendPresenceError(String to,
			      String from,
			      Serializable data,
			      BamError error)
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
