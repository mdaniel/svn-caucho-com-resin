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

package com.caucho.server.hmux;

import com.caucho.bam.AbstractBamStream;
import com.caucho.bam.BamError;
import com.caucho.bam.BamException;
import com.caucho.hessian.io.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.util.logging.*;

/**
 * Sends bam messages to a hmux stream
 */
public class HmuxBamStream extends AbstractBamStream {
  private static final L10N L = new L10N(HmuxBamStream.class);
  
  private static final Logger log
    = Logger.getLogger(HmuxBamStream.class.getName());

  private HmuxRequest _request;

  public HmuxBamStream(HmuxRequest request)
  {
    _request = request;
  }

  /**
   * Sends a message to the stream.
   */
  public void message(String to,
		      String from,
		      Serializable value)
  {
    try {
      _request.writeHmtpMessage(to, from, value);
    } catch (IOException e) {
      throw new BamException(e);
    }
  }

  /**
   * Sends a message to the stream.
   */
  public void messageError(String to,
			   String from,
			   Serializable value,
			   BamError error)
  {
    try {
      _request.writeHmtpMessageError(to, from, value, error);
    } catch (IOException e) {
      throw new BamException(e);
    }
  }

  public boolean queryGet(long id,
			  String to,
			  String from,
			  Serializable value)
  {
    try {
      _request.writeHmtpQueryGet(id, to, from, value);

      return true;
    } catch (IOException e) {
      throw new BamException(e);
    }
  }

  public boolean querySet(long id,
			  String to,
			  String from,
			  Serializable value)
  {
    try {
      _request.writeHmtpQuerySet(id, to, from, value);
      
      return true;
    } catch (IOException e) {
      throw new BamException(e);
    }
  }

  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable value)
  {
    try {
      _request.writeHmtpQueryResult(id, to, from, value);
    } catch (IOException e) {
      throw new BamException(e);
    }
  }

  public void queryError(long id,
			 String to,
			 String from,
			 Serializable value,
			 BamError error)
  {
    try {
      _request.writeHmtpQueryError(id, to, from, value, error);
    } catch (IOException e) {
      throw new BamException(e);
    }
  }

  public void close()
  {
    _request = null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _request + "]";
  }
}
