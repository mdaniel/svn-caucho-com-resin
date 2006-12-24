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

package com.caucho.iiop.orb;

import com.caucho.vfs.*;
import com.caucho.iiop.*;

import org.omg.CORBA.*;

import java.applet.Applet;
import java.lang.reflect.Proxy;
import java.util.Properties;
import java.util.logging.Logger;
import javax.rmi.PortableRemoteObject;
import java.io.IOException;

public class ORBImpl extends org.omg.CORBA.ORB
{
  public static final Logger log
    = Logger.getLogger(ORBImpl.class.getName());
  
  private String _host;
  private int _port;

  private Path _path;

  private final StubDelegateImpl _stubDelegate;

  public ORBImpl()
  {
    _stubDelegate = new StubDelegateImpl(this);

    System.setProperty("javax.rmi.CORBA.PortableRemoteObjectClass",
		       "com.caucho.iiop.orb.PortableRemoteObjectDelegateImpl");
  }

  public String getHost()
  {
    return _host;
  }

  public void setHost(String host)
  {
    _host = host;
  }

  public int getPort()
  {
    return _port;
  }

  public void setPort(int port)
  {
    _port = port;
  }

  StubDelegateImpl getStubDelegate()
  {
    return _stubDelegate;
  }

  WriteStream openWriter()
  {
    try {
      ReadWritePair pair = _path.openReadWrite();

      return pair.getWriteStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  ReadWritePair openReadWrite()
  {
    try {
      return _path.openReadWrite();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public TypeCode create_alias_tc(String id, String name, TypeCode original)
  {
    throw new UnsupportedOperationException();
  }

  public Any create_any()
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_array_tc(int length, TypeCode element_type)
  {
    throw new UnsupportedOperationException();
  }

  public ContextList create_context_list()
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_enum_tc(String id, String name, String []members)
  {
    throw new UnsupportedOperationException();
  }

  public Environment create_environment()
  {
    throw new UnsupportedOperationException();
  }

  public ExceptionList create_exception_list()
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_exception_tc(String id,
				      String name,
				      StructMember []members)
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_interface_tc(String id,
				      String name)
  {
    throw new UnsupportedOperationException();
  }

  public NVList create_list(int count)
  {
    throw new UnsupportedOperationException();
  }

  public NamedValue create_named_value(String s, Any any, int flags)
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_native_tc(String id, String name)
  {
    throw new UnsupportedOperationException();
  }

  public org.omg.CORBA.portable.OutputStream create_output_stream()
  {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public TypeCode create_recursive_sequence_tc(int bound, int offset)
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_sequence_tc(int bound, TypeCode element_type)
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_string_tc(int bound)
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_struct_tc(String id,
				   String name,
				   StructMember []members)
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_union_tc(String id,
				  String name,
				  TypeCode discriminator_type,
				  UnionMember []members)
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode create_wstring_tc(int bound)
  {
    throw new UnsupportedOperationException();
  }

  public Context get_default_context()
  {
    throw new UnsupportedOperationException();
  }

  public Request get_next_response()
  {
    throw new UnsupportedOperationException();
  }

  public TypeCode get_primitive_tc(org.omg.CORBA.TCKind tcKind)
  {
    throw new UnsupportedOperationException();
  }

  public String []list_initial_services()
  {
    return new String[0];
  }

  @Override
  public String object_to_string(org.omg.CORBA.Object obj)
  {
    throw new UnsupportedOperationException();
  }

  public boolean poll_next_response()
  {
    throw new UnsupportedOperationException();
  }

  public org.omg.CORBA.Object resolve_initial_references(String object_name)
  {
    try {
      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      Iiop10Writer writer = new Iiop10Writer();

      ReadWritePair pair = openReadWrite();

      MessageWriter out = new StreamMessageWriter(pair.getWriteStream());

      IiopReader in = new IiopReader(pair.getReadStream());
      in.setOrb(this);
    
      writer.init(out, new IiopReader(pair.getReadStream()));

      byte []oid = new byte[] { 'I', 'N', 'I', 'T' };

      writer.startRequest(oid, 0, oid.length, "get", 1);

      writer.writeString(object_name);

      in = writer._call();
      in.setOrb(this);

      org.omg.CORBA.Object value = in.read_Object();

      in.close();
      
      return value;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void send_multiple_requests_deferred(Request []req)
  {
    throw new UnsupportedOperationException();
  }

  public void send_multiple_requests_oneway(Request []req)
  {
    throw new UnsupportedOperationException();
  }

  public void set_parameters(Applet app, Properties props)
  {
    throw new UnsupportedOperationException();
  }

  public void set_parameters(String []args, Properties props)
  {
    if (props != null) {
      java.lang.Object port = props.get("org.omg.CORBA.ORBInitialPort");
      java.lang.Object host = props.get("org.omg.CORBA.ORBInitialHost");

      if (host != null)
	_host = String.valueOf(host);

      if (port == null) {
      }
      else if (port instanceof Number)
	_port = ((Number) port).intValue();
      else
	_port = Integer.parseInt(String.valueOf(port));

      _path = Vfs.lookup("tcp://" + _host + ":" + port);
    }
  }

  public org.omg.CORBA.Object string_to_object(String str)
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    if (_host != null)
      return "ORBImpl[" + _host + ", " + _port + "]";
    else
      return "ORBImpl[]";
  }
}
