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
 * @author Sam
 */

package com.caucho.quercus.lib.dom;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.TempBufferStringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.Construct;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QDocument;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlPrinter;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DOMDocument
  extends QDocument
{
  private final static Logger log = Logger.getLogger(DOMDocument.class.getName());

  private String _version;
  private String _encoding;

  @Construct
  public DOMDocument(@Optional("'1.0'") String version, @Optional String encoding)
  {
    _version = version;

    if (encoding != null && encoding.length() > 0)
      setEncoding(encoding);
  }

  DOMDocument(DOMImplementation implementation)
  {
    super(implementation.getQDOMImplementation());
  }

  public String getEncoding()
  {
    return _encoding;
  }

  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  public String getVersion()
  {
    return _version;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public DOMConfiguration getConfig()
  {
    throw new UnimplementedException();
  }

  public boolean getFormatOutput()
  {
    throw new UnimplementedException();
  }

  public void setFormatOutput(boolean formatOutput)
  {
    throw new UnimplementedException();
  }

  public boolean getPreserveWhiteSpace()
  {
    throw new UnimplementedException();
  }

  public void setPreserveWhiteSpace(boolean preserveWhiteSpace)
  {
    throw new UnimplementedException();
  }

  public boolean getRecover()
  {
    throw new UnimplementedException();
  }

  public void setRecover(boolean recover)
  {
    throw new UnimplementedException();
  }

  public boolean getResolveExternals()
  {
    throw new UnimplementedException();
  }

  public void setResolveExternals(boolean resolveExternals)
  {
    throw new UnimplementedException();
  }

  public boolean getSubstituteEntities()
  {
    throw new UnimplementedException();
  }

  public void setSubstituteEntities(boolean substituteEntities)
  {
    throw new UnimplementedException();
  }

  public boolean getValidateOnParse()
  {
    throw new UnimplementedException();
  }

  public void setValidateOnParse(boolean validateOnParse)
  {
    throw new UnimplementedException();
  }

  public Element createElement(String name, String textContent)
  {
    Element element = super.createElement(name);

    element.setTextContent(textContent);

    return element;
  }

  public Element createElementNS(String namespaceURI, String qualifiedName, String textContent)
  {
    Element element = super.createElementNS(namespaceURI, qualifiedName);

    element.setTextContent(textContent);

    return element;
  }

  public ProcessingInstruction createProcessingInstruction(String target)
  {
    throw new UnimplementedException();
  }

  public Node importNode(Node node)
  {
    throw new UnimplementedException();
  }

  // XXX: also can be called statically, returns a DOMDocument in that case
  public boolean load(Env env, Path path, @Optional Value options)
  {
    ReadStream is = null;

    try {
      is = path.openRead();

      Xml xml = new Xml();

      xml.parseDocument(this, is, path.getPath());
    }
    catch (SAXException ex) {
      env.warning(ex);
    }
    catch (IOException ex) {
      throw new QuercusModuleException(ex);
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (IOException ex) {
          log.log(Level.FINE, ex.toString(), ex);
        }
      }
    }

    return true;
  }

  /**
   * @param source A string containing the XML
   */
  // XXX: also can be called statically, returns a DOMDocument in that case
  public boolean loadHTML(String source)
  {
    throw new UnimplementedException();
  }

  // XXX: also can be called statically, returns a DOMDocument in that case
  public boolean loadHTMLFile(String filename)
  {
    throw new UnimplementedException();
  }

  // XXX: also can be called statically, returns a DOMDocument in that case
  public boolean loadXML(String source, @Optional Value options)
  {
    throw new UnimplementedException();
  }

  public boolean relaxNGValidate(String rngFilename)
  {
    throw new UnimplementedException();
  }

  public boolean relaxNGValidateSource(String rngSource)
  {
    throw new UnimplementedException();
  }

  /**
   * @return the number of bytes written, or FALSE for an error
   */
  public Value save(String filename, @Optional Value options)
  {
    throw new UnimplementedException();
  }

  @ReturnNullAsFalse
  public void saveHTML()
  {
    throw new UnimplementedException();
  }

  /**
   * @return the number of bytes written, or FALSE for an error
   */

  @ReturnNullAsFalse
  public String saveHTMLFile(String filename)
  {
    throw new UnimplementedException();
  }

  @ReturnNullAsFalse
  public StringValue saveXML()
  {
    TempStream tempStream = new TempStream();

    try {
      tempStream.openWrite();
      WriteStream os = new WriteStream(tempStream);

      XmlPrinter printer = new XmlPrinter(os);

      printer.setVersion(_version);
      printer.setEncoding(_encoding);

      printer.setPrintDeclaration(true);

      printer.printXml(this);

      os.println();
      
      os.close();
    }
    catch (IOException ex) {
      tempStream.discard();
      throw new QuercusModuleException(ex);
    }

    TempBufferStringValue result = new TempBufferStringValue(tempStream.getHead());

    tempStream.discard();

    return result;
  }

  public boolean schemaValidate(String schemaFilename)
  {
    throw new UnimplementedException();
  }

  public boolean schemaValidateSource(String schemaSource)
  {
    throw new UnimplementedException();
  }

  public boolean validate()
  {
    throw new UnimplementedException();
  }

  public int xinclude(@Optional int options)
  {
    throw new UnimplementedException();
  }
}
