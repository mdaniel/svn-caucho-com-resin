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

package com.caucho.hmtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2StreamingInput;

/**
 * ClientToServerLink stream handles client packets received from the server.
 */
abstract public class FromLinkStream {
  private static final Logger log
    = Logger.getLogger(FromLinkStream.class.getName());

  private Hessian2StreamingInput _in;
  
  protected FromLinkStream(InputStream is)
  {
    _in = new Hessian2StreamingInput(is);
  }

  abstract public String getJid();

  abstract protected ActorStream getStream(String to);

  abstract protected ActorStream getToLinkStream();

  abstract protected String getFrom(String from);

  /**
   * Reads the next HMTP packet from the stream, returning false on
   * end of file.
   */
  protected boolean readPacket()
    throws IOException
  {
    Hessian2StreamingInput in = _in;

    if (in == null)
      return false;

    Hessian2Input hIn = null;

    try {
      hIn = in.startPacket();
    } catch (IOException e) {
      log.fine(this + " exception while reading HMTP packet\n  " + e);
      
      log.log(Level.FINER, e.toString(), e);
    }

    if (hIn == null) {
      close();
      return false;
    }

    int type = hIn.readInt();
    String to = hIn.readString();
    String from = hIn.readString();
    
    switch (HmtpPacketType.TYPES[type]) {
    case MESSAGE:
      {
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " message " + value
		    + " {to:" + to + ", from:" + from + "}");
	}
	
	getStream(to).message(to, getFrom(from), value);

	break;
      }
      
    case MESSAGE_ERROR:
      {
	Serializable value = (Serializable) hIn.readObject();
	ActorError error = (ActorError) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " messageError " + error + " " + value
		    + " {to:" + to + ", from:" + from + "}");
	}

	getStream(to).messageError(to, getFrom(from), value, error);

	break;
      }
      
    case QUERY_GET:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " queryGet " + value
		    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
	}

	try {
	  getStream(to).queryGet(id, to, getFrom(from), value);
	} catch (Exception e) {
	  if (log.isLoggable(Level.FINER))
	    log.log(Level.FINER, e.toString(), e);
	  else
	    log.fine(e.toString());
	  
	  getToLinkStream().queryError(id, from, to, value,
				       ActorError.create(e));
	}

	break;
      }
      
    case QUERY_SET:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " querySet " + value
		    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
	}

	try {
	  getStream(to).querySet(id, to, getFrom(from), value);
	} catch (Exception e) {
	  if (log.isLoggable(Level.FINER))
	    log.log(Level.FINER, e.toString(), e);
	  else
	    log.fine(e.toString());

	  getToLinkStream().queryError(id, from, to, value,
				       ActorError.create(e));
	}

	break;
      }
      
    case QUERY_RESULT:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " queryResult " + value
		    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
	}

	getStream(to).queryResult(id, to, getFrom(from), value);

	break;
      }
      
    case QUERY_ERROR:
      {
	long id = hIn.readLong();
	Serializable value = (Serializable) hIn.readObject();
	ActorError error = (ActorError) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " queryError " + error + " " + value
		    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
	}

	getStream(to).queryError(id, to, getFrom(from), value, error);

	break;
      }
      
    case PRESENCE:
      {
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " presence " + value
		    + " {to:" + to + ", from:" + from + "}");
	}

	getStream(to).presence(to, getFrom(from), value);

	break;
      }
      
    case PRESENCE_UNAVAILABLE:
      {
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " presenceUnavailable " + value
		    + " {to:" + to + ", from:" + from + "}");
	}

	getStream(to).presenceUnavailable(to, getFrom(from), value);

	break;
      }
      
    case PRESENCE_PROBE:
      {
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " presenceProbe " + value
		    + " {to:" + to + ", from:" + from + "}");
	}

	getStream(to).presenceProbe(to, getFrom(from), value);

	break;
      }
      
    case PRESENCE_SUBSCRIBE:
      {
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " presenceSubscribe " + value
		    + " {to:" + to + ", from:" + from + "}");
	}

	getStream(to).presenceSubscribe(to, getFrom(from), value);

	break;
      }
      
    case PRESENCE_SUBSCRIBED:
      {
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " presenceSubscribed " + value
		    + " {to:" + to + ", from:" + from + "}");
	}

	getStream(to).presenceSubscribed(to, getFrom(from), value);

	break;
      }
      
    case PRESENCE_UNSUBSCRIBE:
      {
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " presenceUnsubscribe " + value
		    + " {to:" + to + ", from:" + from + "}");
	}

	getStream(to).presenceUnsubscribe(to, getFrom(from), value);

	break;
      }
      
    case PRESENCE_UNSUBSCRIBED:
      {
	Serializable value = (Serializable) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " presenceUnsubscribed " + value
		    + " {to:" + to + ", from:" + from + "}");
	}

	getStream(to).presenceUnsubscribed(to, getFrom(from), value);

	break;
      }

    case PRESENCE_ERROR:
      {
	Serializable value = (Serializable) hIn.readObject();
	ActorError error = (ActorError) hIn.readObject();
	in.endPacket();

	if (log.isLoggable(Level.FINER)) {
	  log.finer(this + " presenceError " + error + " " + value
		    + " {to:" + to + ", from:" + from + "}");
	}

	getStream(to).presenceError(to, getFrom(from), value, error);

	break;
      }

    default:
      throw new UnsupportedOperationException("ERROR: " + HmtpPacketType.TYPES[type]);
    }

    return true;
  }

  protected void close()
  {
    try {
      Hessian2StreamingInput in = _in;
      _in = null;

      if (in != null)
	in.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    // _client.close();
  }

  @Override
  public String toString()
  {
    // XXX: should have the connection
    return getClass().getSimpleName() + "[]";
  }
}
