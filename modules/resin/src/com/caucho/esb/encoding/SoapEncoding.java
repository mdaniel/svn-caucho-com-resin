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
 * @author Emil Ong
 */

package com.caucho.esb.encoding;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.caucho.esb.WebService;

import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.soap.skeleton.DirectSkeleton;

/**
 * Invokes a service based on a Hessian-encoded request.
 */
public class SoapEncoding implements ServiceEncoding {
  private static final Logger log =
    Logger.getLogger(SoapEncoding.class.getName());

  private Object _object;
  private Class _class;
  private DirectSkeleton _skeleton;
  private WebService _webService;

  public void setService(Object service)
  {
    _object = service;

    if (_class == null)
      _class = service.getClass();
  }

  public void setWebService(WebService webService)
  {
    _webService = webService;
  }

  public void setInterface(Class cl)
  {
    _class = cl;
  }

  public void init()
    throws Throwable
  {
  }

  public void invoke(InputStream is, OutputStream os)
  {
    try {
      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      XMLStreamReader in = inputFactory.createXMLStreamReader(is);

      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLStreamWriter out = outputFactory.createXMLStreamWriter(os);

      getSkeleton().invoke(_object, in, out);

      out.flush();
    } catch (XMLStreamException e) {
      log.info(e.toString());
    } catch (IOException e) {
      log.info(e.toString());
    }
  }    

  private DirectSkeleton getSkeleton()
  {
    if (_skeleton == null)
      _skeleton = new WebServiceIntrospector().introspect(_class);

    return _skeleton;
  }
}
