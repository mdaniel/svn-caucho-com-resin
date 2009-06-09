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

package com.caucho.bam.hmtp;

import com.caucho.bam.QueryCallback;
import com.caucho.bam.ActorStream;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorClient;
import com.caucho.bam.SimpleActorClient;
import com.caucho.bam.SimpleActorStream;
import com.caucho.bam.ActorException;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.hessian.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.security.PublicKey;
import javax.servlet.http.HttpServletResponse;

/**
 * HMTP client protocol
 */
public interface LinkClient {
  public String getJid();

  public ActorStream getActorStream();

  public ActorStream getLinkStream();
  
  public boolean isClosed();
  
  public void close();
}
