/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits modification and use of this file in
 * source and binary form ("the Software") subject to the Caucho
 * Developer Source License 1.1 ("the License") which accompanies
 * this file.  The License is also available at
 *   http://www.caucho.com/download/cdsl1-1.xtp
 *
 * In addition to the terms of the License, the following conditions
 * must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Each copy of the Software in source or binary form must include 
 *    an unmodified copy of the License in a plain ASCII text file named
 *    LICENSE.
 *
 * 3. Caucho reserves all rights to its names, trademarks and logos.
 *    In particular, the names "Resin" and "Caucho" are trademarks of
 *    Caucho and may not be used to endorse products derived from
 *    this software.  "Resin" and "Caucho" may not appear in the names
 *    of products derived from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind. 
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.      
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import java.util.HashMap;

import java.io.OutputStream;

import javax.management.*;

import com.caucho.server.cluster.*;

import com.caucho.hessian.io.*;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Admin API
 */
public class AdminClient {
  private ExtSerializerFactory _extFactory;
  ClusterClient _client;
  
  AdminClient(String host, int port)
    throws Exception
  {
    _client = findClient(host, port);

    if (_client == null)
      _client = createClient(host, port);

    _extFactory = new ExtSerializerFactory();
    _extFactory.addSerializer(ObjectName.class,
			      new StringValueSerializer());
    _extFactory.addDeserializer(ObjectName.class,
				new StringValueDeserializer(ObjectName.class));
  }

  private ClusterClient findClient(String host, int port)
  {
    ClusterGroup group = ClusterGroup.getClusterGroup();

    if (group == null)
      return null;

    return group.findClient(host, port);
  }

  private ClusterClient createClient(String host, int port)
    throws Exception
  {
    Cluster cluster = new Cluster();
    ClusterServer server = new ClusterServer(cluster);
    ClusterPort clusterPort = new ClusterPort(server);
    clusterPort.setHost(host);
    clusterPort.setPort(port);
    //server.setPort(clusterPort);
    server.init();
    
    return server.getServerConnector().getClient();
  }
  
  public HashMap lookup(String name)
    throws java.io.IOException
  {
    ClusterStream stream = openStream();

    try {
      WriteStream os = stream.getWriteStream();
      ReadStream is = stream.getReadStream();

      HmuxOutput hmuxOut = new HmuxOutput();

      hmuxOut.open(os);

      HessianOutput out = new HessianOutput(hmuxOut);

      out.startCall("fill");
      out.writeString(name);
      out.completeCall();
      hmuxOut.close();
      
      HmuxInput hmuxIn = new HmuxInput();
      hmuxIn.open(is);

      HessianInput in = new HessianInput(hmuxIn);
      in.getSerializerFactory().addFactory(_extFactory);

      in.startReply();

      Object result = in.readObject();

      in.completeReply();
      
      return (HashMap) result;
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    } finally {
      stream.close();
    }
  }
  
  public String []query(String pattern)
    throws java.io.IOException
  {
    ClusterStream stream = openStream();

    try {
      WriteStream os = stream.getWriteStream();
      ReadStream is = stream.getReadStream();

      HmuxOutput hmuxOut = new HmuxOutput();

      hmuxOut.open(os);

      HessianOutput out = new HessianOutput(hmuxOut);

      out.startCall("query");
      out.writeString(pattern);
      out.completeCall();
      hmuxOut.close();
      
      HmuxInput hmuxIn = new HmuxInput();
      hmuxIn.open(is);

      HessianInput in = new HessianInput(hmuxIn);
      in.getSerializerFactory().addFactory(_extFactory);

      in.startReply();

      Object result = in.readObject();

      in.completeReply();
      
      return (String []) result;
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    } finally {
      stream.close();
    }
  }

  private ClusterStream openStream()
  {
    return _client.open();
  }
}
