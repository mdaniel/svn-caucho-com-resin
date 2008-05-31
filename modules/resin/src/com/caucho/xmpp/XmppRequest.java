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

import com.caucho.xmpp.XmppPubSubLeaf;
import com.caucho.xmpp.XmppProtocol;
import com.caucho.xmpp.XmppBrokerStream;
import com.caucho.xmpp.Stanza;
import com.caucho.jms.memory.*;
import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;
import com.caucho.bam.BamBroker;
import com.caucho.server.connection.*;
import com.caucho.server.port.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.stream.*;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * XMPP protocol
 */
public class XmppRequest implements TcpServerRequest, Runnable {
  private static final L10N L = new L10N(XmppRequest.class);
  private static final Logger log
    = Logger.getLogger(XmppRequest.class.getName());

  private static final String STREAMS_NS = "http://etherx.jabber.org/streams";

  private XmppProtocol _protocol;

  private BamBroker _broker;
  
  private TcpConnection _conn;

  private ReadStream _is;
  private WriteStream _os;

  private volatile int _requestId;

  private String _id;
  private String _from;
  private String _clientTo;
  
  private String _streamFrom;
  private String _clientBind;

  private String _name;
  
  private XMLStreamReaderImpl _in;

  private boolean _isPresent;
  private boolean _isThread;

  private final ThreadPool _threadPool;
  private final BlockingQueue<Stanza> _outboundQueue
    = new ArrayBlockingQueue<Stanza>(1024);

  private State _state;
  private boolean _isFinest;

  XmppRequest(XmppProtocol protocol, TcpConnection conn)
  {
    _protocol = protocol;
    _broker = protocol.getBroker();
    _conn = conn;
    _threadPool = ThreadPool.getThreadPool();
  }

  int getRequestId()
  {
    return _requestId;
  }

  /**
   * Returns the tcp connection
   */
  public TcpConnection getConnection()
  {
    return _conn;
  }
  
  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  public void init()
  {
  }

  /**
   * Return true if the connection should wait for a read before
   * handling the request.
   */
  public boolean isWaitForRead()
  {
    return true;
  }
  
