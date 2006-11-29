/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.simplexml;

import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PHP SimpleXML
 */
public class SimpleXMLModule
  extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(SimpleXMLModule.class.getName());
  private static final L10N L = new L10N(SimpleXMLModule.class);

  public SimpleXMLElement simplexml_load_string(Env env,
                                                InputStream data,
                                                @Optional String className,
                                                @Optional int options)
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();

      Document document = builder.parse(data);
      
      return new SimpleXMLElement(env, document, document.getDocumentElement());
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.toString()), e);
      return null;
    }
  }

  public SimpleXMLElement simplexml_load_file(Env env,
                                              @NotNull Path file,
                                              @Optional String className,
                                              @Optional int options)
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(file.openRead());
      
      return new SimpleXMLElement(env, document, document.getDocumentElement());
    } catch (Exception e) {
      log.log(Level.FINE, L.l(e.toString()), e);
      return null;
    }
  }

  //@todo simplexml_import_dom -- Skip until (XXX. DOM Functions implemented)
}
