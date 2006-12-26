/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop;

import java.io.IOException;

public class Iiop10Writer extends IiopWriter
{
  public Iiop10Writer()
  {
  }

  public Iiop10Writer(IiopWriter parent, MessageWriter out)
  {
    super(parent, out);
  }
  
  /**
   * Writes the header for a request
   *
   * @param operation the method to call
   */
  public void startRequest(byte []oid, int off, int len,
			   String operation, int requestId)
    throws IOException
  {
    startMessage(IiopReader.MSG_REQUEST);

    writeRequestServiceControlList(); // service context list
    
    write_long(requestId);       // request id
    _out.write(1);              // response expected

    writeBytes(oid, off, len); // object id
    writeString(operation);
    writeNull();               // principal
  }
  
  /**
   * Writes the header for a request
   */
  public void startReplyOk(int requestId)
    throws IOException
  {
    startMessage(IiopReader.MSG_REPLY);
    
    write_long(0);         // service control list
    write_long(requestId); // request id
    write_long(IiopReader.STATUS_NO_EXCEPTION); // okay
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
    startMessage(IiopReader.MSG_REPLY);
    
    System.out.println("CAUSE-10:" + cause);
    
    if (cause == null) {
      write_long(0);         // service control list
    } else {
      write_long(1);
      write_long(IiopReader.SERVICE_UNKNOWN_EXCEPTION_INFO);

      EncapsulationMessageWriter out = new EncapsulationMessageWriter();
      Iiop10Writer writer = new Iiop10Writer(this, out);
      writer.init(out);
      writer.write(0); // endian
      writer.write_value(cause);
      //writer.close();
      out.close();

      int len = out.getOffset();
      write_long(len);

      out.writeToWriter(this);
    }
    
    write_long(requestId); // request id
    write_long(IiopReader.STATUS_SYSTEM_EXCEPTION);

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
    startMessage(IiopReader.MSG_REPLY);
    
    write_long(0);         // service control list
    write_long(requestId); // request id
    write_long(IiopReader.STATUS_USER_EXCEPTION);
  }

  /**
   * Starts the message.
   */
  protected void startMessage(int type)
    throws IOException
  {
    _out.start10Message(type);
  }
}
