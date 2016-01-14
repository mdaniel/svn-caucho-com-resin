/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.relaxng;

import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;
import com.caucho.v5.xml.Xml;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;

/**
 * JARV Verifier factory.
 */
public class VerifierFactoryImpl implements VerifierFactory {
  /**
   * Compile a schema.
   */
  public Schema compileSchema(String url)
    throws SAXException, IOException
  {
    PathImpl path = VfsOld.lookup(url);

    ReadStream is = path.openRead();
    try {
      InputSource source = new InputSource(is);
      source.setSystemId(url);

      return compileSchema(source);
    } finally {
      is.close();
    }
  }

    
  /**
   * Compile a schema.
   */
  public Schema compileSchema(InputSource is)
    throws SAXException, IOException
  {
    try {
      RelaxBuilder builder = new RelaxBuilder();

      XMLReader reader = new Xml();

      reader.setContentHandler(builder);

      reader.parse(is);

      return new SchemaImpl(builder.getGrammar());
    } catch (RelaxException e) {
      throw new SAXException(e);
    }
  }
}
