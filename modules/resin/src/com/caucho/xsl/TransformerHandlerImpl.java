/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.xsl;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;
import com.caucho.java.*;
import com.caucho.xpath.*;
import com.caucho.log.Log;

public class TransformerHandlerImpl extends DOMBuilder
  implements TransformerHandler {
  protected static final Logger log = Log.open(TransformerHandlerImpl.class);
  protected static final L10N L = new L10N(TransformerHandlerImpl.class);
  
  private javax.xml.transform.Transformer _transformer;
  private Result _result;
  
  TransformerHandlerImpl(javax.xml.transform.Transformer transformer)
  {
    _transformer = transformer;

    init(new QDocument());
  }

  public String getSystemId()
  {
    return "asdf";
  }

  public javax.xml.transform.Transformer getTransformer()
  {
    return _transformer;
  }

  public void setResult(Result result)
  {
    _result = result;
  }

  public void endDocument()
    throws SAXException
  {
    super.endDocument();

    Node node = getNode();
    DOMSource source = new DOMSource(node);

    try {
      ((TransformerImpl) _transformer).transform(source, _result);
    } catch (TransformerException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public void notationDecl(String name, String publicId, String systemId)
  {
  }
  
  public void unparsedEntityDecl(String name, String publicId,
                                 String systemId, String notationName)
  {
  }
  
  public void startDTD(String name, String publicId, String systemId)
    throws SAXException
  {
  }
  
  public void endDTD()
    throws SAXException
  {
  }
  
  public void startEntity(String name)
    throws SAXException
  {
  }
  
  public void endEntity(String name)
    throws SAXException
  {
  }
  
  public void startCDATA()
    throws SAXException
  {
  }
  
  public void endCDATA()
    throws SAXException
  {
  }
}
