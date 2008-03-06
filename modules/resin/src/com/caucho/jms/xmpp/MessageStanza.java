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

import com.caucho.server.connection.*;
import com.caucho.server.port.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * XMPP protocol
 */
public class MessageStanza extends Stanza {
  private String _subject;
  private String _body;

  public void setSubject(String subject)
  {
    _subject = subject;
  }

  public void setBody(String body)
  {
    _body = body;
  }

  @Override
  protected void print(WriteStream out, String from, String to)
    throws IOException
  {
    out.print("<message");

    if (from != null) {
      out.print(" from='");
      out.print(from);
      out.print("'");
    }

    if (to != null) {
      out.print(" to='");
      out.print(to);
      out.print("'");
    }

    out.print(" type='chat'");

    out.print(">");

    if (_subject != null) {
      out.print("<subject>");
      out.print(_subject);
      out.print("</subject>");
    }

    if (_body != null) {
      out.print("<body>");
      out.print(_body);
      out.print("</body>");
    }

    System.out.println("MSG: " + _body);

    out.print("</message>");
  }
}
