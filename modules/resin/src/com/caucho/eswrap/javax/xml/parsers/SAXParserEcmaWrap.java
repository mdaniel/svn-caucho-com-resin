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

package com.caucho.eswrap.javax.xml.parsers;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;

import com.caucho.vfs.*;
import com.caucho.util.*;
import com.caucho.xml.*;
import com.caucho.es.*;

public class SAXParserEcmaWrap {
  public static void parse(SAXParser parser, Call call, int length)
    throws Throwable
  {
    Object obj = call.getArgObject(0, length);
    Object objBase = call.getArgObject(1, length);

    if (objBase instanceof HandlerBase) {
      HandlerBase base = (HandlerBase) objBase;
      if (obj instanceof InputStream)
        parser.parse((InputStream) obj, base);
      else if (obj instanceof Path) {
        Path path = (Path) obj;
        ReadStream is = path.openRead();
        try {
          parser.parse(is, base);
        } finally {
          is.close();
        }
      }
      else if (obj instanceof String) {
        parser.parse((String) obj, base);
      }
      else
        throw new IOException();
    }
    else {
      DefaultHandler base = (DefaultHandler) objBase;
      if (obj instanceof InputStream)
        parser.parse((InputStream) obj, base);
      else if (obj instanceof Path) {
        Path path = (Path) obj;
        ReadStream is = path.openRead();
        try {
          parser.parse(is, base);
        } finally {
          is.close();
        }
      }
      else if (obj instanceof String) {
        parser.parse((String) obj, base);
      }
      else if (obj instanceof InputSource) {
        parser.parse((InputSource) obj, base);
      }
      else
        throw new IOException();
    }
  }
}