  /**
   * Handles a new connection.  The controlling TcpServer may call
   * handleConnection again after the connection completes, so 
   * the implementation must initialize any variables for each connection.
   */
  public boolean handleRequest()
    throws IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_protocol.getClassLoader());

      _isFinest = log.isLoggable(Level.FINEST);
      
      if (_state == null) {
	return handleInit();
      }

      TcpDuplexHandler handler = new XmppBrokerStream(_broker, _in, _os);
      
      TcpDuplexController controller = new TcpDuplexController(this, handler);

      return true;
      
      /*
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
      */
    } catch (XMLStreamException e) {
      e.printStackTrace();
      throw new IOExceptionWrapper(e);
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private boolean handleInit()
    throws IOException, XMLStreamException
  {
    _state = State.INIT;

    StringBuilder sb = new StringBuilder();
    Base64.encode(sb, RandomUtil.getRandomLong());
    while (sb.charAt(sb.length() - 1) == '=')
      sb.setLength(sb.length() - 1);
    
    _id = sb.toString();
    
    int ch;
      
    _is = _conn.getReadStream();
    _os = _conn.getWriteStream();
      
    _in = new XMLStreamReaderImpl(_is);

    int tag;
    while ((tag = _in.next()) > 0
	   && tag != XMLStreamConstants.START_ELEMENT) {
      if (_isFinest)
	debug(_in);
    }
    
    if (_isFinest)
      debug(_in);

    String name = _in.getLocalName();
      
    if (! "stream".equals(name)) {
      _os.print("<error><invalid-xml/></error>");
      
      if (log.isLoggable(Level.FINE))
	log.fine(L.l("{0}: '{1}' is an unknown tag from {2}",
		     this, name, _conn.getRemoteAddress()));
      return false;
    }
    else if (! STREAMS_NS.equals(_in.getNamespaceURI())) {
      _os.print("<error><bad-namespace-prefix/></error>");
      
      if (log.isLoggable(Level.FINE))
	log.fine(L.l("{0}: xmlns='{1}' is an unknown namespace from {2}",
		     this, name, _conn.getRemoteAddress()));
      
      return false;
    }
    /*
    else if (! "jabber:client".equals(_in.getNamespaceURI(""))) {
      _os.print("<error><bad-namespace-prefix/></error>");
      
      if (log.isLoggable(Level.FINE))
	log.fine(L.l("{0}: xmlns='{1}' is an unknown namespace for '' from {2}",
		     this, name, _conn.getRemoteAddress()));
      
      return false;
    }
    */
    else if (! "1.0".equals(_in.getAttributeValue(null, "version"))) {
      _os.print("<error><unsupported-version/></error>");
      
      if (log.isLoggable(Level.FINE))
	log.fine(L.l("{0}: version='{1}' is an unknown version from {2}",
		     this, _in.getAttributeValue(null, "version"),
		     _conn.getRemoteAddress()));
      
      return false;
    }

    String to = null;
      
    for (int i = _in.getAttributeCount() - 1; i >= 0; i--) {
      String localName = _in.getAttributeLocalName(i);
      String value = _in.getAttributeValue(i);

      if ("to".equals(localName))
	to = value;
    }

    String from = _from;

    if (from == null)
      from = to;

    if (from == null)
      from = _conn.getLocalAddress().getHostAddress();
      
    _streamFrom = from;
    _clientTo = from + "/" + _id;

    _os.print("<stream:stream xmlns='jabber:client'");
    _os.print(" xmlns:stream='http://etherx.jabber.org/streams'");
    _os.print(" id='" + _id + "'");
    _os.print(" from='" + from + "'");
    _os.print(" version='1.0'>");
      
    // + "   <mechanism>DIGEST-MD5</mechanism>\n"
    _os.print("<stream:features>");
	      //		+ "<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>"
    _os.print("<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>");
    _os.print("<mechanism>PLAIN</mechanism>");
    _os.print("</mechanisms>");
    _os.print("<auth xmlns='http://jabber.org/features/iq-auth'></auth>");
    //_os.print("<register xmlns='http://jabber.org/features/iq-register'></register>");
    _os.print("</stream:features>\n");
    _os.flush();

    return true;
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

    String from = _from;

    if (from == null)
      from = to;

    if (from == null)
      from = _conn.getLocalAddress().getHostAddress();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " stream open(from=" + from + " id=" + _id + ")");
      
    _os.print("<stream:stream xmlns='jabber:client'");
    _os.print(" xmlns:stream='http://etherx.jabber.org/streams'");
    _os.print(" id='" + _id + "'");
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

      _clientBind = "test@" + _streamFrom + "/" + resource;

      if (log.isLoggable(Level.FINE)) {
	log.fine(this + " bind-result(jid=" + _clientBind
		 + " id=" + id + " to=" + to + " from=" + from + ")");
      }
	
      _os.print("<iq type='result' id='" + id + "' to='" + _clientTo + "'>");
      _os.print("<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>");
      _os.print("<jid>" + _clientBind + "</jid>");
      _os.print("</bind></iq>\n");
      _os.flush();

      return true;
    }
    else if ("session".equals(_in.getLocalName())) {
      skipToEnd("iq");
      
      _os.print("<iq type='result' id='" + id + "' from='" + _streamFrom + "'/>");
      _os.flush();

      return true;
    }
    else if ("jabber:iq:roster".equals(_in.getNamespaceURI())
	     && "query".equals(_in.getLocalName())) {
      skipToEnd("iq");

      
      _os.print("<iq type='result' id='" + id + "' from='" + _streamFrom + "'>");
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

    if (! _isPresent) {
      _isPresent = true;
      _protocol.addClient(this);

      /*
      _os.print("<iq from='jimmy@localhost' id='disco' type='get'>");
      _os.print("<query xmlns='http://jabber.org/protocol/disco#info'/>");
      _os.print("</iq>");
      */

      _os.print("<presence from='jimmy@localhost'>");
      _os.print("<status>active</status>");
      _os.print("</presence>");
    }

    return true;
  }

  private boolean handleMessage()
    throws IOException, XMLStreamException
  {
    String type = _in.getAttributeValue(null, "type");
    String id = _in.getAttributeValue(null, "id");
    String from = _in.getAttributeValue(null, "from");
    String to = _in.getAttributeValue(null, "to");

    XmppPubSubLeaf leaf = _protocol.getNode(to);

    if (leaf == null) {
      log.fine(this + " message send to '" + to + "' unknown user");
      
      skipToEnd("message");
      
      return true;
    }

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
   * Resumes processing after a wait.
   */
  public boolean handleResume()
    throws IOException
  {
    return false;
  }

  /**
   * Handles a close event when the connection is closed.
   */
  public void protocolCloseEvent()
  {
    _protocol.removeClient(this);
    _requestId++;
    
    _state = null;
    _isPresent = false;

    synchronized (this) {
      _outboundQueue.clear();
    }
  }

  public void offer(int requestId, Stanza stanza)
  {
    synchronized (this) {
      if (requestId != _requestId || ! _isPresent)
	return;

      if (_outboundQueue.offer(stanza)) {
	if (! _isThread) {
	  _isThread = true;
	  _threadPool.schedule(this);
	}
      }
    }
  }

  public void run()
  {
    int id = _requestId;

    while (id == _requestId) {
      Stanza stanza = null;
      
      synchronized (this) {
	stanza = _outboundQueue.poll();

	if (stanza == null) {
	  _isThread = false;
	  return;
	}
      }

      try {
	if (log.isLoggable(Level.FINER))
	  log.finest(this + " send from=localhost/test to=" + _clientBind + " " + stanza);

	stanza.print(_os, "localhost/test", _clientBind);
	_os.flush();
      } catch (IOException e) {
	// XXX: should cause close?
	
	log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  public String toString()
  {
    if (_conn != null)
      return getClass().getSimpleName() + "[" + _conn.getId() + "]";
    else
      return getClass().getSimpleName() + "[]";
  }

  enum State {
    INIT
  };
}
