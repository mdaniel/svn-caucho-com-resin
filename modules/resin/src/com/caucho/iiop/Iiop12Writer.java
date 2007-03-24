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

package com.caucho.iiop;

import java.io.IOException;
import java.util.*;

import com.caucho.vfs.*;

public class Iiop12Writer extends Iiop10Writer
{
  private final static int KEY_ADDR = 0;
  private final static int PROFILE_ADDR = 1;
  private final static int REFERENCE_ADDR = 2;

  public Iiop12Writer()
  {
  }

  public Iiop12Writer(Iiop12Writer parent, MessageWriter out)
  {
    super(parent, out);
  }
  
  /**
   * Writes the header for a request
   *
   * @param operation the method to call
   */
  @Override
  public void startRequest(byte []oid, int off, int len,
			   String operation, int requestId,
			   ArrayList<ServiceContext> contextList)
    throws IOException
  {
    _out.start12Message(IiopReader.MSG_REQUEST);

    _out.writeInt(requestId);
    
    int flags = 0x3;

    _out.write(flags);      // response expected
    _out.write(0);
    _out.write(0);
    _out.write(0);

    write_short((short) KEY_ADDR);       // target
    writeBytes(oid, off, len);  // object id
    
    writeString(operation);

    if (contextList != null) {
      write_long(contextList.size()); // service context list

      for (int i = 0; i < contextList.size(); i++)
	contextList.get(i).write(this);
    }
    else
      write_long(0);       // service context list

    // see IiopRead12.read12Request
    //_out.align(8);
  }
  
  /**
   * Writes the header for a request
   */
  public void startReplyOk(int requestId)
    throws IOException
  {
    _out.start12Message(IiopReader.MSG_REPLY);

    write_long(requestId);
    write_long(IiopReader.STATUS_NO_EXCEPTION); // okay
    write_long(0);                       // service control list
  }
  
  /**
   * Writes the header for a request
   */
  @Override
  public void startReplySystemException(int requestId,
                                        String exceptionId,
                                        int minorStatus,
                                        int completionStatus,
					Throwable cause)
    throws IOException
  {
    _out.start12Message(IiopReader.MSG_REPLY);
    
    write_long(requestId);
    write_long(IiopReader.STATUS_SYSTEM_EXCEPTION);

    if (cause == null) {
      write_long(0);         // service control list
    } else {
      write_long(1);
      write_long(IiopReader.SERVICE_UNKNOWN_EXCEPTION_INFO);

      EncapsulationMessageWriter out = new EncapsulationMessageWriter();
      Iiop12Writer writer = new Iiop12Writer(this, out);
      writer.init(out);
      writer.write(0); // endian
      writer.write_value(cause);
      //writer.close();
      out.close();

      int len = out.getOffset();
      write_long(len);

      out.writeToWriter(this);
    }

    writeString(exceptionId);
    write_long(minorStatus);
    write_long(completionStatus);
  }
  
  /**
   * Writes the header for a request
   */
  public void startReplyUserException(int requestId)
    throws IOException
  {
    _out.start12Message(IiopReader.MSG_REPLY);
    
    write_long(requestId);
    write_long(IiopReader.STATUS_USER_EXCEPTION);
    write_long(0);         // service control list
  }

  /**
   * Writes a 16-bit char.
   */
  public void write_wchar(char v)
  {
    _out.write(2);
    _out.write(v >> 8);
    _out.write(v);
  }

  /**
   * Writes a sequence of 16-bit characters to the output stream.
   */
  public void write_wstring(String a)
  {
    if (a == null) {
      write_long(0);
      return;
    }
    
    int length = a.length();
    write_long(2 * length);
    for (int i = 0; i < length; i++)
      _out.writeShort((int) a.charAt(i));
  }
}
