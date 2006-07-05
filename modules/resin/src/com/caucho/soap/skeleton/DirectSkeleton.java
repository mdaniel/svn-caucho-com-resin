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

package com.caucho.soap.skeleton;

import java.io.*;
import java.util.*;

import javax.xml.stream.*;

import com.caucho.vfs.*;

/**
 * Invokes a SOAP request on a Java POJO
 */
public class DirectSkeleton extends Skeleton {
  private HashMap<String,PojoMethodSkeleton> _actionMap
    = new HashMap<String,PojoMethodSkeleton>();

  public void addAction(String name, PojoMethodSkeleton action)
  {
    _actionMap.put(name, action);
  }
  
  /**
   * Invokes the request.
   */
  public void invoke(Object service,
		     XMLStreamReader in,
		     WriteStream out)
    throws IOException, XMLStreamException
  {
    /*
    in.nextTag();

    if (! "Envelope".equals(in.getName().getLocalPart()))
      throw new IOException("expected Envelope at " + in.getName());

    in.nextTag();

    // XXX: Header

    if (! "Body".equals(in.getName().getLocalPart()))
      throw new IOException("expected Body");

    in.nextTag();

    String action = in.getName().getLocalPart();
    */
    String action = "hello";
    // XXX: parse arguments

    out.println("<?xml version=\"1.0\"?>");
    out.print("<env:Envelope xmlns:env=\"" + SOAP_ENVELOPE + "\"");
    out.print(" xmlns:xsi=\"" + XMLNS_XSI + "\"");
    out.print(" xmlns:xsd=\"" + XMLNS_XSD + "\">");
    out.println("<env:Body>");

    PojoMethodSkeleton method = _actionMap.get(action);

    // XXX: exceptions<->faults
    if (method != null)
      method.invoke(service, in, out);    
    else
      // XXX: fault
      out.println("no such action:" + action);
    /*
    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException("expected </" + action + ">");
    else if (! action.equals(in.getName().getLocalPart()))
      throw new IOException("expected </" + action + ">");

    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException("expected </Body>");
    else if (! "Body".equals(in.getName().getLocalPart()))
      throw new IOException("expected </Body>");

    if (in.nextTag() != in.END_ELEMENT)
      throw new IOException("expected </Envelope>");
    else if (! "Envelope".equals(in.getName().getLocalPart()))
      throw new IOException("expected </Envelope>");
    */
    out.print("\n</env:Body></env:Envelope>");
  }
}


