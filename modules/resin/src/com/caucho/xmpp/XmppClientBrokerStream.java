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

import com.caucho.bam.*;
import com.caucho.bam.im.*;
import com.caucho.vfs.*;
import com.caucho.xml.stream.*;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * xmpp client to broker
 */
class XmppClientBrokerStream extends AbstractBamStream
{
  private static final Logger log
    = Logger.getLogger(XmppClientBrokerStream.class.getName());

  private WriteStream _os;
  private XMLStreamWriter _out;

  XmppClientBrokerStream(XmppClient client, WriteStream os)
  {
    _os = os;
    _out = new XMLStreamWriterImpl(os);
  }

  /**
   * Sends a message to the stream
   */
  @Override
  public void sendMessage(String to, String from, Serializable value)
  {
    try {
      XMLStreamWriter out = _out;

      synchronized (out) {
	out.writeStartElement("message");

	if (to != null)
	  out.writeAttribute("to", to);

	if (from != null)
	  out.writeAttribute("from", to);

	ImMessage msg = (ImMessage) value;

	if (msg.getType() != null)
	  out.writeAttribute("type", msg.getType());

	Text []subjects = msg.getSubjects();
	if (subjects != null) {
	  for (Text subject : subjects) {
	    out.writeStartElement("subject");

	    if (subject.getLang() != null)
	      out.writeAttribute("xml", "http://xml.org", "lang", subject.getLang());
	    
	    out.writeCharacters(subject.getValue());
	    out.writeEndElement(); // </subject>
	  }
	}
	
	Text []bodys = msg.getBodys();
	if (bodys != null) {
	  for (Text body : bodys) {
	    out.writeStartElement("body");

	    if (body.getLang() != null)
	      out.writeAttribute("xml", "http://xml.org", "lang",
				 body.getLang());
	    
	    out.writeCharacters(body.getValue());
	    out.writeEndElement(); // </body>
	  }
	}

	if (msg.getThread() != null) {
	  out.writeStartElement("thread");
	  out.writeCharacters(msg.getThread());
	  out.writeEndElement(); // </thread>
	}

	out.writeEndElement(); // </message>

	out.flush();
      }

      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " sendMessage to=" + to + " from=" + from
		  + " msg=" + value);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
