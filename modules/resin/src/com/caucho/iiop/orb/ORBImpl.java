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

import java.applet.Applet;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import org.omg.CORBA.*;

public class ORBImpl extends org.omg.CORBA.ORB
{
  public static final Logger log
    = Logger.getLogger(ORBImpl.class.getName());

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
    System.out.println("RESOLVE: " + object_name);
    throw new UnsupportedOperationException();
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
      java.lang.Object portName = props.get("org.omg.CORBA.ORBInitialPort");
      java.lang.Object host = props.get("org.omg.CORBA.ORBInitialHost");
      int port = 0;
	
      System.out.println("PROPS: " + props);
    }
  }

  public org.omg.CORBA.Object string_to_object(String str)
  {
    throw new UnsupportedOperationException();
  }
}
